package test.storage;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.http.HttpResponse;
import java.util.*;

import jsonhelper.BooleanReturn;
import jsonhelper.CopyRequest;
import jsonhelper.DataReturn;
import jsonhelper.ExceptionReturn;
import jsonhelper.PathRequest;
import jsonhelper.ReadRequest;
import jsonhelper.ServerInfo;
import jsonhelper.SizeReturn;
import jsonhelper.WriteRequest;
import test.Config;
import test.DFSException;
import test.common.Path;
import test.util.TemporaryDirectory;
import test.util.TestFailed;

/** Tests storage server <code>copy</code> method.

    <p>
    The test starts two storage servers and a test naming server. It then checks
    properties of the <code>copy</code> method.

    <p>
    Properties checked are:
    <ul>
    <li><code>copy</code> rejects <code>null</code> for any of its
        arguments.</li>
    <li><code>copy</code> rejects source directories and non-existent source
        files.</li>
    <li><code>copy</code> correctly creates new destination files and their
        parent directories.</li>
    <li><code>copy</code> correctly replaces existing destination files.</li>
    </ul>
 */
public class ReplicationTest extends StorageTest
{
    /** Test notice. */
    public static final String  notice =
        "checking storage server replication process";
    /** Prerequisites. */
    public static final Class[] prerequisites = new Class[] {AccessTest.class};

    public static final int     SECOND_CLIENT_PORT = 7010;
    public static final int     SECOND_COMMAND_PORT = 7011;

    // root directory of the second storage server.
    private String              second_root_dir = null;
    /** Second temporary directory for the second (source) storage server. */
    private TemporaryDirectory  second_directory = null;
    /** Second (source) storage server. */
    private Process             second_server = null;
    /** Client service interface for the second storage server. */
    private ServerInfo          second_stub = null;

    /** File to be copied. This file already exists on the destination
        server. */
    private final Path          replace_path = new Path("/file4");
    /** File to be copied. This file is new on the destination server. */
    private final Path          create_path = new Path("/replicate/file5");

    /** Creates the <code>ReplicationTest</code> object. */
    public ReplicationTest()
    {
        super(new String[][] {new String[] {"file4"}}, null);
    }

    /** Tests the copy method.

        @throws TestFailed If any of the tests fail.
     */
    @Override
    protected void perform() throws TestFailed
    {
        testBadValues();
        testCreation();
        testReplacement();
    }

    /** Checks that the <code>copy</code> method correctly creates new files.

        @throws TestFailed If the test fails.
     */
    private void testCreation() throws TestFailed
    {
        HttpResponse<String> response = null;
        // Data stored in the file to be copied.
        byte[]  data = "data".getBytes();
        String base64String = Base64.getEncoder().encodeToString(data);

        // Attempt to write the data to the file on the source server.
        try
        {
            WriteRequest writeRequest = new WriteRequest(create_path.toString(), 0, base64String);
            response = getResponse("/storage_write", this.second_stub.server_port, writeRequest);
            boolean success = gson.fromJson(response.body(), BooleanReturn.class).success;
            if (!success) {
                throw new TestFailed("unable to write data to file to be copied");
            }
        }
        catch(Throwable t)
        {
            throw new TestFailed("unable to write data to file to be copied",
                                 t);
        }

        // Copy the file to the destination server, creating its parent
        // directory.
        try
        {
            CopyRequest copyRequest = new CopyRequest(create_path.toString(), second_stub.server_ip, second_stub.server_port);
            response = getResponse("/storage_copy", this.command_stub.server_port, copyRequest);
            boolean success = gson.fromJson(response.body(), BooleanReturn.class).success;
            if(!success)
            {
                throw new TestFailed("unable to create new file by " +
                                     "replication: copy returned false");
            }
        }
        catch(TestFailed e) { throw e; }
        catch(Throwable t)
        {
            throw new TestFailed("unable to create new file by replication", t);
        }

        // Check that the file copy has the correct size and contents.
        long    resulting_size;
        byte[]  resulting_data;

        try
        {
            PathRequest pathRequest = new PathRequest(create_path.toString());
            response = getResponse("/storage_size", this.client_stub.server_port, pathRequest);
            resulting_size = gson.fromJson(response.body(), SizeReturn.class).size;
        }
        catch(Throwable t)
        {
            throw new TestFailed("unable to retrieve size of created file", t);
        }

        if(resulting_size != (long)data.length)
            throw new TestFailed("created file has incorrect size");

        try
        {
            ReadRequest readRequest = new ReadRequest(create_path.toString(), 0, (int)resulting_size);
            response = getResponse("/storage_read", this.client_stub.server_port, readRequest);
            String dataString = gson.fromJson(response.body(), DataReturn.class).data;
            resulting_data = Base64.getDecoder().decode(dataString);
        }
        catch(Throwable t)
        {
            throw new TestFailed("unable to retrieve contents of created file",
                                 t);
        }

        if(!Arrays.equals(resulting_data, data))
            throw new TestFailed("created file has incorrect contents");
    }

