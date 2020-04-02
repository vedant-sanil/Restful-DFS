package storage;

import java.lang.String;

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

    public int getClient_port() {
        return client_port;
    }

    public int getCommand_port() {
        return command_port;
    }

    public String getStorage_ip() {
        return storage_ip;
    }

    public String[] getFiles() {
        return files;
    }

    public boolean verifySameServer(String other_storage_ip, int other_command_port) {
        if ((other_storage_ip.equals(this.storage_ip)) && (other_command_port == this.command_port)) {
            return true;
        } else {
            return false;
        }
    }
}