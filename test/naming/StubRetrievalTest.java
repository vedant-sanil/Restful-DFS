package test.naming;

import java.io.*;
import java.net.http.HttpResponse;

import jsonhelper.ExceptionReturn;
import jsonhelper.PathRequest;
import jsonhelper.ServerInfo;
import test.DFSException;
import test.common.Path;
import test.util.TestFailed;

/** Tests that the naming server <code>getStorage</code> method returns the
    correct storage server stubs.

    <p>
    Items checked are:
    <ul>
    <li><code>getStorage</code> rejects bad arguments such as <code>null</code>,
        non-existent files, and paths to directories.</li>
    <li><code>getStorage</code> returns stubs for storage servers that are
        indeed hosting the file being requested.</li>
    </ul>
 */
public class StubRetrievalTest extends NamingTest
{
    /** Test notice. */
    public static final String  notice =
        "checking naming server getStorage method";
    /** Prerequisites. */
    public static final Class[] prerequisites =
        new Class[] {RegistrationTest.class};

    /** First registering storage server. */
    private TestStorageServer   server1;
    /** Second registering storage server. */
    private TestStorageServer   server2;

    /** First storage server stub. */
    private ServerInfo          server1_stub;
    /** Second storage server stub. */
    private ServerInfo          server2_stub;

    private final Path          file1 = new Path("/file1");
    private final Path          file2 = new Path("/directory/file2");

    private final Path          file3 = new Path("/directory/file3");
    private final Path          file4 = new Path("/another_directory/file4");

    /** Creates the <code>StubRetrievalTest</code> and sets the notice. */
    public StubRetrievalTest() throws IOException
    {
        server1 = new TestStorageServer(this);
        server2 = new TestStorageServer(this);
    }

    /** Performs the tests.

        @throws TestFailed If any of the tests fail.
     */
    @Override
    protected void perform() throws TestFailed
    {
        checkArguments();

        checkStub(file1, server1_stub);
        checkStub(file2, server1_stub);
        checkStub(file3, server2_stub);
        checkStub(file4, server2_stub);
    }

    /** Checks that the naming server returns the correct storage server stub
        for the given file.

        @param path The file for which the stub is to be requested.
        @param expected_stub The stub expected to be received.
        @throws TestFailed If the stub cannot be retrieved, or if the stub
                           retrieved is not the stub expected.
     */
    private void checkStub(Path path, ServerInfo expected_stub) throws TestFailed
    {
        ServerInfo     stub;
        String         exception_type;

        // Try to retrieve the stub from the naming server.
        try
        {
            PathRequest request = new PathRequest(path.toString());
            HttpResponse<String> response = getResponse("/getstorage", SERVICE_PORT, request);
            exception_type = gson.fromJson(response.body(), ExceptionReturn.class).exception_type;
            if(exception_type != null)
            {
                throw new Throwable(response.body());
            }
            stub = gson.fromJson(response.body(), ServerInfo.class);
        }
        catch(Throwable t)
        {
            throw new TestFailed("unable to retrieve storage server stub for " +
                                 path, t);
        }

        if(stub == null)
            throw new TestFailed("received null instead of stub for " + path);

        // Check that the stub received is equal to the stub expected.
        if(!stub.equals(expected_stub))
            throw new TestFailed("received wrong stub for " + path);
    }

    /** Checks that the <code>getStorage</code> method rejects bad arguments.

        @throws TestFailed If the test fails.
     */
    private void checkArguments() throws TestFailed
    {
        // Check that getStorage rejects null.
        try
        {
            HttpResponse<String> response = getResponse("/getstorage", SERVICE_PORT, new PathRequest(""));
            String exception_type = gson.fromJson(response.body(), ExceptionReturn.class).exception_type;

            if(exception_type == null)
            {
                throw new TestFailed("getStorage accepted empty string as argument");

            } else if (DFSException.valueOf(exception_type) != DFSException.IllegalArgumentException) {
                throw new Throwable(response.body());
            }
        }
        catch(TestFailed e) { throw e; }
        catch(Throwable t)
        {
            throw new TestFailed("getStorage threw unexpected exception when " +
                                 "given null as argument", t);
        }

        // Check that getStorage rejects non-existent files.
        try
        {
            PathRequest request = new PathRequest("/another_file");
            HttpResponse<String> response = getResponse("/getstorage", SERVICE_PORT, request);
            String exception_type = gson.fromJson(response.body(), ExceptionReturn.class).exception_type;

            if(exception_type == null)
            {
                throw new TestFailed("getStorage accepted path to non-existent " +
                        "file as argument");
            } else if (DFSException.valueOf(exception_type) != DFSException.FileNotFoundException) {
                throw new Throwable(response.body());
            }
        }
        catch(TestFailed e) { throw e; }
        catch(Throwable t)
        {
            throw new TestFailed("getStorage threw unexpected exception when " +
                                 "given a non-existent file as argument", t);
        }

        // Check that getStorage rejects directories.
        try
        {
            PathRequest request = new PathRequest("/directory");
            HttpResponse<String> response = getResponse("/getstorage", SERVICE_PORT, request);

            String exception_type = gson.fromJson(response.body(), ExceptionReturn.class).exception_type;

            if(exception_type == null)
            {
                throw new TestFailed("getStorage accepted directory as argument");
            } else if (DFSException.valueOf(exception_type) != DFSException.FileNotFoundException) {
                throw new Throwable(response.body());
            }
        }
        catch(TestFailed e) { throw e; }
        catch(Throwable t)
        {
            throw new TestFailed("getStorage threw unexpected exception when " +
                                 "given directory as argument", t);
        }
    }

    /** Starts servers used in the test.

        @throws TestFailed If any of the servers cannot be started.
     */
    @Override
    protected void initialize() throws TestFailed
    {
        super.initialize();

        try
        {
            server1_stub = server1.start(REGISTRATION_PORT,
                                         new Path[] {file1, file2}, null);
            server2_stub = server2.start(REGISTRATION_PORT,
                                         new Path[] {file3, file4, file1},
                                         null);
        }
        catch(Throwable t)
        {
            throw new TestFailed("unable to start storage servers", t);
        }
    }

    /** Stops all servers used in the test. */
    @Override
    protected void clean()
    {
        super.clean();

        if(server1 != null)
        {
            server1.stop();
            server1 = null;
        }

        if(server2 != null)
        {
            server2.stop();
            server2 = null;
        }
    }
}
