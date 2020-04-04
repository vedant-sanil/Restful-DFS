package naming;
import java.util.*;
import java.util.Random;
import storage.StorageServerInfo;
import java.util.Random;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import com.google.gson.Gson;
import java.net.URI;
import java.net.http.HttpClient;
import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpExchange;
import java.io.IOException;
import java.util.HashMap;
import java.util.Set;

public class RWLocks {
    public int readLocks = 0;
    public int writeLocks = 0;

    // We need to ensure fairness amongst the requests which is why every request which comes in is queued for
    // getting access to the lock. The input to the queue is the random number generated for each request.
    public Queue<Integer> queue;
    public int readNumber = 0;

    // Random number generator to generate a random number used as an ID
    Random rand = new Random();
    protected Gson gson;

    // Hashmap of the path along with the replicated server's command port.
    // This is especially helpful in replication (invalidation) and deletion as it helps us invalidate and
    // delete data from all the replicated servers alone while keeping the real copy as it is in the Storage Server.
    Map<String, Set<Integer>> portmap = new HashMap<String, Set<Integer>>();

    public RWLocks()
    {
        this.readLocks = 0;
        this.writeLocks = 0;
        this.queue = new LinkedList<>();
        this.gson = new Gson();
    }

    /** Function to add to the portmap */
    public void add(String x,int y) {
        Set<Integer> set = portmap.get(x);
        if (set == null) {
            portmap.put(x, set = new HashSet<Integer>());
        }
        set.add(y);
    }

    /** Function gets a readLock(Shared Lock) for the resource */
    public synchronized void getReadLock(int uniqueID, ArrayList<StorageServerInfo> regServers, String filepath, boolean file) throws InterruptedException,IOException {

        // This variable helps track the number of times the request has come for this file node.
        this.readNumber += 1;

        // This portion of code is responsible for the replication of file when the number of read requests for that
        // file increases to more than 20. All of this happens before the file is locked for shared access.
        if (this.readNumber > 20 && file == true)
        {
            int n = rand.nextInt(regServers.size());

            // We need to find the actual storage server which the file is present in. For this,
            // I iterate through all the registered servers to find the first server with the occurrence of the file.
            String server_ip = "";
            int server_port = 0;
            serverloop:
            for (StorageServerInfo s : regServers) {
                for (String filename : s.getFiles()) {
                    if (filename.equals(filepath)) {
                        server_ip = s.getStorage_ip();
                        server_port = s.getClient_port();
                        break serverloop;
                    }
                }
            }

            // Now apart from this server, we need to choose at random, a server whose client port does not equal
            // the file server's client port. This is the Server to replicate the file to.
            while(regServers.get(n).getClient_port() == server_port)
            {
                n = rand.nextInt(regServers.size());
            }

            // Create my object for calling Storage Copy and sending the request to the randomly selected file server
            // to go ahead and replicate the file onto itself from the server who's details are given in the
            // body of the HTTP Request.
            Map<String, Object> req = new HashMap<String, Object>();
            req.put("path", filepath);
            req.put("server_ip",server_ip);
            req.put("server_port",server_port);
            HttpResponse<String> response = this.getResponse("/storage_copy", "localhost", regServers.get(n).getCommand_port(), req);

            // Adding the random servers port to the Set corresponding to the filepath. This keeps track of where all
            // the files have been replicated. Resetting the readnumber to 0 so that duplicate replications don't keep
            // happening.
            this.add(filepath, regServers.get(n).getCommand_port());
            this.readNumber = 0;
        }

        // Here, the request is added into the queue, in which it will remain until there are no more writeLocks and
        // the head of the queue is the request ID of itself.
        this.queue.add(uniqueID);
        while (writeLocks > 0 || !(this.queue.peek() == uniqueID)) {
            wait();
        }

        // Locking for shared access and removing the request ID from the queue to signify that the lock for the
        // file node has been given to that particular request from the client.
        readLocks += 1;
        this.queue.remove();
    }

    /** Releasing the ReadLock (Shared Access Lock) */
    public synchronized void releaseReadLock() throws InterruptedException {
        readLocks -= 1;

        // Once the lock is released, we must notify the thread which has been blocked due to the lock been present.
        notifyAll();
    }

    public synchronized void getWriteLock(int uniqueID, ArrayList<StorageServerInfo> regServers, String filepath) throws InterruptedException, IOException {
        // We need to first invalidate all the replicas. The portmap hashmap comes into play here. Since through this,
        // we know all the replicated Servers, we simply call storage_delete on those ports. The presence of the actual
        // file server not being included in this portmap ensures that there will always be one copy of the file on
        // the distributed system.
        String server_ip = "";
        int server_port = 0;
        StorageServerInfo s;
        try {
            Set<Integer> deletion_set = portmap.get(filepath);
            Map<String, Object> req = new HashMap<String, Object>();
            req.put("path", filepath);
            while (!deletion_set.isEmpty()) {
                HttpResponse<String> response = this.getResponse("/storage_delete", "localhost", deletion_set.stream().findFirst().get(), req);
                Iterator<Integer> it = deletion_set.iterator();
                it.remove();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Once all the invalidation is taken care of, we need to put in the request in the queue, in which it will
        // remain until there are no more writeLocks and the head of the queue is the request ID of itself.
        this.queue.add(uniqueID);
        while (readLocks > 0 || writeLocks > 0 || !(this.queue.peek() == uniqueID)) {
            wait();
        }

        // Locking for Exclusive access and removing from the queue.
        writeLocks += 1;
        this.queue.remove();
    }

    public synchronized void releaseWriteLock() throws InterruptedException {
        writeLocks -= 1;

        // Once the lock is released, we must notify the thread which has been blocked due to the lock been present.
        notifyAll();
    }

    private HttpResponse<String> getResponse(String method, String ip,
                                             int port,
                                             Object requestObj) throws IOException, InterruptedException {
        HttpResponse<String> response = null;
        HttpRequest request = HttpRequest.newBuilder().uri(URI.create("http://"+ip+":" + port + method))
                .setHeader("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(gson.toJson(requestObj)))
                .build();
        try {
            response = HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
            return response;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return response;
    }
}