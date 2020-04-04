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
    public Queue<Integer> queue;
    public int readNumber = 0;
    Random rand = new Random();
    protected Gson gson;
    Map<String, Set<Integer>> portmap = new HashMap<String, Set<Integer>>();

    public RWLocks()
    {
        this.readLocks = 0;
        this.writeLocks = 0;
        this.queue = new LinkedList<>();
        this.gson = new Gson();
    }

    public void add(String x,int y) {
        Set<Integer> set = portmap.get(x);
        if (set == null) {
            portmap.put(x, set = new HashSet<Integer>());
        }
        set.add(y);
    }

    public synchronized void getReadLock(int uniqueID, ArrayList<StorageServerInfo> regServers, String filepath, boolean file) throws InterruptedException,IOException {
        this.readNumber += 1;
        if (this.readNumber > 20 && file == true)
        {
            int n = rand.nextInt(regServers.size());

            // Loop through servers to check which server file exists in
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


            while(regServers.get(n).getClient_port() == server_port)
            {
                n = rand.nextInt(regServers.size());
            }

            Map<String, Object> req = new HashMap<String, Object>();
            req.put("path", filepath);
            req.put("server_ip",server_ip);
            req.put("server_port",server_port);
            HttpResponse<String> response = this.getResponse("/storage_copy", "localhost", regServers.get(n).getCommand_port(), req);
            this.add(filepath, regServers.get(n).getCommand_port());
            this.readNumber = 0;
        }

        this.queue.add(uniqueID);
        while (writeLocks > 0 || !(this.queue.peek() == uniqueID)) {
            wait();
        }

        readLocks += 1;
        this.queue.remove();
    }

    public synchronized void releaseReadLock() throws InterruptedException {
        readLocks -= 1;
        notifyAll();
    }

    public synchronized void getWriteLock(int uniqueID, ArrayList<StorageServerInfo> regServers, String filepath) throws InterruptedException, IOException {

        // Loop through servers to check which server file exists in.
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
        this.queue.add(uniqueID);
        while (readLocks > 0 || writeLocks > 0 || !(this.queue.peek() == uniqueID)) {
            wait();
        }
        writeLocks += 1;
        this.queue.remove();
    }

    public synchronized void releaseWriteLock() throws InterruptedException {
        writeLocks -= 1;
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