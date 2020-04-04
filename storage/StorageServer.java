package storage;
import java.io.IOException;
import java.util.*;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.Executors;
import java.io.*;
import java.io.File;
import java.util.Random;
import jsonhelper.*;
import com.google.gson.Gson;
import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpExchange;
import test.common.Path;
import test.util.Test;
import test.util.TestFailed;
import test.util.TestUtil;
import java.io.FileNotFoundException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import static java.lang.Math.toIntExact;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.nio.ByteBuffer;
import java.nio.file.Files;


/** Base class of storage servers.
 */
public class StorageServer
{
    /** Storage IP address. */
    public final static String      STORAGE_IP = "127.0.0.1";
    public int     CLIENT_PORT;
    public int    COMMAND_PORT;
    public int     NAMING_PORT;
    /** Client interface skeleton. */
    protected HttpServer            client_skeleton;
    /** Command interface skeleton. */
    protected HttpServer            command_skeleton;
    /** Indicates that the skeletons have been started. This is used to prevent
     multiple starts of the skeletons by alternative implementations of the
     <code>start</code> method. */
    private boolean                 skeletons_started;
    /** Gson object which can parse json to an object. */
    protected Gson                  gson;
    /** Local Directory to locate files to serve */
    public static String root_dir;

    public StorageServer(int CLIENT_PORT, int COMMAND_PORT, int NAMING_PORT, String root_dir) throws IOException
    {
        this.CLIENT_PORT = CLIENT_PORT;
        this.COMMAND_PORT = COMMAND_PORT;
        this.NAMING_PORT = NAMING_PORT;
        this.root_dir = root_dir;
        this.client_skeleton = HttpServer.create(new InetSocketAddress(CLIENT_PORT), 0);
        this.client_skeleton.setExecutor(Executors.newCachedThreadPool());

        this.command_skeleton = HttpServer.create(new InetSocketAddress(COMMAND_PORT), 0);
        this.command_skeleton.setExecutor(Executors.newCachedThreadPool());
        skeletons_started = false;
        gson = new Gson();
    }

    /** For starting the respective skeletons */
    protected synchronized void startSkeletons() throws TestFailed
    {
        // Prevent repeated starting of the skeletons and re-creation of stubs.
        if(skeletons_started)
            return;

        // Start the client interface skeleton and create the stub.
        client_skeleton.start();

        // Start the registration skeleton and create the stub.
        command_skeleton.start();

        skeletons_started = true;

        // Register all API to two skeletons
        this.add_client_api();
        this.add_command_api();
    }


    /** File listing */
    public List<File> listFilesForFolder(final File folder, List<File> files) {
        for (final File fileEntry : folder.listFiles()) {
            if (fileEntry.isDirectory()) {
                files = listFilesForFolder(fileEntry, files);
            } else {
                files.add(fileEntry);
            }
        }
        return files;
    }

