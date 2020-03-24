package test.storage;

import java.io.*;
import java.net.http.HttpResponse;
import java.util.*;

import jsonhelper.BooleanReturn;
import jsonhelper.DataReturn;
import jsonhelper.ExceptionReturn;
import jsonhelper.PathRequest;
import jsonhelper.ReadRequest;
import jsonhelper.SizeReturn;
import jsonhelper.WriteRequest;
import test.DFSException;
import test.common.Path;
import test.util.TestFailed;

/** Tests storage server file access methods.

    <p>
    This test starts a storage server and a special testing naming server. It
    then obtains a stub for the storage server and checks several properties of
    the <code>read</code>, <code>write</code>, and <code>size</code> methods.

    <p>test
    Properties checked are:
    <ul>
    <li><code>read</code>, <code>write</code>, and <code>size</code> have
        correct behavior for non-existent files and directories.</li>
    <li><code>read</code>, <code>write</code>, and <code>size</code> respond
        correctly to <code>null</code> arguments.</li>
    <li><code>read</code> and <code>write</code> respond correctly to
        out-of-bounds arguments.</li>
    <li><code>read</code>, <code>write</code>, and <code>size</code> have
        correct behavior when given valid arguments.</li>
    <li><code>write</code> performs random access on files.</li>
    </ul>

    <p>
    The effects of <code>write</code> are checked directly by reading the file
    locally, and by reading it through the storage server stub.
 */
public class AccessTest extends StorageTest
{
    /** Test notice. */
    public static final String  notice =
        "checking storage server file access methods (size, read, write)";
    /** Prerequisites. */
    public static final Class[] prerequisites =
        new Class[] {RegistrationTest.class};

    /** File that is not present on the storage server. */
    private final Path          absent_file = new Path("/absent");
    /** Path to a directory on the storage server. */
    private final Path          directory_file = new Path("/subdirectory");
    /** An empty file on the storage server. */
    private final Path          empty_file = new Path("/subdirectory/file2");
    /** File on the storage server to be used for reading and writing tests. */
    private final Path          read_write_file =
                                            new Path("/subdirectory/file1");

    /** Small buffer of data to be used in writing tests. */
    private final byte[]        write_data = "test data".getBytes();

    /** Creates the <code>AccessTest</code> object. */
    public AccessTest()
    {
        super(new String[][] {new String[] {"subdirectory", "file1"},
                              new String[] {"subdirectory", "file2"},
                              new String[] {"file3"},
                              new String[] {"subdirectory", "subdirectory2",
                                            "file1"}},
              null);
    }

    /** Tests the server file access methods.

        @throws TestFailed If any of the tests fail.
     */
    @Override
    protected void perform() throws TestFailed
    {
        testSize();
        testReadBasic();
        testWriteBasic();
        testReadWrite();
        testReadWriteBounds();
        testAppend();
    }

