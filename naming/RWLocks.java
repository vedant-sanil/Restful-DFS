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

public class RWLocks {
    public int readLocks = 0;
    public int writeLocks = 0;
    public Queue<Integer> queue;
    public int readNumber = 0;
    Random rand = new Random();
    protected Gson gson;

    public RWLocks()
    {
        this.readLocks = 0;
        this.writeLocks = 0;
        this.queue = new LinkedList<>();
        this.gson = new Gson();
    }

    public synchronized void getReadLock(int uniqueID, ArrayList<StorageServerInfo> regServers, String filepath) throws InterruptedException,IOException {
        System.out.println("BEFORE ADDING - " +queue);
        this.queue.add(uniqueID);
        System.out.println(uniqueID + " - Added to the queue (Reading)");
        System.out.println("AFTER ADDING - "+queue);
        while (writeLocks > 0 || !(this.queue.peek() == uniqueID)) {
            wait();
        }
        readLocks += 1;
        readNumber += 1;
        if (readNumber > 20)
        {
            System.out.println(filepath + " - Time to Replicate");
            int n = rand.nextInt(regServers.size());

            // Loop through servers to check which server file exists in
            String server_ip = "";
            int server_port = 0;
            serverloop:
            for (StorageServerInfo s : regServers) {
                for (String filename : s.getFiles()) {
                    if (filename.equals(filepath)) {
                        System.out.println(s.getCommand_port() + " : filename is : " + filename);
                        server_ip = s.getStorage_ip();
                        server_port = s.getCommand_port();
                        System.out.println("Server IP: "+server_ip+" Server Port: "+server_port);
                        break serverloop;
                    }
                }
            }

            System.out.println("INITIALLY = getCommand_port - "+regServers.get(n).getCommand_port()+ "getClient_port - "+regServers.get(n).getClient_port()+" Server Command Port - "+server_port);
            while(regServers.get(n).getCommand_port() == server_port)
            {
                n = rand.nextInt(regServers.size());
                System.out.println("getCommand_port - "+regServers.get(n).getCommand_port()+ "getClient_port - "+regServers.get(n).getClient_port()+" Server Command Port - "+server_port);
            }
            System.out.println("FINALLY = getCommand_port - "+regServers.get(n).getCommand_port()+ "getClient_port - "+regServers.get(n).getClient_port()+" Server Command Port - "+server_port);

            System.out.println("Random IP: "+regServers.get(n).getStorage_ip()+" Random Port - "+regServers.get(n).getClient_port());
            Map<String, Object> req = new HashMap<String, Object>();
            req.put("path", filepath);
            req.put("server_ip",regServers.get(n).getStorage_ip());
            req.put("server_port",regServers.get(n).getClient_port());
            System.out.println("REQUEST AS FOLLOWS - "+req);

            HttpResponse<String> response = this.getResponse("/storage_copy", server_port, gson.toJson(req));

            System.out.println("RESPONSE AS FOLLOWS - "+response);
            this.readNumber = 0;
        }
        System.out.println("ReadLocks: "+readLocks+" WriteLocks: "+writeLocks+" Queue Peek: "+this.queue.peek()+" UniqueId: "+uniqueID);
        this.queue.remove();
        System.out.println(uniqueID + " - Removed from the queue (Reading)");
    }

    public synchronized void releaseReadLock() throws InterruptedException {
        readLocks -= 1;
        notifyAll();
    }

    public synchronized void getWriteLock(int uniqueID) throws InterruptedException {
        System.out.println("BEFORE ADDING " +queue);
        this.queue.add(uniqueID);
        System.out.println(uniqueID + " - Added to the queue (Writing)");
        System.out.println("AFTER ADDING "+queue);
        while (readLocks > 0 || writeLocks > 0 || !(this.queue.peek() == uniqueID)) {
            wait();
        }
        writeLocks += 1;
        System.out.println("ReadLocks: "+readLocks+" WriteLocks: "+writeLocks+" Queue Peek: "+this.queue.peek()+" UniqueId: "+uniqueID);
        this.queue.remove();
        System.out.println(uniqueID + " - Removed from the queue (Writing)");
        System.out.println(queue);
    }

    public synchronized void releaseWriteLock() throws InterruptedException {
        writeLocks -= 1;
        notifyAll();
    }

    private HttpResponse<String> getResponse(String method,
                                             int port,
                                             Object requestObj) throws IOException, InterruptedException {
        HttpResponse<String> response;
        HttpRequest request = HttpRequest.newBuilder().uri(URI.create("http://localhost:" + port + method))
                .setHeader("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(gson.toJson(requestObj)))
                .build();

        response = HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
        return response;
    }
}