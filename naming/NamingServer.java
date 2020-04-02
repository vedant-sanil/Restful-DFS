package naming;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.concurrent.Executors;
import java.util.*;
import jsonhelper.*;
import java.io.*;
import storage.StorageServerInfo;
import java.util.Random;
import com.google.gson.Gson;
import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpExchange;
import test.common.Path;
import test.util.Test;
import test.util.TestFailed;
import test.util.TestUtil;

/** Test naming server.
 <p>
 This naming server performs the following checks each time a storage server
 registers:
 <ul>
 <li>The correct file list has been sent.</li>
 </ul>
 */
public class NamingServer
{
    /** List of paths to expect from the next storage server to register. */
    private Path[] expect_files = null;
    /** List of paths to command the next storage server to register to
     delete. */
    private Path[] delete_files = null;

    /** Naming server IP address. */
    public final static String NAMING_IP = "127.0.0.1";
    public int REGISTRATION_PORT;
    public int SERVICE_PORT;

    /** Naming server registration interface skeleton. */
    private HttpServer registration_skeleton;
    /** Naming server service interface skeleton. */
    private HttpServer service_skeleton;
    /** Last registered storage server client interface. */
    private ServerInfo client_stub = null;
    /** Last registered storage server command interface. */
    private ServerInfo command_stub = null;
    /** Indicates that the skeleton has started. */
    private boolean skeletons_started = false;
    /** Gson object which can parse json to an object. */
    protected Gson gson;

    /** Maintain a list of all registered servers*/
    private ArrayList<StorageServerInfo> regServers;
    private ArrayList<StorageServerInfo> currServers;

    /** Directory of all files */
    private NamingDirectory directree;
    /** Initial root node */
    private DirectoryNode rootdir;

    /**
     Create the naming server
     */
    public NamingServer(int SERVICE_PORT, int REGISTRATION_PORT) throws IOException {
        this.REGISTRATION_PORT = REGISTRATION_PORT;
        this.SERVICE_PORT = SERVICE_PORT;

        this.registration_skeleton = HttpServer.create(new java.net.InetSocketAddress(REGISTRATION_PORT), 0);
        this.registration_skeleton.setExecutor(Executors.newCachedThreadPool());

        this.service_skeleton = HttpServer.create(new java.net.InetSocketAddress(SERVICE_PORT), 0);
        service_skeleton.setExecutor(Executors.newCachedThreadPool());

        this.gson = new Gson();
        this.regServers = new ArrayList<StorageServerInfo>();

        // Add root node to directory
        String[] initList = new String[] {"root"};
        this.directree = new NamingDirectory(initList);
        this.rootdir = directree.getRoot();
        this.currServers = new ArrayList<StorageServerInfo>();
    }

    /** Sets the files the next storage server to connect is expected to
     register.
     @param files The files to expect. The naming server will check that
     these are indeed the files that are received. If this
     argument is <code>null</code>, the naming server will not
     perform the check.
     */
    public void expectFiles(Path[] files)
    {
        expect_files = files;
    }

    /** Sets the files the next storage server to connect will be commanded to
     delete.
     @param files The files to be deleted. If this argument is
     <code>null</code>, the naming server will not command the storage server
     to delete any files.
     */
    public void deleteFiles(Path[] files)
    {
        delete_files = files;
    }

    /** Returns the client interface for the last storage server to register. */
    public ServerInfo clientInterface()
    {
        return client_stub;
    }

    /** Returns the command interface for the last storage server to
     register. */
    public ServerInfo commandInterface()
    {
        return command_stub;
    }

    /** Retrieves a registration stub for the test server.
     @return The stub.
     @throws TestFailed If a stub cannot be obtained.
     */
    ServerInfo stub()
    {
        return new ServerInfo(NAMING_IP, REGISTRATION_PORT);
    }

    /** Starts the test naming server.
     @throws TestFailed If the test cannot be started.
     */
    void start() throws TestFailed
    {
        this.startSkeletons();
    }

    /** Stops the test naming server. */
    void stop()
    {
        this.service_skeleton.stop(0);
        this.registration_skeleton.stop(0);
    }

    private void startSkeletons() {
        // Prevent repeated starting of the skeletons and re-creation of stubs.
        if (this.skeletons_started) return;

        this.add_registration_api();
        this.registration_skeleton.start();
        this.add_service_api();
        this.service_skeleton.start();

        this.skeletons_started = true;
    }

    private void add_registration_api() {
        this.register();
    }

