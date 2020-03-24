package jsonhelper;

public class LockRequest {
    public String path;
    public boolean exclusive;

    public LockRequest(String path, boolean exclusive) {
        this.path = path;
        this.exclusive = exclusive;
    }
}