    /** Tests the <code>write</code> method with valid arguments.

        @throws TestFailed If the test fails.
     */
    private void testReadWrite() throws TestFailed
    {
        WriteRequest writeRequest = null;
        HttpResponse<String> response = null;
        String base64String = Base64.getEncoder().encodeToString(write_data);
        // Write test data to file. Check that the data is present in the file
        // by accessing it directly in the server's temporary directory, and by
        // asking the server to retrieve the data. Check also that the file size
        // reported is correct.
        try
        {
            writeRequest = new WriteRequest(read_write_file.toString(), 0, base64String);
            response = getResponse("/storage_write", this.client_stub.server_port, writeRequest);
            boolean success = gson.fromJson(response.body(), BooleanReturn.class).success;
            if (!success) {
                throw new TestFailed("unable to write to file");
            }
        }
        catch(Throwable t)
        {
            throw new TestFailed("unable to write to file", t);
        }

        // Check the file locally. First check the file's presence, kind, and
        // size.
        File            direct_access =
            new File(directory.root(), "subdirectory");
        direct_access = new File(direct_access, "file1");

        if(!direct_access.exists())
            throw new TestFailed("file does not exist after writing");

        if(direct_access.isDirectory())
            throw new TestFailed("file is a directory after writing");

        if(direct_access.length() != write_data.length)
            throw new TestFailed("file has incorrect size after writing");

        // Read the file directly from the local fileystem and check its
        // contents.
        FileInputStream stream;

        try
        {
            stream = new FileInputStream(direct_access);
        }
        catch(Throwable t)
        {
            throw new TestFailed("unable to open written file directly for " +
                                 "reading", t);
        }

        byte[]          local_data;

        try
        {
            local_data = new byte[write_data.length];

            if(stream.read(local_data, 0, write_data.length) !=
                write_data.length)
            {
                throw new TestFailed("end of file reached while reading " +
                                     "written data directly");
            }
        }
        catch(IOException e)
        {
            throw new TestFailed("unable to read written file directly", e);
        }
        finally
        {
            // No matter the circumstances, attempt to close the input stream.
            try
            {
                stream.close();
            }
            catch(Throwable t) { }
        }

        if(!Arrays.equals(local_data, write_data))
        {
            throw new TestFailed("data retrieved from written file directly " +
                                 "do not match written data");
        }

        // Check the file's size and contents through the storage server stub.
        try
        {
            PathRequest pathRequest = new PathRequest(read_write_file.toString());
            response = getResponse("/storage_size", this.client_stub.server_port, pathRequest);
            long size = gson.fromJson(response.body(), SizeReturn.class).size;
            if(size != write_data.length)
            {
                throw new TestFailed("stub reports incorrect file size after " +
                                     "data is written to file");
            }
        }
        catch(TestFailed e) { throw e; }
        catch(Throwable t)
        {
            throw new TestFailed("unable to retrieve written file size " +
                                 "through stub", t);
        }

        try
        {
            ReadRequest readRequest = new ReadRequest(read_write_file.toString(), 0, write_data.length);
            response = getResponse("/storage_read", this.client_stub.server_port, readRequest);
            String data = gson.fromJson(response.body(), DataReturn.class).data;
            byte[] remote_data = Base64.getDecoder().decode(data);

            if(!Arrays.equals(remote_data, write_data))
            {
                throw new TestFailed("data retrieved through stub from " +
                                     "written file does not match written " +
                                     "data");
            }
        }
        catch(TestFailed e) { throw e; }
        catch(Throwable t)
        {
            throw new TestFailed("unable to retrieve written file contents " +
                                 "through stub", t);
        }
    }