    private void add_service_api() {
        this.isValidPath();
        this.getStorage();
        this.createDirectory();
        this.createFile();
        this.isDirectory();
        this.createFile();
    }

    /** Method to check if directory exists */
    public void isDirectory() {
        this.service_skeleton.createContext("/is_directory", (exchange ->
        {
            System.out.println("Is directory?");
            String jsonString = "";
            int returnCode = 0;

            if ("POST".equals(exchange.getRequestMethod())) {
                PathRequest pathRequest = null;
                try {
                    InputStreamReader isr = new InputStreamReader(exchange.getRequestBody(), "utf-8");
                    pathRequest = gson.fromJson(isr, PathRequest.class);
                    String filepath = pathRequest.path;

                    // Check path validity
                    if (filepath.equals("") || filepath == null || filepath.equals("null")) {
                        throw new IllegalArgumentException("Illegal Argument!");
                    }

                    Path filePath = new Path(filepath);
                    String[] filelist = filePath.toString().substring(1).split("/");
                    System.out.println("--------------------------");
                    System.out.println("File to be checked : " + filePath);

                    if (this.directree.dirExists(this.rootdir, filelist)) {
                        // Check if file or directory exists
                        if (this.directree.isDirectoryStatus()) {
                            System.out.println("Is a directory!");
                            boolean success = true;
                            returnCode = 200;
                            BooleanReturn booleanReturn = new BooleanReturn(success);
                            jsonString = gson.toJson(booleanReturn);
                        }
                        if (this.directree.isFileStatus()) {
                            System.out.println("Is not a directory!");
                            boolean success = false;
                            returnCode = 200;
                            BooleanReturn booleanReturn = new BooleanReturn(success);
                            jsonString = gson.toJson(booleanReturn);
                        }
                    } else {
                        System.out.println("File not found!");
                        throw new FileNotFoundException("File Not Found!");
                    }

                    } catch (IllegalArgumentException e) {
                        returnCode = 404;
                        String exception_type = "IllegalArgumentException";
                        String exception_info = "File/path cannot be found.";
                        ExceptionReturn exceptionReturn = new ExceptionReturn(exception_type, exception_info);
                        this.generateResponseAndClose(exchange, gson.toJson(exceptionReturn), returnCode);
                        return;
                    } catch (FileNotFoundException f) {
                        returnCode = 404;
                        String exception_type = "FileNotFoundException";
                        String exception_info = "File/path cannot be found.";
                        ExceptionReturn exceptionReturn = new ExceptionReturn(exception_type, exception_info);
                        this.generateResponseAndClose(exchange, gson.toJson(exceptionReturn), returnCode);
                        return;
                    }
                } else {
                    jsonString = "The REST method should be POST for <register>!\n";
                    returnCode = 400;
                }
                this.generateResponseAndClose(exchange, jsonString, returnCode);
            }));
    }

    /** Function to create a new directory */
    public void createDirectory() {
        this.service_skeleton.createContext("/create_directory", (exchange ->
        {
            System.out.println("Create directory!");
            String jsonString = "";
            int returnCode = 0;
            boolean success = false;
            String[] parentList;
            if ("POST".equals(exchange.getRequestMethod())) {
                PathRequest pathRequest = null;
                try {
                    InputStreamReader isr = new InputStreamReader(exchange.getRequestBody(), "utf-8");
                    pathRequest = gson.fromJson(isr, PathRequest.class);
                    String filepath = pathRequest.path;
                    BooleanReturn booleanReturn = null;

                    // Check path validity
                    if (filepath.equals("") || filepath == null || filepath.equals("null")) {
                        throw new IllegalArgumentException("Illegal Argument!");
                    }

                    Path filePath = new Path(filepath);
                    String[] filelist = filePath.toString().substring(1).split("/");
                    if (filelist.length == 1) {
                        parentList = Arrays.copyOfRange(filelist,0,filelist.length);
                    } else {
                        parentList = Arrays.copyOfRange(filelist, 0, filelist.length - 1);
                    }
                    System.out.println("--------------------------");
                    System.out.println("File to be checked : " + filePath);

                    if (filepath.equals("/")) {
                        success = false;
                        returnCode = 200;
                        booleanReturn = new BooleanReturn(success);
                        jsonString = gson.toJson(booleanReturn);
                    } else {
                        if (this.directree.dirExists(this.rootdir, parentList)) {
                            System.out.println("Parent Dir exists");
                            // Parent directory exists
                            if (this.directree.dirExists(this.rootdir, filelist)) {
                                // Path provided is a directory
                                System.out.println("Dir exists");
                                success = false;
                            } else if (this.directree.fileExists(this.rootdir, filelist)) {
                                System.out.println("File exists");
                                success = false;
                            } else {
                                System.out.println("WE ARE CREATING A DIRECTORY!");
                                success = true;
                                this.directree.addDirectory(this.rootdir, filelist);
                            }
                            returnCode = 200;
                            booleanReturn = new BooleanReturn(success);
                            jsonString = gson.toJson(booleanReturn);
                        } else {
                            throw new FileNotFoundException("Directory Not Found!");
                        }
                    }
                } catch (IllegalArgumentException e) {
                    returnCode = 404;
                    String exception_type = "IllegalArgumentException";
                    String exception_info = "Directory cannot be found.";
                    ExceptionReturn exceptionReturn = new ExceptionReturn(exception_type, exception_info);
                    this.generateResponseAndClose(exchange, gson.toJson(exceptionReturn), returnCode);
                    return;
                } catch (FileNotFoundException f) {
                    returnCode = 404;
                    String exception_type = "FileNotFoundException";
                    String exception_info = "File/path cannot be found.";
                    ExceptionReturn exceptionReturn = new ExceptionReturn(exception_type, exception_info);
                    this.generateResponseAndClose(exchange, gson.toJson(exceptionReturn), returnCode);
                    return;
                }
            } else {
                jsonString = "The REST method should be POST for <register>!\n";
                returnCode = 400;
            }
            this.generateResponseAndClose(exchange, jsonString, returnCode);
        }));
    }

