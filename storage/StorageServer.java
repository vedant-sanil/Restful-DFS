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
        System.out.println(CLIENT_PORT);
        System.out.println(COMMAND_PORT);
        System.out.println(NAMING_PORT);
        System.out.println(root_dir);
        System.out.println("--------------------");
    }

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
//        int storagePort = this.client_skeleton.getAddress().getPort();
//        int commandPort = this.command_skeleton.getAddress().getPort();

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


        for(int i = 0; i < files.length; i++)
        {
            files_string[i] = files[i].toString().replace(root_dir, "");
            System.out.println(files_string[i]);
        }

        // Register the storage server with the naming server.
        HttpResponse<String> response = register(gson, this.NAMING_PORT, files_string);
        delete_files_string = gson.fromJson(response.body(), FilesReturn.class).files;
        File[] delete_files = new File[delete_files_string.length];

//        System.out.println(root_dir);
        System.out.println("DELETED FILES");
        for(int i = 0; i < delete_files_string.length; i++)
        {
            System.out.println("-----");
            System.out.println(delete_files_string[i]);
            System.out.println("-----");
            delete_files[i] = new File(root_dir + delete_files_string[i]);
            System.out.println(delete_files[i]);
            boolean a = delete_files[i].delete();
            System.out.println(a);
            String dir = delete_files[i].getParent();
            File dirfile = new File(dir);
            while (dirfile.list().length == 0)
            {
                System.out.println(dirfile);
                a = dirfile.delete();
                System.out.println(a);
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
        Random rand = new Random();
        File file = new File("./StorageServer" + rand.nextInt() + ".output");
        PrintStream stream = new PrintStream(file);
        System.setOut(stream);
        System.setErr(stream);
        System.out.println("MAIN METHOD CALLED");
        StorageServer s = new StorageServer(Integer.parseInt(args[0]), Integer.parseInt(args[1]), Integer.parseInt(args[2]), args[3]);
        s.start(Integer.parseInt(args[2]));
    }

    /** Throws <code>UnsupportedOperationException</code>. */
    public void size() throws TestFailed
    {
        this.client_skeleton.createContext("/storage_size", (exchange ->
        {
            System.out.println("Coming into Storage Context");
            HashMap<String, String> respText = new HashMap<String, String>();
            String jsonString = "";
            int returnCode = 200;
            if ("POST".equals(exchange.getRequestMethod())) {
                // parse request json
                try
                {
                    System.out.println("Coming into POST portion");
                    InputStreamReader isr = new InputStreamReader(exchange.getRequestBody(), "utf-8");
                    Map<String, String> map = new HashMap<String, String>();
                    map = (Map<String, String>) gson.fromJson(isr, map.getClass());
                    System.out.println(map);
                    String filepath = map.get("path");
                    System.out.println((root_dir+filepath) + " - This is the filepath");
                    if (filepath.equals("")) {
                        System.out.println("Illegal Argument");
                        returnCode = 404;
                        respText.put("exception_type", "IllegalArgumentException");
                        respText.put("exception_info", "IllegalArgumentException: File/path invalid.");
                        jsonString = gson.toJson(respText);
                        System.out.println("---");
                        System.out.println(jsonString);
                        this.generateResponseAndClose(exchange, jsonString, returnCode);
                        return;
                    }
                    try {
                        String path = root_dir + filepath;
                        File f = new File(path);
                        if (!f.exists() || f.isDirectory()) {
                            System.out.println("This does not exist/This is a directory");
                            returnCode = 404;
                            respText.put("exception_type", "FileNotFoundException");
                            respText.put("exception_info", "FileNotFoundException: File/path not found.");
                            jsonString = gson.toJson(respText);
                            System.out.println("---");
                            System.out.println(jsonString);
                            this.generateResponseAndClose(exchange, jsonString, returnCode);
                            return;
                        }
                        long length = f.length();
                        respText.put("size", String.valueOf(length));
                        returnCode = 200;
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                } catch (Exception e) {
                    System.out.println("Illegal Argument");
                    returnCode = 404;
                    respText.put("exception_type", "IllegalArgumentException");
                    respText.put("exception_info", "IllegalArgumentException: File/path invalid.");
                    jsonString = gson.toJson(respText);
                    this.generateResponseAndClose(exchange, jsonString, returnCode);
                    return;
                }
                jsonString = gson.toJson(respText);
                System.out.println("---");
                System.out.println(jsonString);
            }
            else {
                returnCode = 404;
            }
            this.generateResponseAndClose(exchange, jsonString, returnCode);
        }));
    }

    /** Throws <code>UnsupportedOperationException</code>. */
    public void read()
    {
        this.client_skeleton.createContext("/storage_read", (exchange ->
        {
            System.out.println("Coming into Storage_Read Context");
            System.out.flush();
            HashMap<String, String> respText = new HashMap<String, String>();
            String jsonString = "";
            int returnCode = 200;
            if ("POST".equals(exchange.getRequestMethod())) {
                // parse request json
                try
                {
                    System.out.println("Coming into POST portion");
                    System.out.flush();
                    InputStreamReader isr = new InputStreamReader(exchange.getRequestBody(), "utf-8");
                    Map<String, Object> map = new HashMap<String, Object>();
                    map = (Map<String, Object>) gson.fromJson(isr, map.getClass());
                    System.out.println(map);
                    String filepath = (String) map.get("path");
                    Double offset = (Double) map.get("offset");
                    Double length = (Double) map.get("length");
                    System.out.println((root_dir+filepath) + " - This is the filepath");
                    System.out.flush();
                    if (filepath.equals("") || filepath == null || filepath.equals("null")) {
                        System.out.println("Illegal Argument");
                        returnCode = 404;
                        respText.put("exception_type", "IllegalArgumentException");
                        respText.put("exception_info", "IllegalArgumentException: File/path invalid.");
                        jsonString = gson.toJson(respText);
                        System.out.println("---");
                        System.out.println(jsonString);
                        this.generateResponseAndClose(exchange, jsonString, returnCode);
                        return;
                    }
                    if (offset < 0 || length < 0) {
                        System.out.println("Index out of Bound");
                        returnCode = 404;
                        respText.put("exception_type", "IndexOutOfBoundsException");
                        respText.put("exception_info", "IndexOutOfBoundsException: Offset/Length Index is negative/out of bounds.");
                        jsonString = gson.toJson(respText);
                        System.out.println("---");
                        System.out.println(jsonString);
                        this.generateResponseAndClose(exchange, jsonString, returnCode);
                        return;
                    }
                    String path = root_dir + filepath;
                    File f = new File(path);
                    if (!f.exists() || f.isDirectory()) {
                        System.out.println("This does not exist" + !f.exists() + " /This is a directory "+ f.isDirectory());
                        returnCode = 404;
                        respText.put("exception_type", "FileNotFoundException");
                        respText.put("exception_info", "FileNotFoundException: File/path not found.");
                        jsonString = gson.toJson(respText);
                        System.out.println("---");
                        System.out.println(jsonString);
                        this.generateResponseAndClose(exchange, jsonString, returnCode);
                        return;
                    }
                    FileInputStream fstream = new FileInputStream(f);
                    System.out.println("Values of offset, length and filesize are :");
                    System.out.println(offset+" "+length+" "+fstream.available());
                    if ((offset.intValue() + length.intValue()) <= fstream.available())
                    {
                        System.out.println("Read data properly");
                        byte[] bytes = new byte[length.intValue()];
                        fstream.read(bytes, offset.intValue(), length.intValue());
                        String str = Base64.getEncoder().encodeToString(bytes);
//                        String str = new String(bytes, StandardCharsets.UTF_8);
                        respText.put("data", str);
                        System.out.println(respText);
                        fstream.close();
                    } else {
                        System.out.println("Index out of Bound");
                        returnCode = 404;
                        respText.put("exception_type", "IndexOutOfBoundsException");
                        respText.put("exception_info", "IndexOutOfBoundsException: Offset/Length Index is negative/out of bounds.");
                        fstream.close();
                        jsonString = gson.toJson(respText);
                        System.out.println("---");
                        System.out.println(jsonString);
                        this.generateResponseAndClose(exchange, jsonString, returnCode);
                        return;
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    System.out.println("Illegal Argument");
                    returnCode = 404;
                    respText.put("exception_type", "IllegalArgumentException");
                    respText.put("exception_info", "IllegalArgumentException: File/path invalid.");
                    jsonString = gson.toJson(respText);
                    this.generateResponseAndClose(exchange, jsonString, returnCode);
                    return;
                }
                returnCode = 200;
                jsonString = gson.toJson(respText);
                System.out.println("---");
                System.out.println(jsonString);
            } else {
                returnCode = 404;
            }
            this.generateResponseAndClose(exchange, jsonString, returnCode);
        }));
    }

    /** Throws <code>UnsupportedOperationException</code>. */
    public void write()
    {
        this.client_skeleton.createContext("/storage_write", (exchange ->
        {
            System.out.println("Coming into storage_write Context");
            HashMap<String, String> respText = new HashMap<String, String>();
            String jsonString = "";
            int returnCode = 200;
            if ("POST".equals(exchange.getRequestMethod())) {
                // parse request json
                try
                {
                    System.out.println("Coming into POST portion");
                    InputStreamReader isr = new InputStreamReader(exchange.getRequestBody(), "utf-8");
                    Map<String, Object> map = new HashMap<String, Object>();
                    map = (Map<String, Object>) gson.fromJson(isr, map.getClass());
                    System.out.println(map);
                    String filepath = (String) map.get("path");
                    Double offset = (Double) map.get("offset");
                    String data = (String) map.get("data");
                    System.out.println((root_dir+filepath) + " - This is the filepath");
                    if (data == null) {
                        System.out.println("NullPointerException Argument");
                        returnCode = 404;
                        respText.put("exception_type", "NullPointerException");
                        respText.put("exception_info", "NullPointerException: Data invalid.");
                        jsonString = gson.toJson(respText);
                        System.out.println("---");
                        System.out.println(jsonString);
                        this.generateResponseAndClose(exchange, jsonString, returnCode);
                        return;
                    }
                    if (filepath.equals("") || filepath == null || filepath.equals("null")) {
                        System.out.println("Illegal Argument");
                        returnCode = 404;
                        respText.put("exception_type", "IllegalArgumentException");
                        respText.put("exception_info", "IllegalArgumentException: File/path invalid.");
                        jsonString = gson.toJson(respText);
                        System.out.println("---");
                        System.out.println(jsonString);
                        this.generateResponseAndClose(exchange, jsonString, returnCode);
                        return;
                    }
                    if (offset.intValue() < 0) {
                        System.out.println("Index out of Bound");
                        returnCode = 404;
                        respText.put("exception_type", "IndexOutOfBoundsException");
                        respText.put("exception_info", "IndexOutOfBoundsException: Offset/Length Index is negative/out of bounds.");
                        jsonString = gson.toJson(respText);
                        System.out.println("---");
                        System.out.println(jsonString);
                        this.generateResponseAndClose(exchange, jsonString, returnCode);
                        return;
                    }
                    String path = root_dir + filepath;
                    File f = new File(path);
                    if (!f.exists() || f.isDirectory()) {
                        System.out.println("This does not exist" + !f.exists() + " /This is a directory "+ f.isDirectory());
                        returnCode = 404;
                        respText.put("exception_type", "FileNotFoundException");
                        respText.put("exception_info", "FileNotFoundException: File/path not found.");
                        jsonString = gson.toJson(respText);
                        System.out.println("---");
                        System.out.println(jsonString);
                        this.generateResponseAndClose(exchange, jsonString, returnCode);
                        return;
                    }
                    FileOutputStream ostream = null;
                    ostream = new FileOutputStream(f, true);
                    System.out.println("Write data properly");
                    // TESTING
                    System.out.println("Before Writing");
                    FileReader fr = new FileReader(path);
                    int i;
                    while ((i=fr.read()) != -1)
                        System.out.print((char) i);
                    // TESTING ENDS

                    System.out.println("\nData Length is "+data.length());
                    System.out.println("Offset value is "+offset.intValue());
                    System.out.println("File Length is "+f.length());
                    System.out.println("Data length is " + Base64.getDecoder().decode(data).length);
                    int newOffset = offset.intValue();
                    if (offset.intValue() > f.length())
                    {
                        if (f.length() == 0)
                            newOffset = 0;
                        else
                            newOffset = toIntExact(f.length()) + 1;
                        System.out.println("New Offset value is "+newOffset);
                    }
//                        Base64.getEncoder().decode(data.getBytes());
//                        RandomAccessFile seek = new RandomAccessFile(path, "rw");
//                    Paths p = Paths.get(path);
                    RandomAccessFile aFile = new RandomAccessFile(path, "rw");
//                    FileChannel fchannel = FileChannel.open(p);
//                    FileChannel fchannel = ostream.getChannel();
                    FileChannel fchannel = aFile.getChannel();
                    ByteBuffer bb = ByteBuffer.wrap(Base64.getDecoder().decode(data));
                    fchannel.position(offset.intValue());
                    fchannel.write(bb);

                    // TESTING
                    System.out.println("\nAfter Writing");
                    FileReader fro = new FileReader(path);
                    int j;
                    while ((j=fro.read()) != -1)
                        System.out.print((char) j);
                    // TESTING ENDS
                    respText.put("success", "true");
                    System.out.println(' ');
                    System.out.println(respText);
                    ostream.close();
                    fchannel.close();
                } catch (Exception e) {
                    e.printStackTrace();
                    System.out.println("Illegal Argument");
                    returnCode = 404;
                    respText.put("exception_type", "IllegalArgumentException");
                    respText.put("exception_info", "IllegalArgumentException: File/path invalid.");
                }
                returnCode = 200;
                jsonString = gson.toJson(respText);
                System.out.println("---");
                System.out.println(jsonString);
            } else {
                returnCode = 404;
            }
            this.generateResponseAndClose(exchange, jsonString, returnCode);
        }));
    }

    /** Throws <code>UnsupportedOperationException</code>. */
    public void create()
    {
        this.command_skeleton.createContext("/storage_create", (exchange ->
        {
            System.out.println("Coming into storage_create Context");
            HashMap<String, String> respText = new HashMap<String, String>();
            String jsonString = "";
            int returnCode = 200;
            if ("POST".equals(exchange.getRequestMethod())) {
                // parse request json
                try
                {
                    System.out.println("Coming into POST portion");
                    InputStreamReader isr = new InputStreamReader(exchange.getRequestBody(), "utf-8");
                    Map<String, Object> map = new HashMap<String, Object>();
                    map = (Map<String, Object>) gson.fromJson(isr, map.getClass());
                    System.out.println(map);
                    String filepath = (String) map.get("path");
                    System.out.println((root_dir+filepath) + " - This is the filepath");
                    if (filepath.equals("") || filepath == null || filepath.equals("null")) {
                        System.out.println("Illegal Argument");
                        returnCode = 404;
                        respText.put("exception_type", "IllegalArgumentException");
                        respText.put("exception_info", "IllegalArgumentException: File/path invalid.");
                        jsonString = gson.toJson(respText);
                        System.out.println("---");
                        System.out.println(jsonString);
                        this.generateResponseAndClose(exchange, jsonString, returnCode);
                        return;
                    }
                    String path = root_dir + filepath;
                    File temp = new File(path);
                    String dir = temp.getParent();
                    String fil = temp.getName();
                    boolean flagdir = new File(dir).mkdirs();
                    boolean flag = new File(path).createNewFile();
                    if (flag)
                        respText.put("success", "true");
                    else
                        respText.put("success", "false");
                    System.out.println(respText);
                } catch (Exception e) {
                    e.printStackTrace();
                    System.out.println("Illegal Argument");
                    returnCode = 404;
                    respText.put("exception_type", "IllegalArgumentException");
                    respText.put("exception_info", "IllegalArgumentException: File/path invalid.");
                    jsonString = gson.toJson(respText);
                    this.generateResponseAndClose(exchange, jsonString, returnCode);
                    return;
                }
                returnCode = 200;
                jsonString = gson.toJson(respText);
                System.out.println("---");
                System.out.println(jsonString);
            } else {
                returnCode = 404;
            }
            this.generateResponseAndClose(exchange, jsonString, returnCode);
        }));
    }


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

    /** Throws <code>UnsupportedOperationException</code>. */
    public void delete()
    {
        this.command_skeleton.createContext("/storage_delete", (exchange ->
        {
            System.out.println("Coming into storage_delete Context");
            HashMap<String, String> respText = new HashMap<String, String>();
            String jsonString = "";
            int returnCode = 200;
            if ("POST".equals(exchange.getRequestMethod())) {
                // parse request json
                try
                {
                    System.out.println("Coming into POST portion");
                    InputStreamReader isr = new InputStreamReader(exchange.getRequestBody(), "utf-8");
                    Map<String, Object> map = new HashMap<String, Object>();
                    map = (Map<String, Object>) gson.fromJson(isr, map.getClass());
                    System.out.println(map);
                    String filepath = (String) map.get("path");
                    System.out.println((root_dir+filepath) + " - This is the filepath");
                    if (filepath.equals("") || filepath == null || filepath.equals("null") || filepath.equals(root_dir) || filepath.equals("/")) {
                        System.out.println("Illegal Argument");
                        returnCode = 404;
                        respText.put("exception_type", "IllegalArgumentException");
                        respText.put("exception_info", "IllegalArgumentException: File/path invalid.");
                        jsonString = gson.toJson(respText);
                        System.out.println("---");
                        System.out.println(jsonString);
                        this.generateResponseAndClose(exchange, jsonString, returnCode);
                        return;
                    }
                    File delete_files = new File(root_dir + filepath);
                    boolean flag;
                    if (delete_files.isDirectory())
                    {
                        flag = deleteDir(delete_files);
                    } else {
                        System.out.println(delete_files);
                        flag = delete_files.delete();
                        System.out.println(flag);
                        String dir = delete_files.getParent();
                        File dirfile = new File(dir);
                        while (dirfile.list().length == 0)
                        {
                            System.out.println(dirfile);
                            flag = dirfile.delete();
                            System.out.println(flag);
                            String d = dirfile.getParent();
                            dirfile = new File(d);
                        }
                    }
                    if (flag)
                        respText.put("success", "true");
                    else
                        respText.put("success", "false");
                    System.out.println(respText);
                } catch (Exception e) {
                    e.printStackTrace();
                    System.out.println("Illegal Argument");
                    returnCode = 404;
                    respText.put("exception_type", "IllegalArgumentException");
                    respText.put("exception_info", "IllegalArgumentException: File/path invalid.");
                    jsonString = gson.toJson(respText);
                    this.generateResponseAndClose(exchange, jsonString, returnCode);
                    return;
                }
                returnCode = 200;
                jsonString = gson.toJson(respText);
                System.out.println("---");
                System.out.println(jsonString);
            } else {
                returnCode = 404;
            }
            this.generateResponseAndClose(exchange, jsonString, returnCode);
        }));
    }

    /** Throws <code>UnsupportedOperationException</code>. */
    public void copy()
    {
        this.command_skeleton.createContext("/storage_copy", (exchange ->
        {
            System.out.println("====================Coming into storage_copy Context==================");
            HashMap<String, String> respText = new HashMap<String, String>();
            String jsonString = "";
            int returnCode = 200;
            if ("POST".equals(exchange.getRequestMethod())) {
                // parse request json
                try
                {
                    System.out.println("Coming into POST portion");
                    InputStreamReader isr = new InputStreamReader(exchange.getRequestBody(), "utf-8");
                    Map<String, Object> map = new HashMap<String, Object>();
                    map = (Map<String, Object>) gson.fromJson(isr, map.getClass());
                    System.out.println(map);
                    String filepath = (String) map.get("path");
                    String serverIp = (String) map.get("server_ip");
                    if (serverIp.equals("localhost"))
                    {
                        serverIp = "127.0.0.1";
                    }
                    Double serverPort = (Double) map.get("server_port");
                    System.out.println((serverIp) + " - This is the IP");
                    System.out.println((serverPort) + " - This is the Port");
                    System.out.println((root_dir+filepath) + " - This is the filepath");
                    if (filepath.equals("") || filepath == null || filepath.equals("null") || filepath.equals(root_dir) || filepath.equals("/")) {
                        System.out.println("Illegal Argument");
                        returnCode = 404;
                        respText.put("exception_type", "IllegalArgumentException");
                        respText.put("exception_info", "IllegalArgumentException: File/path invalid.");
                        jsonString = gson.toJson(respText);
                        System.out.println("---");
                        System.out.println(jsonString);
                        this.generateResponseAndClose(exchange, jsonString, returnCode);
                        return;
                    }

                    Map<String, Object> sendText = new HashMap<String, Object>();
                    sendText.put("path", filepath);
                    System.out.println(sendText);
                    HttpClient client = HttpClient.newHttpClient();

                    HttpRequest request = HttpRequest.newBuilder()
                            .uri(URI.create("http://"+serverIp+":" + serverPort.intValue() + "/storage_size"))
                            .setHeader("Content-Type", "application/json")
                            .POST(HttpRequest.BodyPublishers.ofString(gson.toJson(sendText)))
                            .build();
                    System.out.println(request);
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
                        System.out.println("---");
                        System.out.println(response.body());
                        this.generateResponseAndClose(exchange, response.body(), returnCode);
                        return;
                    } else {
                        System.out.println("2@2@");
                        sendText.put("offset", 0.0);
                        Map<String, String> temp = new HashMap<String,String>();
                        temp = (Map<String, String>) gson.fromJson(response.body(), temp.getClass());
                        System.out.println(temp.get("size").getClass());
                        sendText.put("length", new Double(temp.get("size")));
                        System.out.println(sendText);
                        HttpRequest request2 = HttpRequest.newBuilder()
                                .uri(URI.create("http://"+serverIp+":" + serverPort.intValue() + "/storage_read"))
                                .setHeader("Content-Type", "application/json")
                                .POST(HttpRequest.BodyPublishers.ofString(gson.toJson(sendText)))
                                .build();
                        System.out.println(request2);
                        HttpResponse<String> response2;
                        try
                        {
                            response2 = client.send(request2, HttpResponse.BodyHandlers.ofString());
                        }
                        catch (Throwable t)
                        {
                            throw new TestFailed("unable to send request to naming server", t);
                        }
                        System.out.println(response2);
                        System.out.println(response2.body());

                        // Reading done. Now writing.
                        Map<String, Object> tempread = new HashMap<String,Object>();
                        tempread = (Map<String, Object>) gson.fromJson(response2.body(), tempread.getClass());
                        System.out.println("Temp Read is " + tempread);
                        byte[] byte_data_write = Base64.getDecoder().decode((String) tempread.get("data"));
                        String decodedString = new String(byte_data_write);
                        System.out.println("Data is " + decodedString);
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
//                        RandomAccessFile aFile = new RandomAccessFile((root_dir+filepath), "rw");
//                        FileChannel fchannel = aFile.getChannel();
//                        ByteBuffer bb = ByteBuffer.wrap(byte_data_write);
//                        fchannel.position(0);
//                        fchannel.write(bb);


                        // TESTING
                        System.out.println("\nAfter Writing");
                        FileReader fro = new FileReader((root_dir+filepath));
                        int j;
                        while ((j=fro.read()) != -1)
                            System.out.print((char) j);
                        // TESTING ENDS
                        respText.put("success", "true");
                        System.out.println("====================Getting out of storage_copy Context==================");
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    System.out.println("IOException");
                    returnCode = 404;
                    respText.put("exception_type", "IOException");
                    respText.put("exception_info", "IOException: File/path IO not valid.");
                    jsonString = gson.toJson(respText);
                    this.generateResponseAndClose(exchange, jsonString, returnCode);
                    return;
                }
                returnCode = 200;
                jsonString = gson.toJson(respText);
                System.out.println("---");
                System.out.println(jsonString);
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