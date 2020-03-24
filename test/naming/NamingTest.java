package test.naming;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import com.google.gson.Gson;
import test.Config;
import test.util.Test;
import test.util.TestFailed;

/** Base class of naming server tests.

    <p>
    On initialization, this class starts a naming server and creates stubs for
    its service and registration interfaces. These objects are then accessible
    to subclasses of <code>NamingTest</code>.

    <p>
    Derived classes should override <code>testServer</code> to include the main
    test code. Derived classes should override <code>initialize</code> and
    <code>clean</code> if they have additional servers or system objects to
    create, start, stop, and clean up.
 */
abstract class NamingTest extends Test
{
    /** Naming server under test. */
    private Process                       server = null;

    protected Gson                        gson;

    public static final int               SERVICE_PORT = 8080;
    public static final int               REGISTRATION_PORT = 8090;

    protected HttpResponse<String> getResponse(String method,
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

    abstract protected void perform() throws TestFailed;

    /** Initializes the test.

        <p>
        This method starts the naming server and creates the stubs through which
        its interfaces can be accessed.

        @throws TestFailed If the naming server cannot be started, or if the
                           stubs cannot be created.
     */
    protected void initialize() throws TestFailed
    {
        gson = new Gson();
        Socket service_socket;
        Socket registration_socket;
        try
        {
            Runtime runtime = Runtime.getRuntime();
            server = runtime.exec(Config.startNaming);
        }
        catch (Throwable t)
        {
            throw new TestFailed("unable to start naming server");
        }

        // Attempt to make the connection.
        while (true)
        {
            try
            {
                service_socket = new Socket();
                service_socket.connect(new InetSocketAddress("127.0.0.1", SERVICE_PORT));
                registration_socket = new Socket();
                registration_socket.connect(new InetSocketAddress("127.0.0.1", REGISTRATION_PORT));
                break;
            }
            catch (IOException e)
            {
                // Ignore the exception to give server some time to start up
            }
        }

        // Make a best effort to close the socket if the connection is
        // successful.
        try
        {
            service_socket.close();
            registration_socket.close();
        }
        catch(IOException e) { }
    }

    /** Stops the naming server when the test completes.

        <p>
        If a subclass overrides this method, the new implementation should call
        this method before proceeding to do anything else.
     */
    @Override
    protected void clean()
    {
        if(server != null)
        {
            kill(server.toHandle());

            // Wait for the naming server to stop.
            try
            {
                server.waitFor();
            }
            catch(InterruptedException e) { }

            server = null;
        }
    }

    protected void kill(ProcessHandle handle)
    {
        handle.descendants().forEach(this::kill);
        handle.destroy();
    }
}
