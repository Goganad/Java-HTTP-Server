import java.util.HashMap;

public class HTTPContentType {
    private static final HashMap<String, String> types = new HashMap<>(0);
    static {
        types.put("pdf", "application/pdf");
        types.put("txt", "text/plain");
        types.put("exe", "application/octet-stream");
        types.put("zip", "application/zip");
        types.put("gif", "image/gif");
        types.put("png", "image/png");
        types.put("jpeg", "image/jpg");
        types.put("jpg", "image/jpg");
    }

    public static String getContentTypeFor(String extension) {
        String contentType = types.get(extension);
        if (contentType == null) {
            contentType = "application/octet-stream";
        }
        return contentType;
    }
}