    /** Function to create a new file */
    public void createFile() {
        this.service_skeleton.createContext("/create_file", (exchange ->
        {
            System.out.println("Create file!");
            String jsonString = "";
            int returnCode = 0;
            boolean success = false;
            if ("POST".equals(exchange.getRequestMethod())) {
                PathRequest pathRequest = null;
                try {
                    InputStreamReader isr = new InputStreamReader(exchange.getRequestBody(), "utf-8");
                    pathRequest = gson.fromJson(isr, PathRequest.class);
                    String filepath = pathRequest.path;
                    BooleanReturn booleanReturn = null;
                    String[] parentList;

                    // Check path validity
                    if (filepath.equals("") || filepath == null || filepath.equals("null")) {
                        throw new IllegalArgumentException("Illegal Argument!");
                    }

                    Path filePath = new Path(filepath);
                    String[] filelist = filePath.toString().substring(1).split("/");
                    if (filelist.length == 1) {
                        parentList = Arrays.copyOfRange(filelist,0,filelist.length);
                    } else {
                        parentList = Arrays.copyOfRange(filelist, 0, filelist.length - 1);
                    }
                    System.out.println("--------------------------");
                    System.out.println("File to be checked : " + filePath);

                    if (filepath.equals("/")) {
                        success = false;
                        returnCode = 200;
                        booleanReturn = new BooleanReturn(success);
                        jsonString = gson.toJson(booleanReturn);
                    } else {
                        if (this.directree.dirExists(this.rootdir, parentList)) {
                            System.out.println("Parent Dir exists");
                            // Parent directory exists
                            if (this.directree.fileExists(this.rootdir, filelist)) {
                                // Path provided is a directory
                                System.out.println("File exists");
                                success = false;
                            } else if (this.directree.dirExists(this.rootdir, filelist)) {
                                System.out.println("Dir exists");
                                success = false;
                            } else {
                                System.out.println("WE ARE CREATING A FILE!");
                                success = true;
                                this.directree.addElement(this.rootdir, filelist);
                            }
                            returnCode = 200;
                            booleanReturn = new BooleanReturn(success);
                            jsonString = gson.toJson(booleanReturn);
                        } else {
                            throw new FileNotFoundException("File Not Found!");
                        }
                    }
                } catch (IllegalArgumentException e) {
                    returnCode = 404;
                    String exception_type = "IllegalArgumentException";
                    String exception_info = "Directory cannot be found.";
                    ExceptionReturn exceptionReturn = new ExceptionReturn(exception_type, exception_info);
                    this.generateResponseAndClose(exchange, gson.toJson(exceptionReturn), returnCode);
                    return;
                } catch (FileNotFoundException f) {
                    returnCode = 404;
                    String exception_type = "FileNotFoundException";
                    String exception_info = "File/path cannot be found.";
                    ExceptionReturn exceptionReturn = new ExceptionReturn(exception_type, exception_info);
                    this.generateResponseAndClose(exchange, gson.toJson(exceptionReturn), returnCode);
                    return;
                }
            } else {
                jsonString = "The REST method should be POST for <register>!\n";
                returnCode = 400;
            }
            this.generateResponseAndClose(exchange, jsonString, returnCode);
        }));
    }

