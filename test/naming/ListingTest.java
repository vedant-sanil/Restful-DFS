package test.naming;

import jsonhelper.BooleanReturn;
import jsonhelper.ExceptionReturn;
import jsonhelper.FilesReturn;
import jsonhelper.PathRequest;
import test.DFSException;
import test.common.Path;
import test.util.TestFailed;
import test.util.TestUtil;
import java.io.IOException;
import java.net.http.HttpResponse;

/** Tests the naming server <code>list</code> and <code>isDirectory</code>
    methods.

    <p>
    Items checked are:
    <ul>
    <li><code>list</code> and <code>isDirectory</code> reject <code>null</code>
        pointer arguments.</li>
    <li><code>list</code> and <code>isDirectory</code> reject non-existent
        objects as arguments.</li>
    <li><code>list</code> rejects files as arguments.</li>
    <li>The root directory is reported as a directory.</li>
    <li>Diretories are listed correctly, and <code>isDirectory</code> returns
        the correct results for files and directories.</li>
    </ul>
 */
public class ListingTest extends NamingTest
{
    /** Test notice. */
    public static final String  notice =
        "checking naming server directory listing methods (list, isDirectory)";
    /** Prerequisites. */
    public static final Class[] prerequisites =
        new Class[] {RegistrationTest.class};

    /** Storage server providing the files used in the test. */
    private TestStorageServer   storage_server;

    private final Path          file1 = new Path("/file");
    private final Path          file2 = new Path("/directory/file");
    private final Path          file3 = new Path("/directory/another_file");

    /** Creates the <code>ListTest</code> object and sets the notice. */
    public ListingTest() throws IOException
    {
        storage_server = new TestStorageServer(this);
    }

    /** Performs the tests.

        @throws TestFailed If any of the tests fail.
     */
    @Override
    protected void perform() throws TestFailed
    {
        checkArguments();

        checkDirectoryListing("/", new String[] {"file", "directory"},
                new boolean[] {false, true});
        checkDirectoryListing("/directory",
                new String[] {"file", "another_file"},
                new boolean[] {false, false});
    }

    /** Checks that directories are listed correctly, and checks that files and
        directories are distinguished correctly by <code>isDirectory</code>.

        @param path The directory to be listed.
        @param expected_children The expected children of the directory.
        @param expected_kinds An array of expected results of
                              <code>isDirectory</code> calls. The calls will be
                              made for each child of the directory, in the order
                              they are given in <code>expected_children</code>.
        @throws TestFailed If the directory is not listed correctly, or if
                           <code>isDirectory</code> misidentifies a directory or
                           a file.
     */
    private void checkDirectoryListing(String path, String[] expected_children,
                                       boolean[] expected_kinds)
        throws TestFailed
    {
        // List the given directory.
        String[]    listing;
        HttpResponse<String> response;
        PathRequest request;
        String      exception_type;

        try
        {
            request = new PathRequest(path);
            response = getResponse("/list", SERVICE_PORT, request);
            exception_type = gson.fromJson(response.body(), ExceptionReturn.class).exception_type;
            if(exception_type != null)
            {
                throw new Throwable(response.body());
            }
            listing = gson.fromJson(response.body(), FilesReturn.class).files;
        }
        catch(Throwable t)
        {
            throw new TestFailed("unable to list directory " + path, t);
        }

        if(listing == null)
        {
            throw new TestFailed("listing directory " + path + " resulted in " +
                                 "null");
        }

        // Compare the result of the list method and the set of expected
        // children.
        if(!TestUtil.sameElements(listing, expected_children))
        {
            throw new TestFailed("directory listing incorrect for " + path);
        }

        // Go through all the expected children. For each child, ensure that the
        // isDirectory method returns the correct result.
        for(int child_index = 0; child_index < expected_children.length;
            ++child_index)
        {
            Path    child_path = new Path(path + "/" + expected_children[child_index]);
            boolean is_directory;
            request = new PathRequest(child_path.toString());

            try
            {
                response = getResponse("/is_directory", SERVICE_PORT, request);
                exception_type = gson.fromJson(response.body(), ExceptionReturn.class).exception_type;
                if(exception_type != null)
                {
                    throw new Throwable(response.body());
                }
                is_directory = gson.fromJson(response.body(), BooleanReturn.class).success;
            }
            catch(Throwable t)
            {
                throw new TestFailed("unable to determine if " + child_path +
                                     " is a directory", t);
            }

            if(is_directory && !expected_kinds[child_index])
            {
                throw new TestFailed(child_path + " is reported as a " +
                                     "directory but is in fact a file");
            }
            if(!is_directory && expected_kinds[child_index])
            {
                throw new TestFailed(child_path + " is reported as a file " +
                                     "but is in fact a directory");
            }
        }
    }

