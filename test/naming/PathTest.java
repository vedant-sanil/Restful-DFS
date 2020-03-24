package test.naming;

import jsonhelper.BooleanReturn;
import jsonhelper.ExceptionReturn;
import jsonhelper.PathRequest;
import test.util.TestFailed;

import java.net.http.HttpResponse;

/** Tests the path library.

 <p>
 Tests include:
 <ul>
 <li>The constructors reject empty paths, components, and component strings
 containing the path separator character.</li>
 </ul>
 */
public class PathTest extends NamingTest
{
    /** Test notice. */
    public static final String  notice =
            "checking path library public interface";

    /** Performs the tests.

     @throws TestFailed If any of the tests fail.
     */
    @Override
    protected void perform() throws TestFailed
    {
        testConstructors();
    }

    /** Tests <code>Path</code> constructors and the <code>toString</code> and
     <code>equals</code> methods.

     @throws TestFailed If any of the tests fail.
     */
    private void testConstructors() throws TestFailed
    {
        boolean    result;
        String     exception_type;
        // Make sure the naming server rejects strings that do not
        // begin with the separator or contain a colon.
        try
        {
            PathRequest request = new PathRequest("");
            HttpResponse<String> response = getResponse("/is_valid_path", SERVICE_PORT, request);
            exception_type = gson.fromJson(response.body(), ExceptionReturn.class).exception_type;
            if(exception_type != null)
            {
                throw new Throwable();
            }

            result = gson.fromJson(response.body(), BooleanReturn.class).success;
            if(result)
            {
                throw new TestFailed("Path(Path, String) constructor accepted " +
                        "empty string");
            }
        }
        catch(TestFailed e) { throw e; }
        catch(Throwable t)
        {
            throw new TestFailed("Path(Path, String) constructor threw " +
                    "unexpected exception when given empty " +
                    "string", t);
        }

        try
        {

            PathRequest request = new PathRequest("some-file");
            HttpResponse<String> response = getResponse("/is_valid_path", SERVICE_PORT, request);
            exception_type = gson.fromJson(response.body(), ExceptionReturn.class).exception_type;
            if(exception_type != null)
            {
                throw new Throwable();
            }

            result = gson.fromJson(response.body(), BooleanReturn.class).success;
            if(result)
            {
                throw new TestFailed("Path(String) constructor accepted string " +
                        "that does not start with separator");
            }
        }
        catch(TestFailed e) { throw e; }
        catch(Throwable t)
        {
            throw new TestFailed("Path(Path, String) constructor threw " +
                    "unexpected exception when given string " +
                    "containing the separator", t);
        }

        try
        {
            PathRequest request = new PathRequest("hostname:path");
            HttpResponse<String> response = getResponse("/is_valid_path", SERVICE_PORT, request);
            exception_type = gson.fromJson(response.body(), ExceptionReturn.class).exception_type;
            if(exception_type != null)
            {
                throw new Throwable();
            }

            result = gson.fromJson(response.body(), BooleanReturn.class).success;
            if(result)
            {
                throw new TestFailed("Path(Path, String) constructor accepted " +
                        "string containing a colon");
            }
        }
        catch(TestFailed e) { throw e; }
        catch(Throwable t)
        {
            throw new TestFailed("Path(Path, String) constructor threw " +
                    "unexpected exception when given string " +
                    "containing a colon", t);
        }
    }
}
