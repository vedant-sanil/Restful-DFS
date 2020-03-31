package test.naming;

import jsonhelper.ExceptionReturn;
import test.DFSException;
import test.common.Path;
import test.util.TestFailed;

import java.io.IOException;
import java.net.http.HttpResponse;

/** Tests the naming server <code>register</code> method.

    <p>
    The following items are checked:
    <ul>
    <li>The naming server rejects <code>null</code> pointers as arguments.</li>
    <li>Duplicate registrations are rejected.</li>
    <li>The naming server correctly commands the storage server to delete
        duplicate files.</li>
    <li>The naming server correctly commands the storage server to delete files
        that shadow a directory on the naming server.</li>
    <li>Attempts to register the root directory as a file are silently ignored
        by the naming server.</li>
    </ul>
 */
public class RegistrationTest extends NamingTest
{
    /** Test notice. */
    public static final String  notice =
        "checking naming server registration interface";
    /** Prerequisites. */
    public static final Class[] prerequisites =
        new Class[] {PathTest.class};

    /** First storage server to register. */
    private TestStorageServer server1;
    /** Storage server registering duplicate files. */
    private TestStorageServer               server2;
    /** Storage server registering files that shadow a directory. */
    private TestStorageServer               server3;
    /** Storage server attempting to register the root directory. */
    private TestStorageServer               server4;

    /** Storage server used for the <code>null</code> pointer and duplicate
        registration tests. */
    private UnsuccessfulStorageServer       badServer;

    /** Creates the <code>RegistrationTest</code> object. */
    public RegistrationTest() throws IOException
    {
        server1 = new TestStorageServer(this);
        server2 = new TestStorageServer(this);
        server3 = new TestStorageServer(this);
        server4 = new TestStorageServer(this);
        badServer = new UnsuccessfulStorageServer();
    }

    /** Performs the tests.

        @throws TestFailed If any of the tests fail.
     */
    @Override
    protected void perform() throws TestFailed
    {
        synchronized(this)
        {
            badServer.testDuplicateRegistration();
        }

        testMerging();
    }

    /** Performs several registrations that should succeed, and checks that the
        files chosen by the naming server for deletion are correct.

        @throws TestFailed If any of the tests fail.
     */
    private void testMerging() throws TestFailed
    {
        // Register the first storage server with four files.
        Path[]      server1_files =
            new Path[] {new Path("/file"),
                        new Path("/directory/file"),
                        new Path("/directory/another_file"),
                        new Path("/another_directory/file")};

        try
        {
            synchronized(this)
            {
                server1.start(REGISTRATION_PORT, server1_files, null);
            }
        }
        catch(Throwable t)
        {
            throw new TestFailed("unable to register storage server with " +
                                 "naming server", t);
        }

        // Register the second storage server with three files. Two of them were
        // registered by the first server, and are therefore duplicates. The
        // naming server should request that these files be deleted by the
        // second storage server.
        Path[]      server2_files =
            new Path[] {new Path("/file"),
                        new Path("/directory/file"),
                        new Path("/another_directory/another_file")};
        Path[]      server2_delete_files =
            new Path[] {new Path("/file"),
                        new Path("/directory/file")};

        try
        {
            synchronized(this)
            {
                server2.start(REGISTRATION_PORT, server2_files,
                              server2_delete_files);
            }
        }
        catch(TestFailed e)
        {
            throw new TestFailed("naming server did not command deletion " +
                                 "of the expected files when checking " +
                                 "regular files");
        }
        catch(Throwable t)
        {
            throw new TestFailed("unable to register second storage server " +
                                 "with naming server", t);
        }

        // Register the third storage server with a file that shadows a
        // directory on the naming server. The naming server should command this
        // file to be deleted.
        Path[]      server3_files =
            new Path[] {new Path("/directory"),
                        new Path("/another_file")};
        Path[]      server3_delete_files = new Path[] {new Path("/directory")};

        try
        {
            synchronized(this)
            {
                server3.start(REGISTRATION_PORT, server3_files,
                              server3_delete_files);
            }
        }
        catch(TestFailed e)
        {
            throw new TestFailed("naming server did not command deletion " +
                                 "of the expected files when checking a file " +
                                 "that overlaps a directory");
        }
        catch(Throwable t)
        {
            throw new TestFailed("unable to register third storange server " +
                                 "with naming server", t);
        }

        // Register the fourth storage server with the root directory among its
        // list of files. The naming server should silently ignore this attempt.
        Path[]      server4_files = new Path[] {new Path("/")};
        Path[]      server4_delete_files = new Path[0];

        try
        {
            synchronized(this)
            {
                server4.start(REGISTRATION_PORT, server4_files,
                              server4_delete_files);
            }
        }
        catch(Throwable t)
        {
            throw new TestFailed("naming server did not silently ignore " +
                                 "request to add root directory as a file", t);
        }
    }

    /** Stops all servers started by the test. */
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

        if(server3 != null)
        {
            server3.stop();
            server3 = null;
        }

        if(server4 != null)
        {
            server4.stop();
            server4 = null;
        }

        if(badServer != null)
        {
            badServer.stop();
            badServer = null;
        }
    }

    /** Storage server used for the <code>null</code> pointer and duplicate
        registration tests. */
    private class UnsuccessfulStorageServer extends TestStorageServer
    {
        /** Create the storage server. */
        UnsuccessfulStorageServer() throws IOException
        {
            super(RegistrationTest.this);
        }

        /** Tests that the naming server rejects repeat registrations of the
            same storage server.

            <p>
            This method is a replacement for the <code>start</code> method of
            the storage server.

            @throws TestFailed If the naming server accepts the second
                               registration.
         */
        synchronized void testDuplicateRegistration() throws TestFailed
        {
            // Start the storage server skeletons, if they have not already been
            // started.
            try
            {
                startSkeletons();
            }
            catch(Throwable t)
            {
                throw new TestFailed("unable to start skeletons for " +
                                     "duplicate registration test", t);
            }

            HttpResponse<String> response;
            // Register the storage server with the naming server. This
            // registration should succeed.
            try
            {
                response = register(gson, REGISTRATION_PORT, new String[0]);

                String exception_type = gson.fromJson(response.body(), ExceptionReturn.class).exception_type;

                if(exception_type != null)
                {
                    if(DFSException.valueOf(exception_type) == DFSException.IllegalStateException)
                    {
                        throw new IllegalStateException();
                    }
                    else
                    {
                        throw new Throwable(response.body());
                    }
                }

            }
            catch(IllegalStateException e)
            {
                throw new TestFailed("storage server reported as already " +
                                     "registered during initial registration " +
                                     "in duplicate registration test: " +
                                     "perhaps it was mistakenly registered " +
                                     "during the invalid reference test", e);
            }
            catch(Throwable t)
            {
                throw new TestFailed("unable to perform initial registration " +
                                     "of storage server in duplicate " +
                                     "registration test", t);
            }

            // Attempt to register the storage server with the naming server a
            // second time. The test is failed if this registration succeeds.
            try
            {
                response = register(gson, REGISTRATION_PORT, new String[0]);

                String exception_type = gson.fromJson(response.body(), ExceptionReturn.class).exception_type;

                if(exception_type == null)
                {
                    throw new TestFailed("naming server accepted duplicate " +
                            "registration");
                }
                else if(DFSException.valueOf(exception_type) != DFSException.IllegalStateException)
                {
                    throw new Throwable();
                }
            }
            catch(TestFailed e) { throw e; }
            catch(Throwable t)
            {
                throw new TestFailed("register method threw unexpected " +
                                     "exception during duplicate " +
                                     "registration", t);
            }
        }
    }
}