    /** Tests <code>read</code> and <code>write</code> with arguments that are
        out of file bounds.

        @throws TestFailed If the test fails.
     */
    private void testReadWriteBounds() throws TestFailed
    {
        ReadRequest readRequest = null;
        HttpResponse<String> response = null;
        // Try to perform several reads that are not within the bounds of
        // read_write_file.
        try
        {
            readRequest = new ReadRequest(read_write_file.toString(), -1, write_data.length + 1);
            response = getResponse("/storage_read", this.client_stub.server_port, readRequest);
            String exception_type = gson.fromJson(response.body(), ExceptionReturn.class).exception_type;
            if (exception_type == null) {
                throw new TestFailed("read method allowed negative offset");
            }
            if (DFSException.valueOf(exception_type) == DFSException.IndexOutOfBoundsException) {
                throw new IndexOutOfBoundsException();
            }
            else {
                throw new IllegalStateException();
            }
        }
        catch(TestFailed e) { throw e; }
        catch(IndexOutOfBoundsException e) { }
        catch(Throwable t)
        {
            throw new TestFailed("read method raised unexpected exception " +
                                 "when reading from negative offset", t);
        }

        try
        {
            readRequest = new ReadRequest(read_write_file.toString(), 0, write_data.length + 1);
            response = getResponse("/storage_read", this.client_stub.server_port, readRequest);
            String exception_type = gson.fromJson(response.body(), ExceptionReturn.class).exception_type;
            if (exception_type == null) {
                throw new TestFailed("read method allowed reading past end of the file");
            }
            if (DFSException.valueOf(exception_type) == DFSException.IndexOutOfBoundsException) {
                throw new IndexOutOfBoundsException();
            }
            else {
                throw new IllegalStateException();
            }
        }
        catch(TestFailed e) { throw e; }
        catch(IndexOutOfBoundsException e) { }
        catch(Throwable t)
        {
            throw new TestFailed("read method raised unexpected exception " +
                                 "when reading past end of file", t);
        }

        try
        {
            readRequest = new ReadRequest(read_write_file.toString(), write_data.length, write_data.length);
            response = getResponse("/storage_read", this.client_stub.server_port, readRequest);
            String exception_type = gson.fromJson(response.body(), ExceptionReturn.class).exception_type;
            if (exception_type == null) {
                throw new TestFailed("read method allowed offset outside of file");
            }
            if (DFSException.valueOf(exception_type) == DFSException.IndexOutOfBoundsException) {
                throw new IndexOutOfBoundsException();
            }
            else {
                throw new IllegalStateException();
            }
        }
        catch(TestFailed e) { throw e; }
        catch(IndexOutOfBoundsException e) { }
        catch(Throwable t)
        {
            throw new TestFailed("read method raised unexpected exception " +
                                 "when offset is outside of file", t);
        }

        // Try to perform a read with negative length.
        try
        {
            readRequest = new ReadRequest(read_write_file.toString(), 0, -write_data.length);
            response = getResponse("/storage_read", this.client_stub.server_port, readRequest);
            String exception_type = gson.fromJson(response.body(), ExceptionReturn.class).exception_type;
            if (exception_type == null) {
                throw new TestFailed("read method allowed read with negative length");
            }
            if (DFSException.valueOf(exception_type) == DFSException.IndexOutOfBoundsException) {
                throw new IndexOutOfBoundsException();
            }
            else {
                throw new IllegalStateException();
            }
        }
        catch(TestFailed e) { throw e; }
        catch(IndexOutOfBoundsException e) { }
        catch(Throwable t)
        {
            throw new TestFailed("read method raised unexpected exception " +
                                 "when given negative length", t);
        }

        // Try to perform a write with a negative offset.
        try
        {
            String base64String = Base64.getEncoder().encodeToString(write_data);
            WriteRequest writeRequest = new WriteRequest(read_write_file.toString(), -1, base64String);
            response = getResponse("/storage_write", this.client_stub.server_port, writeRequest);
            String exception_type = gson.fromJson(response.body(), ExceptionReturn.class).exception_type;
            if (exception_type == null) {
                throw new TestFailed("write method raised unexpected exception when given negative offset");
            }
            if (DFSException.valueOf(exception_type) == DFSException.IndexOutOfBoundsException) {
                throw new IndexOutOfBoundsException();
            }
            else {
                throw new IllegalStateException();
            }
        }
        catch(TestFailed e) { throw e; }
        catch(IndexOutOfBoundsException e) { }
        catch(Throwable t)
        {
            throw new TestFailed("write method raised unexpected exception " +
                                 "when given negative offset", t);
        }
    }

    /** Tests <code>write</code> random access capability.

        @throws TestFailed If the test fails.
     */
    private void testAppend() throws TestFailed
    {
        HttpResponse<String> response = null;
        // Write data outside the current bounds of read_write_file and check
        // the size of the resulting file.
        try
        {
            String base64String = Base64.getEncoder().encodeToString(write_data);
            WriteRequest writeRequest = new WriteRequest(read_write_file.toString(), write_data.length + 1, base64String);
            response = getResponse("/storage_write", this.client_stub.server_port, writeRequest);
            boolean success = gson.fromJson(response.body(), BooleanReturn.class).success;
            if (!success) {
                throw new TestFailed("unable to append to file");
            }
        }
        catch(Throwable t)
        {
            throw new TestFailed("unable to append data to file", t);
        }

        // Check that the file has a correct new size.
        long    size;

        try
        {
            PathRequest pathRequest = new PathRequest(read_write_file.toString());
            response = getResponse("/storage_size", this.client_stub.server_port, pathRequest);
            size = gson.fromJson(response.body(), SizeReturn.class).size;
        }
        catch(Throwable t)
        {
            throw new TestFailed("unable to retrieve file size after " +
                                 "appending", t);
        }

        if(size != write_data.length + 1 + write_data.length)
            throw new TestFailed("file has incorrect size after appending");
    }