    /** Checks that the <code>copy</code> method correctly replaces existing
        files.

        @throws TestFailed If the test fails.
     */
    private void testReplacement() throws TestFailed
    {
        HttpResponse<String> response = null;
        // Write some data to the file to be replaced; write less data to the
        // file with which it will be replaced. Then, order the file to be
        // replaced. It should be truncated to the size of the copied file.
        byte[]  old_data = "old data".getBytes();
        String base64String_old_data = Base64.getEncoder().encodeToString(old_data);
        byte[]  new_data = "data".getBytes();
        String base64String_new_data = Base64.getEncoder().encodeToString(new_data);

        try
        {
            WriteRequest writeRequest = new WriteRequest(replace_path.toString(), 0, base64String_old_data);
            response = getResponse("/storage_write", this.client_stub.server_port, writeRequest);
            boolean success = gson.fromJson(response.body(), BooleanReturn.class).success;
            if (!success) {
                throw new TestFailed("unable to write data to file to be " +
                                 "replaced on first storage server");
            }
        }
        catch(Throwable t)
        {
            throw new TestFailed("unable to write data to file to be " +
                                 "replaced on first storage server", t);
        }

        try
        {
            WriteRequest writeRequest = new WriteRequest(replace_path.toString(), 0, base64String_new_data);
            response = getResponse("/storage_write", this.second_stub.server_port, writeRequest);
            boolean success = gson.fromJson(response.body(), BooleanReturn.class).success;
            if (!success) {
                throw new TestFailed("unable to write data to replacing file on " +
                    "second storage server");
            }
        }
        catch(Throwable t)
        {
            throw new TestFailed("unable to write data to replacing file on " +
                                 "second storage server", t);
        }

        // Order the file on the first server to be replaced.
        try
        {
            CopyRequest copyRequest = new CopyRequest(replace_path.toString(), second_stub.server_ip, second_stub.server_port);
            response = getResponse("/storage_copy", this.command_stub.server_port, copyRequest);
            boolean success = gson.fromJson(response.body(), BooleanReturn.class).success;
            if(!success)
            {
                throw new TestFailed("unable to replace file by " +
                                     "replication: copy returned false");
            }
        }
        catch(TestFailed e) { throw e; }
        catch(Throwable t)
        {
            throw new TestFailed("unable to replace file by replication", t);
        }

        // Check the size and contents of the file after it is replaced.
        long    resulting_size;
        byte[]  resulting_data;

        try
        {
            PathRequest pathRequest = new PathRequest(replace_path.toString());
            response = getResponse("/storage_size", this.client_stub.server_port, pathRequest);
            resulting_size = gson.fromJson(response.body(), SizeReturn.class).size;
        }
        catch(Throwable t)
        {
            throw new TestFailed("unable to retrieve size of replaced file", t);
        }

        if(resulting_size != (long)new_data.length)
            throw new TestFailed("replaced file has incorrect size");

        try
        {
            ReadRequest readRequest = new ReadRequest(replace_path.toString(), 0, (int)resulting_size);
            response = getResponse("/storage_read", this.client_stub.server_port, readRequest);
            String dataString = gson.fromJson(response.body(), DataReturn.class).data;
            resulting_data = Base64.getDecoder().decode(dataString);
        }
        catch(Throwable t)
        {
            throw new TestFailed("unable to retrieve contents of replaced file",
                                 t);
        }

        if(!Arrays.equals(resulting_data, new_data))
            throw new TestFailed("replaced file has incorrect contents");
    }

