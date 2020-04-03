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
        System.out.println("BEFORE ADDING - " +queue);
        this.readNumber += 1;
        if (this.readNumber > 20 && file == true)
        {
            System.out.println("__________Value of ReadNumber is : "+this.readNumber);
            System.out.println(filepath + " - Time to Replicate");
            int n = rand.nextInt(regServers.size());

            // Loop through servers to check which server file exists in
            String server_ip = "";
            int server_port = 0;
            serverloop:
            for (StorageServerInfo s : regServers) {
                for (String filename : s.getFiles()) {
                    if (filename.equals(filepath)) {
                        System.out.println(s.getClient_port() + " : filename is : " + filename);
                        server_ip = s.getStorage_ip();
                        server_port = s.getClient_port();
                        System.out.println("File Server IP: "+server_ip+" File Server Client Port: "+s.getClient_port());
                        break serverloop;
                    }
                }
            }


            System.out.println("INITIALLY = Random Server getCommand_port - "+regServers.get(n).getCommand_port()+ " Random Server getClient_port - "+regServers.get(n).getClient_port()+" File Server Client Port - "+server_port);
            while(regServers.get(n).getClient_port() == server_port)
            {
                n = rand.nextInt(regServers.size());
                System.out.println(" Random Server getCommand_port - "+regServers.get(n).getCommand_port()+ " Random Server getClient_port - "+regServers.get(n).getClient_port()+" File Server Client Port - "+server_port);
            }
            System.out.println("FINALLY = Random Server getCommand_port - "+regServers.get(n).getCommand_port()+ " Random Server getClient_port - "+regServers.get(n).getClient_port()+" File Server Client Port - "+server_port);

            System.out.println("Random Server IP: "+regServers.get(n).getStorage_ip()+" Random Server Command Port - "+regServers.get(n).getCommand_port());
            Map<String, Object> req = new HashMap<String, Object>();
            req.put("path", filepath);
            req.put("server_ip",server_ip);
            req.put("server_port",server_port);
            System.out.println("REQUEST AS FOLLOWS - "+req);
            HttpResponse<String> response = this.getResponse("/storage_copy", "localhost", regServers.get(n).getCommand_port(), req);
            this.add(filepath, regServers.get(n).getCommand_port());
            System.out.println("RESPONSE AS FOLLOWS - "+response);
            System.out.println(portmap);
            this.readNumber = 0;
            System.out.println("__________Value of ReadNumber is : "+this.readNumber);
        }

        this.queue.add(uniqueID);
        System.out.println(uniqueID + " - Added to the queue (Reading)");
        System.out.println("AFTER ADDING - "+queue);
        while (writeLocks > 0 || !(this.queue.peek() == uniqueID)) {
            wait();
        }

        System.out.println("ReadLocks: "+readLocks+" WriteLocks: "+writeLocks+" Queue Peek: "+this.queue.peek()+" UniqueId: "+uniqueID);
        readLocks += 1;
        this.queue.remove();
        System.out.println(uniqueID + " - Removed from the queue (Reading)");
    }

    public synchronized void releaseReadLock() throws InterruptedException {
        readLocks -= 1;
        notifyAll();
    }

    public synchronized void getWriteLock(int uniqueID, ArrayList<StorageServerInfo> regServers, String filepath) throws InterruptedException, IOException {

        System.out.println("FILE TO BE INVALIDATED");
        // Loop through servers to check which server file exists in.
        String server_ip = "";
        int server_port = 0;
        StorageServerInfo s;
//        serverloop:
//        try {
//        for (int i = 0; i < regServers.size(); i++) {
//            System.out.println("Between the 2 loops");
//            System.out.flush();
//            s = regServers.get(i);
//            System.out.println("Files are - " + s.getFiles().length);
//            System.out.println("s.getCommand_port() = " + s.getCommand_port() + " server_port = " + server_port);
//            for (String filename : s.getFiles()) {
//                System.out.println("s.getCommand_port() = " + s.getCommand_port() + " server_port = " + server_port + " || filename: " + filename + " filepath: " + filepath);
//                System.out.flush();
//                if (filename.equals(filepath)) {
//                    System.out.println(s.getCommand_port() + " : filename is : " + filename);
//                    System.out.flush();
//                    server_ip = s.getStorage_ip();
//                    server_port = s.getCommand_port();
//                    System.out.println("File Server IP: "+server_ip+" File Server Command Port: "+s.getCommand_port());
//                    System.out.flush();
//                }
//            }
//        }
//
//        // Loop through file servers and delete the file in everything except the main server
//        System.out.println("Size of regServers : " +regServers.size());
//        System.out.flush();
//            for (int i = 0; i < regServers.size(); i++) {
//                s = regServers.get(i);
//                System.out.println("Files are - " + s.getFiles().length);
//                System.out.println("s.getCommand_port() = " + s.getCommand_port() + " server_port = " + server_port);
//                for (String filename : s.getFiles()) {
//                    System.out.println("s.getCommand_port() = " + s.getCommand_port() + " server_port = " + server_port + " || filename: " + filename + " filepath: " + filepath);
//                    System.out.flush();
//                    if (filename.equals(filepath) && s.getCommand_port() != server_port) {
//                        System.out.println(s.getCommand_port() + " : filename is : " + filename + " Actual Server Port : " + server_port);
//                        System.out.flush();
//                        Map<String, Object> req = new HashMap<String, Object>();
//                        req.put("path", filepath);
//                        HttpResponse<String> response = this.getResponse("/storage_delete", s.getStorage_ip(), s.getCommand_port(), req);
//                        System.out.println("INVALIDATION - Response is " + response);
//                        System.out.flush();
//                    }
//                }
//            }
//        } catch(Exception e) {
//            e.printStackTrace();
//        }

        try {
            System.out.println("Check for Invalidation");
            Set<Integer> deletion_set = portmap.get(filepath);
            Map<String, Object> req = new HashMap<String, Object>();
            req.put("path", filepath);
            while (!deletion_set.isEmpty()) {
                HttpResponse<String> response = this.getResponse("/storage_delete", "localhost", deletion_set.stream().findFirst().get(), req);
                System.out.println("INVALIDATION - Response is " + response);
                Iterator<Integer> it = deletion_set.iterator();
                it.remove();
                System.out.println(deletion_set);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        System.out.println("BEFORE ADDING " + queue);
        this.queue.add(uniqueID);
        System.out.println(uniqueID + " - Added to the queue (Writing)");
        System.out.println("AFTER ADDING " + queue);
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