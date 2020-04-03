package test.storage;

import java.io.*;

import test.common.Path;
import test.util.TestFailed;
import test.util.TestUtil;

/** Tests the storage server registration process.

    <p>
    This test starts a special testing naming server, creates and populates
    temporary directory, and starts the storage server in that directory. It
    then commands the storage server to connect to the naming server, checking
    that the storage server transmits the correct directory listing and removes
    files from the directory as directed by the naming server. It also checks
    that the storage server has correctly pruned empty directories.
 */
public class RegistrationTest extends StorageTest
{
    /** Test notice. */
    public static final String  notice =
        "checking storage server registration process";

    /** Creates a <code>RegistrationTest</code> object. */
    public RegistrationTest()
    {
        super(new String[][] {new String[] {"subdirectory", "file1"},
                              new String[] {"subdirectory", "file2"},
                              new String[] {"file3"},
                              new String[] {"subdirectory", "subdirectory2",
                                            "file1"},
                              new String[] {"prune", "dir1", "file"},
                              new String[] {"prune", "dir2", "file"}},
              new Path[] {new Path("/subdirectory/file1"),
                          new Path("/file3"),
                          new Path("/prune/dir1/file"),
                          new Path("/prune/dir2/file")});
    }

    /** Runs the core of the test.

        @throws TestFailed If the test fails.
     */
    @Override
    protected void perform() throws TestFailed
    {
        // The registration server has already checked that the storage server
        // has transmitted the correct file list. Check that the storage server
        // has deleted the files it was commanded to delete by checking the
        // contents of the temporary directory.
        Path[]  remaining_files =
            new Path[] {new Path("/subdirectory/file2"),
                        new Path("/subdirectory/subdirectory2/file1")};

        Path[]  listed;

        try
        {
            listed = Path.list(directory.root());
        }
        catch(FileNotFoundException e)
        {
            throw new TestFailed("cannot list storage server root directory",
                                 e);
        }

        if(!TestUtil.sameElements(listed, remaining_files))
        {
            throw new TestFailed("storage server did not remove correct " +
                                 "files after registration");
        }

        // Check that empty directories have been pruned.
        File    pruned = new File(directory.root(), "prune");

        if(pruned.exists())
        {
            throw new TestFailed("storage server did not prune directories " +
                                 "that contain no files");
        }
    }
}
