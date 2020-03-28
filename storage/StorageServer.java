package storage;
import java.io.IOException;
import java.util.*;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.Executors;
import java.io.*;
import java.io.File;
import java.util.Random;
import jsonhelper.*;
import com.google.gson.Gson;
import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpExchange;
import test.common.Path;
import test.util.Test;
import test.util.TestFailed;
import test.util.TestUtil;
import java.io.FileNotFoundException;

/** Base class of storage servers.
 */
public class StorageServer
{
    /** Storage IP address. */
    public final static String      STORAGE_IP = "127.0.0.1";
    public int     CLIENT_PORT;
    public int    COMMAND_PORT;
    public int     NAMING_PORT;
    /** Client interface skeleton. */
    protected HttpServer            client_skeleton;
    /** Command interface skeleton. */
    protected HttpServer            command_skeleton;
    /** Indicates that the skeletons have been started. This is used to prevent
     multiple starts of the skeletons by alternative implementations of the
     <code>start</code> method. */
    private boolean                 skeletons_started;
    /** Gson object which can parse json to an object. */
    protected Gson                  gson;
    /** Local Directory to locate files to serve */
    public static String root_dir;

    public StorageServer(int CLIENT_PORT, int COMMAND_PORT, int NAMING_PORT, String root_dir) throws IOException
    {
        this.CLIENT_PORT = CLIENT_PORT;
        this.COMMAND_PORT = COMMAND_PORT;
        this.NAMING_PORT = NAMING_PORT;
        this.root_dir = root_dir;
        this.client_skeleton = HttpServer.create(new InetSocketAddress(CLIENT_PORT), 0);
        this.client_skeleton.setExecutor(Executors.newCachedThreadPool());

        this.command_skeleton = HttpServer.create(new InetSocketAddress(COMMAND_PORT), 0);
        this.command_skeleton.setExecutor(Executors.newCachedThreadPool());
        skeletons_started = false;
        gson = new Gson();
        System.out.println(CLIENT_PORT);
        System.out.println(COMMAND_PORT);
        System.out.println(NAMING_PORT);
        System.out.println(root_dir);
        System.out.println("--------------------");
    }

    protected synchronized void startSkeletons() throws TestFailed
    {
        // Prevent repeated starting of the skeletons and re-creation of stubs.
        if(skeletons_started)
            return;

        // Start the client interface skeleton and create the stub.
        client_skeleton.start();

        // Start the registration skeleton and create the stub.
        command_skeleton.start();

        skeletons_started = true;

        // Register all API to two skeletons
        this.add_client_api();
        this.add_command_api();
    }


    /** File listing */
    public List<File> listFilesForFolder(final File folder, List<File> files) {
        for (final File fileEntry : folder.listFiles()) {
            if (fileEntry.isDirectory()) {
                files = listFilesForFolder(fileEntry, files);
            } else {
                files.add(fileEntry);
            }
        }
        return files;
    }

