package test;

import test.util.Series;
import test.util.SeriesReport;
import test.util.Test;

public class ConformanceCheckpointTests
{
    /** Runs the tests.

     @param arguments Ignored.
     */
    public static void main(String[] arguments)
    {
        // Create the test list, the series object, and run the test series.
        @SuppressWarnings("unchecked")
        Class<? extends Test>[]     tests =
                new Class[] {
                        test.naming.PathTest.class,
                        test.naming.RegistrationTest.class,
                        test.naming.ListingTest.class,
                        test.naming.CreationTest.class,
                        test.naming.StubRetrievalTest.class,
                        test.storage.RegistrationTest.class,
                        test.storage.AccessTest.class,
                        test.storage.DirectoryTest.class,
                };
        Series series = new Series(tests);
        SeriesReport report = series.run(5, System.out);

        // Print the report and exit with an appropriate exit status.
        report.print(System.out);
        System.exit(report.successful() ? 0 : 2);
    }
}

