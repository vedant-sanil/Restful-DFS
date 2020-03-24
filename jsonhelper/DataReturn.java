package jsonhelper;

/**
 * Note:
 * The data String MUST be the result of the
 * base64 encoding byte arrays
 * https://docs.oracle.com/javase/8/docs/api/java/util/Base64.Encoder.html
 * https://stackoverflow.com/questions/20706783/put-byte-array-to-json-and-vice-versa
 */
public class DataReturn {
    public String data;

    public DataReturn(String data) {
        this.data = data;
    }
}