    /** Register to a naming server */
    public HttpResponse<String> register(Gson gson, int naming_register_port, String[] files) throws TestFailed
    {
//        int storagePort = this.client_skeleton.getAddress().getPort();
//        int commandPort = this.command_skeleton.getAddress().getPort();

        RegisterRequest registerRequest = new RegisterRequest(STORAGE_IP, CLIENT_PORT, COMMAND_PORT, files);
        HttpClient client = HttpClient.newHttpClient();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://127.0.0.1:" + naming_register_port + "/register"))
                .setHeader("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(gson.toJson(registerRequest)))
                .build();

        HttpResponse<String> response;

        try
        {
            response = client.send(request, HttpResponse.BodyHandlers.ofString());
        }
        catch (Throwable t)
        {
            throw new TestFailed("unable to send request to naming server", t);
        }
        return response;
    }

    /** Starts the test storage server. */
    public synchronized ServerInfo start(int naming_register_port) throws TestFailed
    {
        // Start storage server skeletons.
        startSkeletons();
        File f = new File(root_dir);
        File[] files;
        List<File> fi = new ArrayList<>();
        fi = listFilesForFolder(f, fi);
        files = fi.toArray(new File[fi.size()]);
        String[] files_string = new String[files.length];
        String[] delete_files_string = new String[files.length];


        for(int i = 0; i < files.length; i++)
        {
            files_string[i] = files[i].toString().replace(root_dir, "");
            System.out.println(files_string[i]);
        }

        // Register the storage server with the naming server.
        HttpResponse<String> response = register(gson, this.NAMING_PORT, files_string);
        delete_files_string = gson.fromJson(response.body(), FilesReturn.class).files;
        File[] delete_files = new File[delete_files_string.length];

//        System.out.println(root_dir);
        System.out.println("DELETED FILES");
        for(int i = 0; i < delete_files_string.length; i++)
        {
            System.out.println("-----");
            System.out.println(delete_files_string[i]);
            System.out.println("-----");
            delete_files[i] = new File(root_dir + delete_files_string[i]);
            System.out.println(delete_files[i]);
            boolean a = delete_files[i].delete();
            System.out.println(a);
            String dir = delete_files[i].getParent();
            File dirfile = new File(dir);
            while (dirfile.list().length == 0)
            {
                System.out.println(dirfile);
                a = dirfile.delete();
                System.out.println(a);
                String d = dirfile.getParent();
                dirfile = new File(d);
            }
        }
        return new ServerInfo(STORAGE_IP, client_skeleton.getAddress().getPort());
    }

    /** Stops the storage server. */
    public void stop() throws TestFailed
    {
        command_skeleton.stop(0);
        client_skeleton.stop(0);
    }

    /** Add APIs supported by client skeleton. */
    private void add_client_api() throws TestFailed
    {
        this.size();
//        this.read();
//        this.write();
    }

    /** Add APIs supported by command skeleton. */
    private void add_command_api() throws TestFailed
    {
//        this.create();
//        this.delete();
//        this.copy();
    }

    public static void main(String[] args) throws FileNotFoundException, IOException, TestFailed
    {
        Random rand = new Random();
        File file = new File("./StorageServer" + rand.nextInt() + ".output");
        PrintStream stream = new PrintStream(file);
        System.setOut(stream);
        System.setErr(stream);
        System.out.println("MAIN METHOD CALLED");
        StorageServer s = new StorageServer(Integer.parseInt(args[0]), Integer.parseInt(args[1]), Integer.parseInt(args[2]), args[3]);
        s.start(Integer.parseInt(args[2]));
    }

    /** Throws <code>UnsupportedOperationException</code>. */
    public void size() throws TestFailed
    {
        this.client_skeleton.createContext("/storage_size", (exchange ->
        {
            String respText = "";
            int returnCode = 200;
            if ("POST".equals(exchange.getRequestMethod())) {
                // parse request json
                RegisterRequest registerRequest = null;
                try {
                    InputStreamReader isr = new InputStreamReader(exchange.getRequestBody(), "utf-8");
                    registerRequest = gson.fromJson(isr, RegisterRequest.class);
                    System.out.println("3123132123123123123123123123123123123123123123123123123123213");
                    System.out.println(registerRequest.toString());
                } catch (Exception e) {
                    respText = "Error during parse JSON object!\n";
                    returnCode = 400;
                    this.generateResponseAndClose(exchange, respText, returnCode);
                    return;
                }
                respText = gson.toJson("");
                returnCode = 200;
            }
            else {
                respText = "The REST method should be POST for <register>!\n";
                returnCode = 400;
            }
            this.generateResponseAndClose(exchange, respText, returnCode);
        }));
    }

    /**
     * call this function when you want to write to response and close the connection.
     */
    private void generateResponseAndClose(HttpExchange exchange, String respText, int returnCode) throws IOException {
        exchange.sendResponseHeaders(returnCode, respText.getBytes().length);
        OutputStream output = exchange.getResponseBody();
        output.write(respText.getBytes());
        output.flush();
        exchange.close();
    }


    protected void sendBooleanReturn(HttpExchange exchange, boolean success, int returnCode) throws IOException
    {
        BooleanReturn booleanReturn = new BooleanReturn(success);
        String respText = gson.toJson(booleanReturn);
        exchange.sendResponseHeaders(returnCode, respText.getBytes().length);
        OutputStream output = exchange.getResponseBody();
        output.write(respText.getBytes());
        output.flush();
        exchange.close();
    }
}