package jsonhelper;

public class WriteRequest {
    public String path;
    public long offset;
    public String data;

    // Note: data should be a byte[] after base64 encoding
    public WriteRequest(String path, long offset, String data) {
        this.path = path;
        this.offset = offset;
        this.data = data;
    }
}