    public void getStorage() {
        this.service_skeleton.createContext("/getstorage", (exchange ->
        {
            System.out.println("Storage check!");
            String jsonString = "";
            int returnCode = 200;
            if ("POST".equals(exchange.getRequestMethod())) {
                PathRequest pathRequest = null;
                try {
                    InputStreamReader isr = new InputStreamReader(exchange.getRequestBody(), "utf-8");
                    pathRequest = gson.fromJson(isr, PathRequest.class);
                    String filepath = pathRequest.path;
                    String server_ip = "";
                    int server_port = 0;

                    if (filepath.equals("") || filepath == null || filepath.equals("null")) {
                        throw new IllegalArgumentException("Illegal Argument!");
                    }

                    Path filePath = new Path(filepath);
                    String[] filelist = filePath.toString().substring(1).split("/");
                    System.out.println("--------------------------");
                    System.out.println("File to be checked : " + filePath);

                    // Check if files exist in directory and generate appropriate response
                    if (this.directree.fileExists(this.rootdir, filelist)) {
                        System.out.println("EXISTS! + " + this.regServers.size());
                        returnCode = 200;

                        // Loop through servers to check which server file exists in
                        serverloop:
                        for (StorageServerInfo s : this.regServers) {
                            for (String filename : s.getFiles()) {
                                System.out.println(s.getClient_port() + " : filename is : " + filename);
                                if (filename.equals(filepath)) {
                                    server_ip = s.getStorage_ip();
                                    server_port = s.getClient_port();
                                    break serverloop;
                                }
                            }
                        }

                        if (server_ip.equals("") || server_port==0) {
                            throw new IllegalArgumentException("Illegal Argument within existing file!");
                        }

                        System.out.println("Server IP: " + server_ip + " server port : " + server_port);
                        ServerInfo serverInfo = new ServerInfo(server_ip, server_port);
                        jsonString = gson.toJson(serverInfo);
                    } else {
                        throw new FileNotFoundException("File Not Found!");
                    }

                } catch (IllegalArgumentException e) {
                    returnCode = 404;
                    String exception_type = "IllegalArgumentException";
                    String exception_info = "File/path cannot be found.";
                    ExceptionReturn exceptionReturn = new ExceptionReturn(exception_type, exception_info);
                    this.generateResponseAndClose(exchange, gson.toJson(exceptionReturn), returnCode);
                    return;
                } catch (FileNotFoundException f) {
                    returnCode = 404;
                    String exception_type = "FileNotFoundException";
                    String exception_info = "File/path cannot be found.";
                    ExceptionReturn exceptionReturn = new ExceptionReturn(exception_type, exception_info);
                    this.generateResponseAndClose(exchange, gson.toJson(exceptionReturn), returnCode);
                    return;
                }
            } else {
                jsonString = "The REST method should be POST for <register>!\n";
                returnCode = 400;
            }
            this.generateResponseAndClose(exchange, jsonString, returnCode);
        }));
    }

    public void isValidPath()
    {
        this.service_skeleton.createContext("/is_valid_path", (exchange ->
        {
            HashMap<String, Object> respText = new HashMap<String, Object>();
            String jsonString = "";
            int returnCode = 200;
            if ("POST".equals(exchange.getRequestMethod())) {
                RegisterRequest registerRequest = null;
                try {
                    InputStreamReader isr = new InputStreamReader(exchange.getRequestBody(), "utf-8");
                    Map<String, String> map = new HashMap<String, String>();
                    map = (Map<String, String>) gson.fromJson(isr, map.getClass());
                    System.out.println(map);
                    String filepath = map.get("path");
                    System.out.println(filepath + " - This is the filepath");
                    if (filepath.equals("") || !filepath.startsWith("/") || !filepath.contains(":")) {
                        System.out.println("Illegal Argument");
                        returnCode = 200;
                        respText.put("success", "false");
                        jsonString = gson.toJson(respText);
                        System.out.println("---");
                        System.out.println(jsonString);
                        this.generateResponseAndClose(exchange, jsonString, returnCode);
                        return;
                    } else {
                        respText.put("success", "true");
                        jsonString = gson.toJson(respText);
                        System.out.println("---");
                        System.out.println(jsonString);
                        this.generateResponseAndClose(exchange, jsonString, returnCode);
                        return;
                    }
                } catch (Exception e) {
                    jsonString = "Error during parse JSON object!\n";
                    returnCode = 400;
                    this.generateResponseAndClose(exchange, jsonString, returnCode);
                    return;
                }
            } else {
                jsonString = "The REST method should be POST for <register>!\n";
                returnCode = 400;
            }
            this.generateResponseAndClose(exchange, jsonString, returnCode);
        }));
    }