    /** Tests the <code>size</code> method with bad arguments.

        @throws TestFailed If the test fails.
     */
    private void testSize() throws TestFailed
    {
        PathRequest pathRequest = null;
        HttpResponse<String> response = null;
        // Try to get the size of an absent file.
        try
        {
            pathRequest = new PathRequest(absent_file.toString());
            response = getResponse("/storage_size", this.client_stub.server_port, pathRequest);
            String exception_type = gson.fromJson(response.body(), ExceptionReturn.class).exception_type;
            if (exception_type == null) {
                throw new TestFailed("size method doesn't return Exception for non-existent file");
            }
            if (DFSException.valueOf(exception_type) == DFSException.FileNotFoundException) {
                throw new FileNotFoundException();
            }
            else {
                throw new IllegalStateException();
            }
        }
        catch(FileNotFoundException e) { }
        catch(TestFailed e) { throw e; }
        catch(Throwable t)
        {
            throw new TestFailed("size method threw unexpected exception " +
                                 "when accessing non-existent file", t);
        }

        // Try to get the size of a directory.
        try
        {
            pathRequest = new PathRequest(directory_file.toString());
            response = getResponse("/storage_size", this.client_stub.server_port, pathRequest);
            String exception_type = gson.fromJson(response.body(), ExceptionReturn.class).exception_type;
            if (exception_type == null) {
                throw new TestFailed("size method doesn't return Exception for directory");
            }
            if (DFSException.valueOf(exception_type) == DFSException.FileNotFoundException) {
                throw new FileNotFoundException();
            }
            else {
                throw new IllegalStateException();
            }
        }
        catch(FileNotFoundException e) { }
        catch(TestFailed e) { throw e; }
        catch(Throwable t)
        {
            throw new TestFailed("size method threw unexpected exception " +
                                 "when accessing directory", t);
        }

        // Try to get the size of an empty file.
        try
        {
            pathRequest = new PathRequest(empty_file.toString());
            response = getResponse("/storage_size", this.client_stub.server_port, pathRequest);
            long size = gson.fromJson(response.body(), SizeReturn.class).size;
            if (size != 0) {
                throw new TestFailed("size method returned nonzero result " +
                                     "for empty file");
            }
        }
        catch(TestFailed e) { throw e; }
        catch(Throwable t)
        {
            throw new TestFailed("size method threw unexpected exception " +
                                 "when accessing empty file", t);
        }

        // Try to call size with null as argument.
        try
        {
            pathRequest = new PathRequest("");
            response = getResponse("/storage_size", this.client_stub.server_port, pathRequest);
            String exception_type = gson.fromJson(response.body(), ExceptionReturn.class).exception_type;
            if (exception_type == null) {
                throw new TestFailed("size method doesn't return Exception when given null as argument");
            }
            if (DFSException.valueOf(exception_type) == DFSException.IllegalArgumentException) {
                throw new IllegalArgumentException();
            }
            else {
                throw new IllegalStateException();
            }
        }
        catch(IllegalArgumentException e) {}
        catch(TestFailed e) { throw e; }
        catch(Throwable t)
        {
            throw new TestFailed("size method threw unexpected exception " +
                                 "when given null as argument", t);
        }
    }

    /** Tests the <code>read</code> method with bad arguments.

        @throws TestFailed If the test fails.
     */
    private void testReadBasic() throws TestFailed
    {        
        ReadRequest readRequest = null;
        HttpResponse<String> response = null;
        // Try to read from a non-existent file.
        try
        {
            readRequest = new ReadRequest(absent_file.toString(), 0, 0);
            response = getResponse("/storage_read", this.client_stub.server_port, readRequest);
            String exception_type = gson.fromJson(response.body(), ExceptionReturn.class).exception_type;
            if (exception_type == null) {
                throw new TestFailed("read method doesn't return Exception for non-existent file");
            }
            if (DFSException.valueOf(exception_type) == DFSException.FileNotFoundException) {
                throw new FileNotFoundException();
            }
            else {
                throw new IllegalStateException();
            }
        }
        catch(FileNotFoundException e) { }
        catch(TestFailed e) { throw e; }
        catch(Throwable t)
        {
            throw new TestFailed("read method threw unexpected exception " +
                                 "when accessing non-existent file", t);
        }

        // Try to read from a directory.
        try
        {
            readRequest = new ReadRequest(directory_file.toString(), 0, 0);
            response = getResponse("/storage_read", this.client_stub.server_port, readRequest);
            String exception_type = gson.fromJson(response.body(), ExceptionReturn.class).exception_type;
            if (exception_type == null) {
                throw new TestFailed("read method doesn't return Exception for directory");
            }
            if (DFSException.valueOf(exception_type) == DFSException.FileNotFoundException) {
                throw new FileNotFoundException();
            }
            else {
                throw new IllegalStateException();
            }
        }
        catch(FileNotFoundException e) { }
        catch(TestFailed e) { throw e; }
        catch(Throwable t)
        {
            throw new TestFailed("read method threw unexpected exception " +
                                 "when accessing directory", t);
        }

        // Try to read from an empty file.
        try
        {
            readRequest = new ReadRequest(empty_file.toString(), 0, 0);
            response = getResponse("/storage_read", this.client_stub.server_port, readRequest);
            String data = gson.fromJson(response.body(), DataReturn.class).data;
            byte[] result = Base64.getDecoder().decode(data);

            if(result == null)
            {
                throw new TestFailed("read method returned null when reading " +
                                     "from empty file");
            }

            if(result.length != 0)
            {
                throw new TestFailed("read method returned incorrect number " +
                                     "of bytes when reading empty file");
            }
        }
        catch(TestFailed e) { throw e; }
        catch(Throwable t)
        {
            throw new TestFailed("read method threw unexpected exception " +
                                 "when reading from empty file", t);
        }

        // Call read with null as the file argument.
        try
        {
            readRequest = new ReadRequest("", 0, 0);
            response = getResponse("/storage_read", this.client_stub.server_port, readRequest);
            String exception_type = gson.fromJson(response.body(), ExceptionReturn.class).exception_type;
            if (exception_type == null) {
                throw new TestFailed("read method doesn't return Exception when given null as argument");
            }
            if (DFSException.valueOf(exception_type) == DFSException.IllegalArgumentException) {
                throw new IllegalArgumentException();
            }
            else {
                throw new IllegalStateException();
            }
        }
        catch(IllegalArgumentException e) { }
        catch(TestFailed e) { throw e; }
        catch(Throwable t)
        {
            throw new TestFailed("read method threw unexpected exception " +
                                 "when given null as argument", t);
        }
    }