    /** Tests that the <code>copy</code> method rejects bad arguments such as
        <code>null</code>.

        @throws TestFailed If any of the tests fail.
     */
    private void testBadValues() throws TestFailed
    {
        CopyRequest copyRequest = null;
        HttpResponse<String> response = null;
        // Attempt to pass null arguments to copy.
        try
        {
            copyRequest = new CopyRequest("", second_stub.server_ip, second_stub.server_port);
            response = getResponse("/storage_copy", this.command_stub.server_port, copyRequest);
            String exception_type = gson.fromJson(response.body(), ExceptionReturn.class).exception_type;
            if (exception_type == null) {
                throw new TestFailed("copy accepted invalid path");
            }
            if (DFSException.valueOf(exception_type) == DFSException.IllegalArgumentException) {
                throw new IllegalArgumentException();
            }
            else {
                throw new IllegalStateException();
            }
        }
        catch(TestFailed e) { throw e; }
        catch(IllegalArgumentException e) { }
        catch(Throwable t)
        {
            throw new TestFailed("copy threw unexpected exception when given " +
                                 "invalid path", t);
        }

        // Attempt to copy a non-existent file.
        try
        {
            copyRequest = new CopyRequest("/absent-file", second_stub.server_ip, second_stub.server_port);
            response = getResponse("/storage_copy", this.command_stub.server_port, copyRequest);
            String exception_type = gson.fromJson(response.body(), ExceptionReturn.class).exception_type;
            if (exception_type == null) {
                throw new TestFailed("copy succeeded when given missing file");
            }
            if (DFSException.valueOf(exception_type) == DFSException.FileNotFoundException) {
                throw new FileNotFoundException();
            }
            else {
                throw new IllegalStateException();
            }
        }
        catch(TestFailed e) { throw e; }
        catch(FileNotFoundException e) { }
        catch(Throwable t)
        {
            throw new TestFailed("copy threw unexpected exception when given " +
                                 "missing file", t);
        }

        // Attempt to copy a directory.
        try
        {
            copyRequest = new CopyRequest("/replicate", second_stub.server_ip, second_stub.server_port);
            response = getResponse("/storage_copy", this.command_stub.server_port, copyRequest);
            String exception_type = gson.fromJson(response.body(), ExceptionReturn.class).exception_type;
            if (exception_type == null) {
                throw new TestFailed("copy succeeded when given directory");
            }
            if (DFSException.valueOf(exception_type) == DFSException.FileNotFoundException) {
                throw new FileNotFoundException();
            }
            else {
                throw new IllegalStateException();
            }
        }
        catch(TestFailed e) { throw e; }
        catch(FileNotFoundException e) { }
        catch(Throwable t)
        {
            throw new TestFailed("copy threw unexpected exception when given " +
                                 "directory", t);
        }
    }

    /**
     * Initializes the test.
     * 
     * <p>
     * This implementation calls the superclass implementation, and additionally
     * creates a second temporary directory and starts a second storage server.
     * 
     * @throws TestFailed           If initialization fails.
     * @throws InterruptedException
     * @throws IOException
     */
    @Override
    protected void initialize() throws TestFailed, IOException, InterruptedException
    {
        // Initialize the naming server and the first storage server. If that
        // initialization fails, return immediately.
        super.initialize();

        // get the root directory of the storage server
        String[] splits = Config.startStorage1.split(" ");
        this.second_root_dir = splits[splits.length - 1];

        // Create a second temporary directory.
        try {
            this.second_directory = new TemporaryDirectory(this.second_root_dir);
        }
        catch(Throwable t)
        {
            throw new TestFailed("unable to create the second temporary directory " + this.second_root_dir,
                                 t);
        }

        // populate the second root directory with expected files.
        try
        {
            second_directory.add(new String[] {"file4"});
            second_directory.add(new String[] {"replicate", "file5"});
        }
        catch(Throwable t)
        {
            throw new TestFailed("unable to add file to second temporary " +
                                 "directory" , t);
        }

        // Set the expected files for the registration of the second storage
        // server.
        naming_server.expectFiles(new Path[] {replace_path, create_path});
        naming_server.deleteFiles(null);

        // start the second storage server according to the command line specified in Config.java
        if (SECOND_CLIENT_PORT != Integer.parseInt(splits[splits.length - 4])) {
            throw new TestFailed("StorgeServer0 Storage Port should be " + SECOND_CLIENT_PORT + " not " + splits[splits.length - 4] +
                " Please change the port number in Config.java!");
        }
        if (SECOND_COMMAND_PORT != Integer.parseInt(splits[splits.length - 3])) {
            throw new TestFailed("StorgeServer0 Command Port should be " + SECOND_COMMAND_PORT + " not " + splits[splits.length - 3] +
                " Please change the port number in Config.java!");
        }
        Socket storage_socket;
        Socket registration_socket;
        try {
            Runtime runtime = Runtime.getRuntime();
            second_server = runtime.exec(Config.startStorage1);
        } catch (Throwable t) {
            throw new TestFailed("unable to start the second storage server");
        }
        
        // Attempt to make the connection.
        while (true) {
            try {
                storage_socket = new Socket();
                storage_socket.connect(new InetSocketAddress("127.0.0.1", SECOND_CLIENT_PORT));
                registration_socket = new Socket();
                registration_socket.connect(new InetSocketAddress("127.0.0.1", SECOND_COMMAND_PORT));
                break;
            }
            catch (IOException e) {
                // Ignore the exception to give server some time to start up
            }
        }

        // Make a best effort to close the socket if the connection is successful.
        try {
            storage_socket.close();
            registration_socket.close();
        }
        catch(IOException e) { 
            e.printStackTrace();
        }

        // Retrieve storage server client interface stub.
        second_stub = naming_server.clientInterface();
    }

    /** Stops all servers and removes all temporary directories.

        <p>
        This implementation stops the second storage server and removes its
        temporary directory, in addition to calling the superclass
        implementation.
     */
    @Override
    protected void clean()
    {
        super.clean();
        
        if (second_server != null) {
            kill(second_server.toHandle());
            // Wait for the naming server to stop.
            try {
                second_server.waitFor();
            } catch (InterruptedException e) { }
            second_server = null;
        }

        if(second_directory != null)
        {
            second_directory.remove();
            second_directory = null;
        }
    }
}