    public static void main(String[] args) throws FileNotFoundException, IOException, TestFailed
    {
        Random rand = new Random();
        File file = new File("./NamingServer" + rand.nextInt() + ".output");
        PrintStream stream = new PrintStream(file);
        System.setOut(stream);
        System.setErr(stream);
        System.out.println("MAIN METHOD CALLED");
        NamingServer n = new NamingServer(Integer.parseInt(args[0]), Integer.parseInt(args[1]));
        n.start();
    }


    private void register() {
        this.registration_skeleton.createContext("/register", (exchange -> {
            //HashMap<String, Object> respText = new HashMap<String, Object>();
            String jsonString = "";
            int returnCode = 200;
            int command_port = 0;
            int client_port = 0;
            if ("POST".equals(exchange.getRequestMethod())) {
                RegisterRequest registerRequest = null;
                try {
                    InputStreamReader isr = new InputStreamReader(exchange.getRequestBody(), "utf-8");
                    registerRequest = gson.fromJson(isr, RegisterRequest.class);
                } catch (Exception e) {
                    jsonString = "Error during parse JSON object!\n";
                    returnCode = 400;
                    this.generateResponseAndClose(exchange, jsonString, returnCode);
                    return;
                }
                try {
                    String storage_ip = registerRequest.storage_ip;
                    client_port = registerRequest.client_port;
                    command_port = registerRequest.command_port;
                    String[] files = registerRequest.files;

                    System.out.println(command_port + " : " + registerRequest + " " + this.currServers.size());

                    /** Raise exception here if duplicate servers exist */
                    if (this.regServers.size() == 0) {
                        this.currServers.add(new StorageServerInfo(storage_ip, client_port, command_port, files));
                        //System.out.println(command_port + " : " + registerRequest + " " + this.currServers.size());
                    } else {
                        for (int i=0; i < this.regServers.size(); i++) {
                            if (this.regServers.get(i).verifySameServer(storage_ip, command_port)) {
                                // Server already has been registered, throw exception
                                returnCode = 409;
                                throw new java.lang.IllegalStateException("Illegal State");
                            } else {
                                this.currServers.add(new StorageServerInfo(storage_ip, client_port, command_port, files));
                                break;
                            }
                        }
                    }
                    this.regServers = this.currServers;
                } catch (Exception e) {
                    System.out.println("Exception thrown (" + command_port + ") : " + e);
                    returnCode = 409;
                    String exception_type = "IllegalStateException";
                    String exception_info = "This storage client already registered.";
                    ExceptionReturn exceptionReturn = new ExceptionReturn(exception_type, exception_info);
                    this.generateResponseAndClose(exchange, gson.toJson(exceptionReturn), returnCode);
                    return;
                }

                /** Loop over list of files obtained from server */
                List<String> repeated_list = new ArrayList<String>();
                for (String filename : registerRequest.files) {
                    Path filePath = new Path(filename);
                    String[] filelist = filePath.toString().substring(1).split("/");
                    System.out.println("--------------------------");
                    System.out.println("Files being sent in : " + filePath);

                    // Add new files to tree directory
                    this.directree.addElement(this.rootdir, filelist);
                    if (!this.directree.isUnique()) {
                        repeated_list.add(filename);
                        //System.out.println("The repeated file dir is : " + filename);
                    }

                    System.out.println("--------------------------");
                }

                //String[] replLists = repeated_list.toArray()<String>;
                String[] replLists = new String[repeated_list.size()];
                replLists = repeated_list.toArray(replLists);

                FilesReturn filesReturn = new FilesReturn(replLists);
                jsonString = gson.toJson(filesReturn);
                returnCode = 200;
                //System.out.println(respText);
            } else {
                jsonString = "The REST method should be POST for <register>!\n";
                returnCode = 400;
            }
            this.generateResponseAndClose(exchange, jsonString, returnCode);
        }));
    }

    /**
     * call this function when you want to write to response and close the connection.
     */
    private void generateResponseAndClose(HttpExchange exchange, String respText, int returnCode) throws IOException {
        exchange.sendResponseHeaders(returnCode, respText.getBytes().length);
        OutputStream output = exchange.getResponseBody();
        output.write(respText.getBytes());
        output.flush();
        exchange.close();
    }
}