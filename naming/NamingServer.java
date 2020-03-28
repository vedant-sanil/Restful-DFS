package test.storage;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.concurrent.Executors;

import jsonhelper.*;
import com.google.gson.Gson;
import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpExchange;
import test.common.Path;
import test.util.Test;
import test.util.TestFailed;
import test.util.TestUtil;

/** Test naming server.

 <p>
 This naming server performs the following checks each time a storage server
 registers:
 <ul>
 <li>The correct file list has been sent.</li>
 </ul>
 */
public class NamingServer
{
    /** List of paths to expect from the next storage server to register. */
    private Path[] expect_files = null;
    /** List of paths to command the next storage server to register to
     delete. */
    private Path[] delete_files = null;
    /** Naming server IP address. */
    public final static String NAMING_IP = "127.0.0.1";
    public int REGISTRATION_PORT;
    public int SERVICE_PORT ;
    /** Naming server registration interface skeleton. */
    private HttpServer registration_skeleton;
    /** Naming server service interface skeleton. */
    private HttpServer service_skeleton;
    /** Last registered storage server client interface. */
    private ServerInfo client_stub = null;
    /** Last registered storage server command interface. */
    private ServerInfo command_stub = null;
    /** Indicates that the skeleton has started. */
    private boolean skeletons_started = false;
    /** Gson object which can parse json to an object. */
    protected Gson gson;

    /**
    Create the naming server
     */
    NamingServer(int REGISTRATION_PORT, int SERVICE_PORT) throws IOException{
        this.REGISTRATION_PORT = REGISTRATION_PORT;
        this.SERVICE_PORT = SERVICE_PORT;

        this.registration_skeleton = HttpServer.create(new java.net.InetSocketAddress(REGISTRATION_PORT), 0);
        this.registration_skeleton.setExecutor(Executors.newCachedThreadPool());

        this.service_skeleton = HttpServer.create(new java.net.InetSocketAddress(SERVICE_PORT), 0);
        service_skeleton.setExecutor(Executors.newCachedThreadPool());

        this.gson = new Gson();

        System.out.println(REGISTRATION_PORT);
        System.out.println(SERVICE_PORT);
    }

    /** Sets the files the next storage server to connect is expected to
     register.

     @param files The files to expect. The naming server will check that
     these are indeed the files that are received. If this
     argument is <code>null</code>, the naming server will not
     perform the check.
     */
    public void expectFiles(Path[] files)
    {
        expect_files = files;
    }

    /** Sets the files the next storage server to connect will be commanded to
     delete.

     @param files The files to be deleted. If this argument is
     <code>null</code>, the naming server will not command the storage server
     to delete any files.
     */
    public void deleteFiles(Path[] files)
    {
        delete_files = files;
    }

    /** Returns the client interface for the last storage server to register. */
    public ServerInfo clientInterface()
    {
        return client_stub;
    }

    /** Returns the command interface for the last storage server to
     register. */
    public ServerInfo commandInterface()
    {
        return command_stub;
    }

    /** Retrieves a registration stub for the test server.

     @return The stub.
     @throws TestFailed If a stub cannot be obtained.
     */
    ServerInfo stub()
    {
        return new ServerInfo(NAMING_IP, REGISTRATION_PORT);
    }

    /** Starts the test naming server.

     @throws TestFailed If the test cannot be started.
     */
    void start() throws TestFailed
    {
        this.startSkeletons();
    }

    /** Stops the test naming server. */
    void stop()
    {
        this.service_skeleton.stop(0);
        this.registration_skeleton.stop(0);
    }

    private void startSkeletons() {
        // Prevent repeated starting of the skeletons and re-creation of stubs.
        if (this.skeletons_started) return;

        this.add_registration_api();
        this.registration_skeleton.start();
        this.add_service_api();
        this.service_skeleton.start();

        this.skeletons_started = true;
    }

    private void add_registration_api() {
        this.register();
    }

    private void add_service_api() {

    }

    private void register() {

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
}
