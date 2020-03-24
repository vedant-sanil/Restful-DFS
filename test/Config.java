package test;

/** Configuration for running the test
 <p>
 This configuration file configures the command to start a naming server
 for naming test and several storage servers for storage test.

 NOTE: MUST have the same port number that follow port.config!!!
 We don't need to specify IP address here because it is all 'localhost' or '127.0.0.1'
 </p>
 */
public class Config {
    private static String separator = System.getProperty("path.separator", ":");

    /**
     * Command to start naming server.
     * TODO: change this string to start your own naming server.
     * Note: You must follow the specification in port.config!
     * After we run this command in the project's root directory, it should start a naming server
     * that listen on 2 ports: 8080 (for SERVICE) and 8090 (for REGISTRATION). 
    */
    public static final String startNaming = String.format("java -cp .%sgson-2.8.6.jar " +
            "naming.NamingServer 8080 8090", separator);

    /**
     * Command to start the first storage server.
     * TODO: change this string to start your own storage server.
     * Note: You must follow the specification in port.config!
     * After we run this command in the project's root directory, it should start a storage server
     * that listen on 2 ports: 7000 (for CLIENT) and 7001 (for COMMAND).
     * It will also register it self through the naming server's REGISTRATION port 8090.
     * This storage server will store all its files under the directory '/tmp/dist-systems-0'
    */
    public static final String startStorage0 = String.format("java -cp .%sgson-2.8.6.jar " +
            "storage.StorageServer 7000 7001 8090 /tmp/dist-systems-0", separator);

    /**
     * Command to start the second storage server.
     * TODO: change this string to start your own storage server.
     * Note: You must follow the specification in port.config!
     * After we run this command in the project's root directory, it should start a storage server
     * that listen on 2 ports: 7010 (for CLIENT) and 7011 (for COMMAND).
     * It will also register it self through the naming server's REGISTRATION port 8090.
     * This storage server will store all its files under the directory '/tmp/dist-systems-1'
    */
    public static final String startStorage1 = String.format("java -cp .%sgson-2.8.6.jar " +
            "storage.StorageServer 7010 7011 8090 /tmp/dist-systems-1", separator);
}