    /** Tests the <code>write</code> method with bad arguments.

        @throws TestFailed If the test fails.
     */
    private void testWriteBasic() throws TestFailed
    {
        WriteRequest writeRequest = null;
        HttpResponse<String> response = null;
        String base64String = Base64.getEncoder().encodeToString(write_data);
        // Try to write to a non-existent file.
        try
        {
            writeRequest = new WriteRequest(absent_file.toString(), 0, base64String);
            response = getResponse("/storage_write", this.client_stub.server_port, writeRequest);
            String exception_type = gson.fromJson(response.body(), ExceptionReturn.class).exception_type;
            if (exception_type == null) {
                throw new TestFailed("write method doesn't return Exception for non-existent file");
            }
            if (DFSException.valueOf(exception_type) == DFSException.FileNotFoundException) {
                throw new FileNotFoundException();
            }
            else {
                throw new IllegalStateException();
            }
        }
        catch(FileNotFoundException e) { }
        catch(TestFailed e) { throw e; }
        catch(Throwable t)
        {
            throw new TestFailed("write method threw unexpected exception " +
                                 "when accessing non-existent file", t);
        }

        // Try to write to a directory.
        try
        {
            writeRequest = new WriteRequest(directory_file.toString(), 0, base64String);
            response = getResponse("/storage_write", this.client_stub.server_port, writeRequest);
            String exception_type = gson.fromJson(response.body(), ExceptionReturn.class).exception_type;
            if (exception_type == null) {
                throw new TestFailed("write method doesn't return Exception for directory");
            }
            if (DFSException.valueOf(exception_type) == DFSException.FileNotFoundException) {
                throw new FileNotFoundException();
            }
            else {
                throw new IllegalStateException();
            }
        }
        catch(FileNotFoundException e) { }
        catch(TestFailed e) { throw e; }
        catch(Throwable t)
        {
            throw new TestFailed("write method threw unexpected exception " +
                                 "when accessing directory", t);
        }

        // Try to call write with null as the path.
        try
        {
            writeRequest = new WriteRequest("", 0, base64String);
            response = getResponse("/storage_write", this.client_stub.server_port, writeRequest);
            String exception_type = gson.fromJson(response.body(), ExceptionReturn.class).exception_type;
            if (exception_type == null) {
                throw new TestFailed("write method doesn't return Exception when given null for path argument");
            }
            if (DFSException.valueOf(exception_type) == DFSException.IllegalArgumentException) {
                throw new IllegalArgumentException();
            }
            else {
                throw new IllegalStateException();
            }
        }
        catch(IllegalArgumentException e) { }
        catch(TestFailed e) { throw e; }
        catch(Throwable t)
        {
            throw new TestFailed("write method threw unexpected exception " +
                                 "when given null for path argument", t);
        }
    }
}
