package jsonhelper;

public class RegisterRequest {
    public String storage_ip;
    public int client_port;
    public int command_port;
    public String[] files;
    
    public RegisterRequest(String storage_ip, int client_port, int command_port,
        String[] files) {
        this.storage_ip = storage_ip;
        this.client_port = client_port;
        this.command_port = command_port;
        this.files = files;
    }

    @Override
    public String toString() {
        String result = "RegisterRequest: " + "storage_ip = " + storage_ip + " client_port = " +
            client_port + " command_port = " + command_port;
        for (String s : files) {
            result += " " + s;
        }
        return result;
    }
}
