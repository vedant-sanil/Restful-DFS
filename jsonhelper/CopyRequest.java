package jsonhelper;

public class CopyRequest {
    public String path;
    public String server_ip;
    public int server_port;

    public CopyRequest(String path, String server_ip, int server_port) {
        this.path = path;
        this.server_ip = server_ip;
        this.server_port = server_port;
    }
    
    @Override
    public String toString() {
        return "CopyRequest: " + "path = <" + path + "> server_ip = <" + server_ip + "> server_port = <" +
            server_port + ">";
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (!(obj instanceof CopyRequest)) return false;
        CopyRequest copyRequest = (CopyRequest) obj;
        return this.path.equals(copyRequest.path) && this.server_ip.equals(copyRequest.server_ip)
                && this.server_port == copyRequest.server_port;
    }
}
