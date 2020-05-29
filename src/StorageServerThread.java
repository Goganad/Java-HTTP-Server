import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

public class StorageServerThread extends Thread {

    private final Socket socket;
    private final String folderPath;

    public StorageServerThread(Socket socket, String folderPath) {
        this.socket = socket;
        this.folderPath = folderPath;
    }

    @Override
    public void run() {
        super.run();

        try (DataInputStream inputStream = new DataInputStream(socket.getInputStream());
             DataOutputStream outputStream = new DataOutputStream(socket.getOutputStream())){

            Request request = new Request(inputStream);
            Response response = new Functions(request, folderPath).getResponse();
            response.send(outputStream);

        } catch (IOException e) {
            e.printStackTrace();
        }

        try {
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

}