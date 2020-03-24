package jsonhelper;

public class ServerInfo {
    public String server_ip;
    public int server_port;

    public ServerInfo(String server_ip, int server_port) {
        this.server_ip = server_ip;
        this.server_port = server_port;
    }
    
    @Override
    public String toString() {
        return "ServerInfo: " + "server_ip = <" + server_ip + "> server_port = <" +
            server_port + ">";
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (!(obj instanceof ServerInfo)) return false;
        ServerInfo serverInfo = (ServerInfo) obj;
        return this.server_ip.equals(serverInfo.server_ip) && this.server_port == serverInfo.server_port;
    }

    @Override
    public int hashCode() {
        return server_ip.hashCode() * 31 + server_port;
    }
}
