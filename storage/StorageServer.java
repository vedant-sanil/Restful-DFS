import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.Executors;

import jsonhelper.*;
import com.google.gson.Gson;
import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpExchange;

/** Base class of storage servers.
 */
public class StorageServer
{

    /** Storage IP address. */
    public final static String      STORAGE_IP = "127.0.0.1";
    public final static String     CLIENT_PORT;
    public final static String    COMMAND_PORT;
    public final static String     NAMING_PORT;
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
    public String root_dir;

    public StorageServer(String CLIENT_PORT, String COMMAND_PORT, String NAMING_PORT, String root_dir) throws IOException
    {
        this.CLIENT_PORT = CLIENT_PORT;
        this.COMMAND_PORT = COMMAND_PORT;
        this.NAMING_PORT = NAMING_PORT;
        this.root_dir = root_dir;
        this.client_skeleton = HttpServer.create(new InetSocketAddress(0), 0);
        this.client_skeleton.setExecutor(Executors.newCachedThreadPool());

        this.command_skeleton = HttpServer.create(new InetSocketAddress(0), 0);
        this.command_skeleton.setExecutor(Executors.newCachedThreadPool());
        skeletons_started = false;
        gson = new Gson();
    }

    protected synchronized void startSkeletons()
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

    public HttpResponse<String> register(Gson gson, int naming_register_port, String[] files)
    {
        int storagePort = this.client_skeleton.getAddress().getPort();
        int commandPort = this.command_skeleton.getAddress().getPort();

        RegisterRequest registerRequest = new RegisterRequest(STORAGE_IP, storagePort, commandPort, files);
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
        catch (Exception e)
        {
            e.printStackTrace();
        }
        return response;
    }

    /** Stops the storage server. */
    public void stop()
    {
        command_skeleton.stop(0);
        client_skeleton.stop(0);
    }

    /** Add APIs supported by client skeleton. */
    private void add_client_api()
    {
//        this.size();
//        this.read();
//        this.write();
    }

    /** Add APIs supported by command skeleton. */
    private void add_command_api()
    {
//        this.create();
//        this.delete();
//        this.copy();
    }

    /** Throws <code>UnsupportedOperationException</code>. */
    public void size()
    {
        this.client_skeleton.createContext("/storage_size", (exchange ->
        {

        }));
    }

}