    /** Register to a naming server */
    public HttpResponse<String> register(Gson gson, int naming_register_port, String[] files) throws TestFailed
    {
        RegisterRequest registerRequest = new RegisterRequest(STORAGE_IP, CLIENT_PORT, COMMAND_PORT, files);
        HttpClient client = HttpClient.newHttpClient();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://127.0.0.1:" + naming_register_port + "/register"))
                .setHeader("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(gson.toJson(registerRequest)))
                .build();

        HttpResponse<String> response;

        try
        {
            response = client.send(request, HttpResponse.BodyHandlers.ofString());
        }
        catch (Throwable t)
        {
            throw new TestFailed("unable to send request to naming server", t);
        }
        return response;
    }

    /** Starts the test storage server. */
    public synchronized ServerInfo start(int naming_register_port) throws TestFailed
    {
        // Start storage server skeletons.
        startSkeletons();
        File f = new File(root_dir);
        File[] files;
        List<File> fi = new ArrayList<>();
        fi = listFilesForFolder(f, fi);
        files = fi.toArray(new File[fi.size()]);
        String[] files_string = new String[files.length];
        String[] delete_files_string = new String[files.length];


        // Make sure the root directory is removed from each file path
        for(int i = 0; i < files.length; i++)
        {
            files_string[i] = files[i].toString().replace(root_dir, "");
        }

        // Register the storage server with the naming server.
        HttpResponse<String> response = register(gson, this.NAMING_PORT, files_string);
        delete_files_string = gson.fromJson(response.body(), FilesReturn.class).files;
        File[] delete_files = new File[delete_files_string.length];

        // Looping across the returned files from Naming Server in order to remove the duplicates.
        for(int i = 0; i < delete_files_string.length; i++)
        {
            delete_files[i] = new File(root_dir + delete_files_string[i]);
            boolean a = delete_files[i].delete();
            String dir = delete_files[i].getParent();
            File dirfile = new File(dir);
            // Whenever the number of files in the directory become 0, we have to make sure that the directory is also
            // deleted
            while (dirfile.list().length == 0)
            {
                a = dirfile.delete();
                String d = dirfile.getParent();
                dirfile = new File(d);
            }
        }
        return new ServerInfo(STORAGE_IP, client_skeleton.getAddress().getPort());
    }

    /** Stops the storage server. */
    public void stop() throws TestFailed
    {
        command_skeleton.stop(0);
        client_skeleton.stop(0);
    }

    /** Add APIs supported by client skeleton. */
    private void add_client_api() throws TestFailed
    {
        this.size();
        this.read();
        this.write();
    }

    /** Add APIs supported by command skeleton. */
    private void add_command_api() throws TestFailed
    {
        this.create();
        this.delete();
        this.copy();
    }

    public static void main(String[] args) throws FileNotFoundException, IOException, TestFailed
    {
        // Code to redirect the Output to files.
        Random rand = new Random();
        File file = new File("./StorageServer" + rand.nextInt() + ".output");
        PrintStream stream = new PrintStream(file);
        System.setOut(stream);
        System.setErr(stream);

        // New storage server is created.
        StorageServer s = new StorageServer(Integer.parseInt(args[0]), Integer.parseInt(args[1]), Integer.parseInt(args[2]), args[3]);
        s.start(Integer.parseInt(args[2]));
    }

    /** Gets the size of the file stored on the storage server. */
    public void size() throws TestFailed
    {
        this.client_skeleton.createContext("/storage_size", (exchange ->
        {
            HashMap<String, String> respText = new HashMap<String, String>();
            String jsonString = "";
            int returnCode = 200;
            if ("POST".equals(exchange.getRequestMethod())) {
                // parse request json
                try
                {
                    InputStreamReader isr = new InputStreamReader(exchange.getRequestBody(), "utf-8");
                    Map<String, String> map = new HashMap<String, String>();
                    map = (Map<String, String>) gson.fromJson(isr, map.getClass());
                    String filepath = map.get("path");

                    // If the filepath is null, we need to return IllegalArgumentException
                    if (filepath.equals("")) {
                        returnCode = 404;
                        respText.put("exception_type", "IllegalArgumentException");
                        respText.put("exception_info", "IllegalArgumentException: File/path invalid.");
                        jsonString = gson.toJson(respText);
                        this.generateResponseAndClose(exchange, jsonString, returnCode);
                        return;
                    }
                    try {
                        String path = root_dir + filepath;
                        File f = new File(path);

                        // Code to verify if the path is a file and exists. If not then
                        // API tells us to return FileNotFoundException.
                        if (!f.exists() || f.isDirectory()) {
                            returnCode = 404;
                            respText.put("exception_type", "FileNotFoundException");
                            respText.put("exception_info", "FileNotFoundException: File/path not found.");
                            jsonString = gson.toJson(respText);
                            this.generateResponseAndClose(exchange, jsonString, returnCode);
                            return;
                        }

                        // Returning a 200 OK, if none of the exceptions are raised along with the
                        // size for the path passed.
                        long length = f.length();
                        respText.put("size", String.valueOf(length));
                        returnCode = 200;
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                } catch (Exception e) {
                    returnCode = 404;
                    respText.put("exception_type", "IllegalArgumentException");
                    respText.put("exception_info", "IllegalArgumentException: File/path invalid.");
                    jsonString = gson.toJson(respText);
                    this.generateResponseAndClose(exchange, jsonString, returnCode);
                    return;
                }
                jsonString = gson.toJson(respText);
            }
            else {
                returnCode = 404;
            }
            this.generateResponseAndClose(exchange, jsonString, returnCode);
        }));
    }

    /** Responsible for reading the data from a file placed on a storage server */
    public void read()
    {
        this.client_skeleton.createContext("/storage_read", (exchange ->
        {
            System.out.flush();
            HashMap<String, String> respText = new HashMap<String, String>();
            String jsonString = "";
            int returnCode = 200;
            if ("POST".equals(exchange.getRequestMethod())) {
                // parse request json
                try
                {
                    // Reading the HTTP Exchange into a hashmap from which individual values can
                    // be extracted.
                    InputStreamReader isr = new InputStreamReader(exchange.getRequestBody(), "utf-8");
                    Map<String, Object> map = new HashMap<String, Object>();
                    map = (Map<String, Object>) gson.fromJson(isr, map.getClass());
                    String filepath = (String) map.get("path");
                    Double offset = (Double) map.get("offset");
                    Double length = (Double) map.get("length");

                    // If the filepath is null, the API states that we must pass IllegalArgumentException
                    if (filepath.equals("") || filepath == null || filepath.equals("null")) {
                        returnCode = 404;
                        respText.put("exception_type", "IllegalArgumentException");
                        respText.put("exception_info", "IllegalArgumentException: File/path invalid.");
                        jsonString = gson.toJson(respText);
                        this.generateResponseAndClose(exchange, jsonString, returnCode);
                        return;
                    }

                    // Negative values for offset and length must raise IndexOutOfBoundsException.
                    if (offset < 0 || length < 0) {
                        returnCode = 404;
                        respText.put("exception_type", "IndexOutOfBoundsException");
                        respText.put("exception_info", "IndexOutOfBoundsException: Offset/Length Index is negative/out of bounds.");
                        jsonString = gson.toJson(respText);
                        this.generateResponseAndClose(exchange, jsonString, returnCode);
                        return;
                    }

                    // Code to verify if the path is a file and exists. If not then
                    // API tells us to return FileNotFoundException.
                    String path = root_dir + filepath;
                    File f = new File(path);
                    if (!f.exists() || f.isDirectory()) {
                        returnCode = 404;
                        respText.put("exception_type", "FileNotFoundException");
                        respText.put("exception_info", "FileNotFoundException: File/path not found.");
                        jsonString = gson.toJson(respText);
                        this.generateResponseAndClose(exchange, jsonString, returnCode);
                        return;
                    }

                    // We dont want to outshoot the total length of the file when we go from
                    // offset till the amount that has been asked to be read. So in this case,
                    // We are checking if the values actually are within bounds. Else IndexOutOfBoundsException.
                    FileInputStream fstream = new FileInputStream(f);
                    if ((offset.intValue() + length.intValue()) <= fstream.available())
                    {
                        byte[] bytes = new byte[length.intValue()];
                        fstream.read(bytes, offset.intValue(), length.intValue());
                        String str = Base64.getEncoder().encodeToString(bytes);
                        respText.put("data", str);
                        fstream.close();
                    } else {
                        returnCode = 404;
                        respText.put("exception_type", "IndexOutOfBoundsException");
                        respText.put("exception_info", "IndexOutOfBoundsException: Offset/Length Index is negative/out of bounds.");
                        fstream.close();
                        jsonString = gson.toJson(respText);
                        this.generateResponseAndClose(exchange, jsonString, returnCode);
                        return;
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                    returnCode = 404;
                    respText.put("exception_type", "IllegalArgumentException");
                    respText.put("exception_info", "IllegalArgumentException: File/path invalid.");
                    jsonString = gson.toJson(respText);
                    this.generateResponseAndClose(exchange, jsonString, returnCode);
                    return;
                }

                // Comes here when no other exception is caught. Which is why ReturnCode = 200.
                returnCode = 200;
                jsonString = gson.toJson(respText);
            } else {
                returnCode = 404;
            }
            this.generateResponseAndClose(exchange, jsonString, returnCode);
        }));
    }

    /** Responsible for writing into a file on the storage server */
    public void write()
    {
        this.client_skeleton.createContext("/storage_write", (exchange ->
        {
            HashMap<String, String> respText = new HashMap<String, String>();
            String jsonString = "";
            int returnCode = 200;
            if ("POST".equals(exchange.getRequestMethod())) {
                // parse request json
                try
                {
                    // Reading the HTTP Exchange into a hashmap from which individual values can
                    // be extracted.
                    InputStreamReader isr = new InputStreamReader(exchange.getRequestBody(), "utf-8");
                    Map<String, Object> map = new HashMap<String, Object>();
                    map = (Map<String, Object>) gson.fromJson(isr, map.getClass());
                    String filepath = (String) map.get("path");
                    Double offset = (Double) map.get("offset");
                    String data = (String) map.get("data");

                    // NullPointerException check with the data field in the hashmap.
                    if (data == null) {
                        returnCode = 404;
                        respText.put("exception_type", "NullPointerException");
                        respText.put("exception_info", "NullPointerException: Data invalid.");
                        jsonString = gson.toJson(respText);
                        this.generateResponseAndClose(exchange, jsonString, returnCode);
                        return;
                    }

                    // If the filepath is null, the API states that we must pass IllegalArgumentException
                    if (filepath.equals("") || filepath == null || filepath.equals("null")) {
                        returnCode = 404;
                        respText.put("exception_type", "IllegalArgumentException");
                        respText.put("exception_info", "IllegalArgumentException: File/path invalid.");
                        jsonString = gson.toJson(respText);
                        this.generateResponseAndClose(exchange, jsonString, returnCode);
                        return;
                    }

                    // Negative values for offset must raise IndexOutOfBoundsException.
                    if (offset.intValue() < 0) {
                        returnCode = 404;
                        respText.put("exception_type", "IndexOutOfBoundsException");
                        respText.put("exception_info", "IndexOutOfBoundsException: Offset/Length Index is negative/out of bounds.");
                        jsonString = gson.toJson(respText);
                        this.generateResponseAndClose(exchange, jsonString, returnCode);
                        return;
                    }

                    // Code to verify if the path is a file and exists. If not then
                    // API tells us to return FileNotFoundException.
                    String path = root_dir + filepath;
                    File f = new File(path);
                    if (!f.exists() || f.isDirectory()) {
                        returnCode = 404;
                        respText.put("exception_type", "FileNotFoundException");
                        respText.put("exception_info", "FileNotFoundException: File/path not found.");
                        jsonString = gson.toJson(respText);
                        this.generateResponseAndClose(exchange, jsonString, returnCode);
                        return;
                    }
                    FileOutputStream ostream = null;
                    ostream = new FileOutputStream(f, true);

                    // Moving to the position of the offset before writing.
                    RandomAccessFile aFile = new RandomAccessFile(path, "rw");
                    FileChannel fchannel = aFile.getChannel();
                    ByteBuffer bb = ByteBuffer.wrap(Base64.getDecoder().decode(data));
                    fchannel.position(offset.intValue());
                    fchannel.write(bb);
                    respText.put("success", "true");
                    ostream.close();
                    fchannel.close();
                } catch (Exception e) {
                    e.printStackTrace();
                    returnCode = 404;
                    respText.put("exception_type", "IllegalArgumentException");
                    respText.put("exception_info", "IllegalArgumentException: File/path invalid.");
                }

                // Comes here if no other exception is raised.
                returnCode = 200;
                jsonString = gson.toJson(respText);
            } else {
                returnCode = 404;
            }
            this.generateResponseAndClose(exchange, jsonString, returnCode);
        }));
    }

    /** Creates a File/Directory on the path that is given. */
    public void create()
    {
        this.command_skeleton.createContext("/storage_create", (exchange ->
        {
            HashMap<String, String> respText = new HashMap<String, String>();
            String jsonString = "";
            int returnCode = 200;
            if ("POST".equals(exchange.getRequestMethod())) {
                // parse request json
                try
                {
                    // Reading the HTTP Exchange into a hashmap from which individual values can
                    // be extracted.
                    InputStreamReader isr = new InputStreamReader(exchange.getRequestBody(), "utf-8");
                    Map<String, Object> map = new HashMap<String, Object>();
                    map = (Map<String, Object>) gson.fromJson(isr, map.getClass());
                    String filepath = (String) map.get("path");

                    // If the filepath is null, the API states that we must pass IllegalArgumentException
                    if (filepath.equals("") || filepath == null || filepath.equals("null")) {
                        returnCode = 404;
                        respText.put("exception_type", "IllegalArgumentException");
                        respText.put("exception_info", "IllegalArgumentException: File/path invalid.");
                        jsonString = gson.toJson(respText);
                        this.generateResponseAndClose(exchange, jsonString, returnCode);
                        return;
                    }
                    String path = root_dir + filepath;
                    File temp = new File(path);
                    String dir = temp.getParent();
                    String fil = temp.getName();
                    boolean flagdir = new File(dir).mkdirs();
                    boolean flag = new File(path).createNewFile();

                    // File creation will lead to a True value of the flag.
                    if (flag)
                        respText.put("success", "true");
                    else
                        respText.put("success", "false");
                } catch (Exception e) {
                    e.printStackTrace();
                    returnCode = 404;
                    respText.put("exception_type", "IllegalArgumentException");
                    respText.put("exception_info", "IllegalArgumentException: File/path invalid.");
                    jsonString = gson.toJson(respText);
                    this.generateResponseAndClose(exchange, jsonString, returnCode);
                    return;
                }

                // Comes here if no other exception.
                returnCode = 200;
                jsonString = gson.toJson(respText);
            } else {
                returnCode = 404;
            }
            this.generateResponseAndClose(exchange, jsonString, returnCode);
        }));
    }


    /** Function to remove the files in the directory and then the directory */
    public boolean deleteDir(File file) {
        File[] contents = file.listFiles();
        if (contents != null) {
            for (File f : contents) {
                if (! Files.isSymbolicLink(f.toPath())) {
                    deleteDir(f);
                }
            }
        }
        return file.delete();
    }

    /** To carry out the deletion of files on the Storage Server */
    public void delete()
    {
        this.command_skeleton.createContext("/storage_delete", (exchange ->
        {
            HashMap<String, String> respText = new HashMap<String, String>();
            String jsonString = "";
            int returnCode = 200;
            if ("POST".equals(exchange.getRequestMethod())) {
                // parse request json
                try
                {
                    // Reading the HTTP Exchange into a hashmap from which individual values can
                    // be extracted.
                    InputStreamReader isr = new InputStreamReader(exchange.getRequestBody(), "utf-8");
                    Map<String, Object> map = new HashMap<String, Object>();
                    map = (Map<String, Object>) gson.fromJson(isr, map.getClass());
                    String filepath = (String) map.get("path");

                    // If the filepath is null, the API states that we must pass IllegalArgumentException
                    if (filepath.equals("") || filepath == null || filepath.equals("null") || filepath.equals(root_dir) || filepath.equals("/")) {
                        returnCode = 404;
                        respText.put("exception_type", "IllegalArgumentException");
                        respText.put("exception_info", "IllegalArgumentException: File/path invalid.");
                        jsonString = gson.toJson(respText);
                        this.generateResponseAndClose(exchange, jsonString, returnCode);
                        return;
                    }

                    // File to delete is taken and if it is a directory, you first delete all the files in the
                    // directory before deleting the directory.
                    File delete_files = new File(root_dir + filepath);
                    boolean flag;
                    if (delete_files.isDirectory())
                    {
                        flag = deleteDir(delete_files);
                    } else {
                        flag = delete_files.delete();
                        String dir = delete_files.getParent();
                        File dirfile = new File(dir);
                        while (dirfile.list().length == 0)
                        {
                            flag = dirfile.delete();
                            String d = dirfile.getParent();
                            dirfile = new File(d);
                        }
                    }

                    // If the delete function returns true, then the success is true.
                    if (flag)
                        respText.put("success", "true");
                    else
                        respText.put("success", "false");
                } catch (Exception e) {
                    e.printStackTrace();
                    returnCode = 404;
                    respText.put("exception_type", "IllegalArgumentException");
                    respText.put("exception_info", "IllegalArgumentException: File/path invalid.");
                    jsonString = gson.toJson(respText);
                    this.generateResponseAndClose(exchange, jsonString, returnCode);
                    return;
                }

                // Comes here if no other exception.
                returnCode = 200;
                jsonString = gson.toJson(respText);
            } else {
                returnCode = 404;
            }
            this.generateResponseAndClose(exchange, jsonString, returnCode);
        }));
    }

    /** Function to carry out the replication amongst multiple servers */
    public void copy()
    {
        this.command_skeleton.createContext("/storage_copy", (exchange ->
        {
            HashMap<String, String> respText = new HashMap<String, String>();
            String jsonString = "";
            int returnCode = 200;
            if ("POST".equals(exchange.getRequestMethod())) {
                // parse request json
                try
                {
                    // Reading the HTTP Exchange into a hashmap from which individual values can
                    // be extracted.
                    InputStreamReader isr = new InputStreamReader(exchange.getRequestBody(), "utf-8");
                    Map<String, Object> map = new HashMap<String, Object>();
                    map = (Map<String, Object>) gson.fromJson(isr, map.getClass());
                    String filepath = (String) map.get("path");
                    String serverIp = (String) map.get("server_ip");

                    // Replacing localhost with the value of the IP address
                    if (serverIp.equals("localhost"))
                    {
                        serverIp = "127.0.0.1";
                    }
                    Double serverPort = (Double) map.get("server_port");

                    // Reading the HTTP Exchange into a hashmap from which individual values can
                    // be extracted.
                    if (filepath.equals("") || filepath == null || filepath.equals("null") || filepath.equals(root_dir) || filepath.equals("/")) {
                        returnCode = 404;
                        respText.put("exception_type", "IllegalArgumentException");
                        respText.put("exception_info", "IllegalArgumentException: File/path invalid.");
                        jsonString = gson.toJson(respText);
                        this.generateResponseAndClose(exchange, jsonString, returnCode);
                        return;
                    }

                    // With the given path, first find the number of characters you need to read. That is nothing
                    // but the size of the file which needs to be read. This can be got using the Storage_API.
                    Map<String, Object> sendText = new HashMap<String, Object>();
                    sendText.put("path", filepath);
                    HttpClient client = HttpClient.newHttpClient();

                    HttpRequest request = HttpRequest.newBuilder()
                            .uri(URI.create("http://"+serverIp+":" + serverPort.intValue() + "/storage_size"))
                            .setHeader("Content-Type", "application/json")
                            .POST(HttpRequest.BodyPublishers.ofString(gson.toJson(sendText)))
                            .build();
                    HttpResponse<String> response;
                    try
                    {
                        response = client.send(request, HttpResponse.BodyHandlers.ofString());
                    }
                    catch (Throwable t)
                    {
                        throw new TestFailed("unable to send request to naming server", t);
                    }
                    if (response.statusCode() != 200)
                    {
                        returnCode = 404;
                        this.generateResponseAndClose(exchange, response.body(), returnCode);
                        return;
                    } else {

                        // If the values don't return any exception, then we can go ahead and read the data from the
                        // file using the storage_read API. Here we want to read the entire file which is why we keep
                        // the offset as 0.0 and the length = size of the file.
                        sendText.put("offset", 0.0);
                        Map<String, String> temp = new HashMap<String,String>();
                        temp = (Map<String, String>) gson.fromJson(response.body(), temp.getClass());
                        sendText.put("length", new Double(temp.get("size")));
                        HttpRequest request2 = HttpRequest.newBuilder()
                                .uri(URI.create("http://"+serverIp+":" + serverPort.intValue() + "/storage_read"))
                                .setHeader("Content-Type", "application/json")
                                .POST(HttpRequest.BodyPublishers.ofString(gson.toJson(sendText)))
                                .build();
                        HttpResponse<String> response2;
                        try
                        {
                            response2 = client.send(request2, HttpResponse.BodyHandlers.ofString());
                        }
                        catch (Throwable t)
                        {
                            throw new TestFailed("unable to send request to naming server", t);
                        }

                        // With the response from reading having the data which needs to be written to the file,
                        // we go ahead and write using the FileOutputStream object. We cannot use the getStorage API
                        // because we need to write locally. So we create a new file (and a new directory, if the
                        // directory is not present) and then go ahead and write.
                        Map<String, Object> tempread = new HashMap<String,Object>();
                        tempread = (Map<String, Object>) gson.fromJson(response2.body(), tempread.getClass());
                        byte[] byte_data_write = Base64.getDecoder().decode((String) tempread.get("data"));
                        String decodedString = new String(byte_data_write);
                        File newFile = new File(root_dir+filepath);
                        String newDirectory = newFile.getParent();
                        if (!new File(newDirectory).exists())
                        {
                            new File(newDirectory).mkdirs();
                        }
                        if (!newFile.exists()) {
                            newFile.createNewFile();
                        }
                        try (FileOutputStream outputStream = new FileOutputStream(newFile))
                        {
                            outputStream.write(byte_data_write);
                            outputStream.flush();
                            outputStream.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }

                        respText.put("success", "true");

                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    returnCode = 404;
                    respText.put("exception_type", "IOException");
                    respText.put("exception_info", "IOException: File/path IO not valid.");
                    jsonString = gson.toJson(respText);
                    this.generateResponseAndClose(exchange, jsonString, returnCode);
                    return;
                }
                returnCode = 200;
                jsonString = gson.toJson(respText);
            } else {
                returnCode = 404;
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