    /** Checks that bad arguments are rejected, and checks that the root
        directory is identified as a directory.

        @throws TestFailed If any of the methods accept a bad argument, or if
                           the root directory is reported to be a file.
     */
    private void checkArguments() throws TestFailed
    {
        HttpResponse<String> response;

        // Try to call isDirectory with null as the path.
        try
        {
            response = getResponse("/is_directory", SERVICE_PORT, new PathRequest(""));
            ExceptionReturn exception = gson.fromJson(response.body(), ExceptionReturn.class);

            if(exception.exception_type == null)
            {
                throw new TestFailed("isDirectory accepted empty string as argument");

            }
            else if(DFSException.valueOf(exception.exception_type) != DFSException.IllegalArgumentException)
            {
                throw new Throwable(response.body());
            }
        }
        catch(TestFailed e) { throw e; }
        catch(Throwable t)
        {
            throw new TestFailed("isDirectory threw unexpected exception " +
                                 "when given null as argument", t);
        }

        // Try to call list with null as the path.
        try
        {
            response = getResponse("/list", SERVICE_PORT, new PathRequest(""));
            ExceptionReturn exception = gson.fromJson(response.body(), ExceptionReturn.class);

            if(exception.exception_type == null)
            {
                throw new TestFailed("list accepted empty string as argument");

            }
            else if(DFSException.valueOf(exception.exception_type) != DFSException.IllegalArgumentException)
            {
                throw new Throwable(response.body());
            }
        }
        catch(TestFailed e) { throw e; }
        catch(Throwable t)
        {
            throw new TestFailed("list threw unexpected exception when given " +
                                 "null as argument", t);
        }

        // Try to call isDirectory with the path of a non-existent object.
        PathRequest        non_existent_path = new PathRequest("/another_file");
        try
        {
            response = getResponse("/is_directory", SERVICE_PORT, non_existent_path);
            ExceptionReturn exception = gson.fromJson(response.body(), ExceptionReturn.class);

            if(exception.exception_type == null)
            {
                throw new TestFailed("isDirectory accepted a non-existent path as argument");
            }

            if(DFSException.valueOf(exception.exception_type) != DFSException.FileNotFoundException)
            {
                throw new Throwable(response.body());
            }
        }
        catch(TestFailed e) { throw e; }
        catch(Throwable t)
        {
            throw new TestFailed("isDirectory threw unexpected exception " +
                                 "when given a non-existent path as argument",
                                 t);
        }

        // Try to call list with the path of a non-existent object.
        try
        {
            response = getResponse("/list", SERVICE_PORT, non_existent_path);
            ExceptionReturn exception = gson.fromJson(response.body(), ExceptionReturn.class);

            if(exception.exception_type == null)
            {
                throw new TestFailed("list accepted a non-existent path as argument");

            }
            else if(DFSException.valueOf(exception.exception_type) != DFSException.FileNotFoundException)
            {
                throw new Throwable(response.body());
            }
        }
        catch(TestFailed e) { throw e; }
        catch(Throwable t)
        {
            throw new TestFailed("list threw unexpected exception when given " +
                                 "a non-existent path as argument", t);
        }

        // Try to call list on a file.
        try
        {
            response = getResponse("/list", SERVICE_PORT, new PathRequest(file1.toString()));
            ExceptionReturn exception = gson.fromJson(response.body(), ExceptionReturn.class);

            if(exception.exception_type == null)
            {
                throw new TestFailed("list accepted a file as argument");

            }
            else if(DFSException.valueOf(exception.exception_type) != DFSException.FileNotFoundException)
            {
                throw new Throwable(response.body());
            }
        }
        catch(TestFailed e) { throw e; }
        catch(Throwable t)
        {
            throw new TestFailed("list threw unexpected exception when given " +
                                 "a file as argument", t);
        }

        // Call isDirectory on the root directory, and ensure the result is
        // true.
        boolean     root_is_directory;
        String      exception_type;

        try
        {
            response = getResponse("/is_directory", SERVICE_PORT, new PathRequest("/"));
            exception_type = gson.fromJson(response.body(), ExceptionReturn.class).exception_type;
            if(exception_type != null)
            {
                throw new Throwable(response.body());
            }
            root_is_directory = gson.fromJson(response.body(), BooleanReturn.class).success;
        }
        catch(Throwable t)
        {
            throw new TestFailed("unable to determine if the root is a " +
                                 "directory", t);
        }

        if(!root_is_directory)
            throw new TestFailed("root is not reported as a directory");
    }

    /** Starts the test servers.

        @throws TestFailed If any of the servers cannot be started.
     */
    @Override
    protected void initialize() throws TestFailed
    {
        super.initialize();

        try
        {
            storage_server.start(REGISTRATION_PORT,
                                 new Path[] {file1, file2, file3}, null);
        }
        catch(Throwable t)
        {
            throw new TestFailed("unable to start storage server", t);
        }
    }

    /** Stops test servers upon completion of the test. */
    @Override
    protected void clean()
    {
        super.clean();

        if(storage_server != null)
        {
            storage_server.stop();
            storage_server = null;
        }
    }
}
