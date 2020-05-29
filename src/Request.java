import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Scanner;
import java.util.Vector;

public class Request {

    private final String requestLine;
    private final HashMap<String, String> headers = new HashMap<>(0);
    private byte[] data = new byte[]{};

    public byte[] getData() {
        return data;
    }

    public String getHeaderValue(String key) {
        return headers.get(key);
    }

    public Request(DataInputStream inputStream) throws IOException {
        Scanner scanner = new Scanner(inputStream);
        String line;
        if (scanner.hasNextLine()) {
            line = scanner.nextLine();
            requestLine = line;
        } else {
            throw new IOException("Nothing to read from socket");
        }

        while (scanner.hasNextLine()) {
            line = scanner.nextLine();
            if (!line.equals("")) {
                int spaceIndex = line.indexOf(" ");
                headers.put(line.substring(0, spaceIndex - 1), line.substring(spaceIndex + 1));
            } else {
                break;
            }
        }

        if (headers.get("Content-Length") != null) {
            int count = Integer.parseInt(headers.get("Content-Length"));
            data = inputStream.readNBytes(count);
        }
    }

    public void outputRequest() {
        System.out.println(requestLine);
        for(HashMap.Entry<String, String> entry : headers.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();

            System.out.println(key + ": " + value);
        }
    }

    public String getRequestMethod() {
        if (requestLine != null) {
            String[] split = requestLine.split(" ");
            return split[0];
        }
        return null;
    }

    public String getRelativePath() {
        if (requestLine != null) {
            String[] split = requestLine.split(" ");
            String path = split[1];
            try {
                URL url = new URL(path);
                path = url.getPath();
            } catch (MalformedURLException e) {
                // Path is already relative
            }
            return path;
        }
        return null;
    }
}