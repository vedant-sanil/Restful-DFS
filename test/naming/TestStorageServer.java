package test.naming;

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
import test.common.Path;
import test.util.Test;
import test.util.TestFailed;
import test.util.TestUtil;

/** Base class of storage servers used to test the naming server.

    <p>
    Instances of this class and its subclasses are connected to the naming
    server under test. Methods of this class can be overridden to monitor the
    behavior of the naming server. The default implementations of the methods
    throw <code>UnsupportedOperationException</code>, indicating that a call to
    any of the methods in the client or command interfaces is not expected
    during a test.
 */
public class TestStorageServer
{
    /** Test which is using this storage server. Used to signal failures. */
    protected final Test            test;
    /** Storage IP address. */
    public final static String      STORAGE_IP = "127.0.0.1";
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

    /** Creates the test storage server.

        @param test The test which is using this storage server.
     */
    public TestStorageServer(Test test) throws IOException
    {
        this.test = test;
        this.client_skeleton = HttpServer.create(new InetSocketAddress(0), 0);
        this.client_skeleton.setExecutor(Executors.newCachedThreadPool());

        this.command_skeleton = HttpServer.create(new InetSocketAddress(0), 0);
        this.command_skeleton.setExecutor(Executors.newCachedThreadPool());
        skeletons_started = false;
        gson = new Gson();
    }

    /** Starts skeletons for the client and command interfaces.

        <p>
        The naming server registration test has alternative implementations of
        the <code>start</code> method. Each such implementation calls this
        method to prevent repeated calls to <code>start</code> in the skeletons.

     */
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

    /** Register to a naming server
         @param gson The Gson object to parse the json request.
         @param naming_register_port The port of Naming server for registration.
         @param files The list of files which is to be registered with the
                      naming server.
         @return A HTTP response returned by naming server.
         @throws TestFailed If registration request can't be sent to the naming server.
     */
    public HttpResponse<String> register(Gson gson, int naming_register_port, String[] files) throws TestFailed
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
        catch (Throwable t)
        {
            throw new TestFailed("unable to send request to naming server", t);
        }
        return response;
    }

    /** Starts the test storage server.

        <p>
        The storage server connects to the given naming server and attempts to
        register. It provides the naming server with the given list of files.
        If <code>expect_files</code> is not <code>null</code>, the storage
        server checks that the naming server then asks it to delete that set of
        files.

        @param naming_register_port The register port of naming server with
                                    which the storage server is to register.
        @param offer_files The list of files which is to be registered with the
                           naming server.
        @param expect_files An optional list of files which the storage server
                            expects the naming server to ask it to delete. This
                            parameter can be <code>null</code> if the storage
                            server should not check which files the naming
                            server replies with.
        @return The storage server client interface stub.
        @throws TestFailed If <code>expect_files</code> is not <code>null</code>
                           and the naming server does not reply with the
                           correct set of duplicate files.
     */
    public synchronized ServerInfo start(int naming_register_port, Path[] offer_files,
                                         Path[] expect_files) throws TestFailed
    {
        // Start storage server skeletons.
        startSkeletons();

        String[] files = new String[offer_files.length];

        for(int i = 0; i < files.length; i++)
        {
            files[i] = offer_files[i].toString();
        }

        // Register the storage server with the naming server.
        HttpResponse<String> response = register(gson, naming_register_port, files);
        files = gson.fromJson(response.body(), FilesReturn.class).files;

        Path[] delete_files = new Path[files.length];
        for(int i = 0; i < files.length; i++)
        {
            delete_files[i] = new Path(files[i]);
        }

        // Check that the naming server replied with the appropriate files.
        if(expect_files != null)
        {
            if(!TestUtil.sameElements(delete_files, expect_files))
            {
                throw new TestFailed("naming server did not command deletion " +
                                     "of the expected files");
            }
        }

        return new ServerInfo(STORAGE_IP, client_skeleton.getAddress().getPort());
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
        this.size();
        this.read();
        this.write();
    }

    /** Add APIs supported by command skeleton. */
    private void add_command_api()
    {
        this.create();
        this.delete();
        this.copy();
    }

    /** Throws <code>UnsupportedOperationException</code>. */
    public void size()
    {
        this.client_skeleton.createContext("/storage_size", (exchange ->
        {
            sendBooleanReturn(exchange, false, 400);
            test.failure(new UnsupportedOperationException("size method not implemented"));
        }));
    }

    /** Throws <code>UnsupportedOperationException</code>. */
    public void read()
    {
        this.client_skeleton.createContext("/storage_read", (exchange ->
        {
            sendBooleanReturn(exchange, false, 400);
            test.failure(new UnsupportedOperationException("read method not implemented"));
        }));
    }

    /** Throws <code>UnsupportedOperationException</code>. */
    public void write()
    {
        this.client_skeleton.createContext("/storage_write", (exchange ->
        {
            sendBooleanReturn(exchange, false, 400);
            test.failure(new UnsupportedOperationException("write method not implemented"));
        }));
    }

    /** Throws <code>UnsupportedOperationException</code>. */
    public void create()
    {
        this.command_skeleton.createContext("/storage_create", (exchange ->
        {
            sendBooleanReturn(exchange, false, 400);
            test.failure(new UnsupportedOperationException("create method not implemented"));
        }));
    }

    /** Throws <code>UnsupportedOperationException</code>. */
    public void delete()
    {
        this.command_skeleton.createContext("/storage_delete", (exchange ->
        {
            sendBooleanReturn(exchange, false, 400);
            test.failure(new UnsupportedOperationException("delete method not implemented"));
        }));
    }

    /** Throws <code>UnsupportedOperationException</code>. */
    public void copy()
    {
        this.command_skeleton.createContext("/storage_copy", (exchange ->
        {
            sendBooleanReturn(exchange, false, 400);
            test.failure(new UnsupportedOperationException("copy method not implemented"));
        }));
    }

    /** Send a BooleanReturn response to naming server.

         @param exchange The exchange object to send the HTTP response.
         @param success The value of the success field in BooleanReturn.
         @param returnCode The return code for the HTTP response.
     */
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
