package storage;

import java.lang.String;

/** Helper class for storing the Storage Files, IP and Ports to be used during time of
 * Replication for the Naming Server
 */
public class StorageServerInfo {
    private String storage_ip;
    private int client_port;
    private int command_port;
    private String[] files;

    public StorageServerInfo(String storage_ip, int client_port, int command_port, String[] files) {
        this.storage_ip = storage_ip;
        this.client_port = client_port;
        this.command_port = command_port;
        this.files = files;
    }

    /** Getter for the Client Port */
    public int getClient_port() {
        return client_port;
    }

    /** Getter for the Command Port */
    public int getCommand_port() {
        return command_port;
    }

    /** Getter for the Storage IP */
    public String getStorage_ip() {
        return storage_ip;
    }

    /** Getter for the files stored on the server */
    public String[] getFiles() {
        return files;
    }

    /** Check if 2 servers are the same */
    public boolean verifySameServer(String other_storage_ip, int other_command_port) {
        if ((other_storage_ip.equals(this.storage_ip)) && (other_command_port == this.command_port)) {
            return true;
        } else {
            return false;
        }
    }
}