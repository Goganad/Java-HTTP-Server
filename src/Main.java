import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class Main {

    public static void main(String[] args) {
        int port;
        if(args.length == 2){
            try {
                port = Integer.parseInt(args[0]);
            } catch (NumberFormatException e) {
                System.out.println("Input PORT and DIRECTORY NAME as arguments");
                return;
            }
        } else {
            System.out.println("Input PORT and DIRECTORY NAME as arguments");
            return;
        }

        String pathStr;
        try {
            pathStr = new java.io.File(args[1]).getCanonicalPath().replace("\\", "/");
            Path path = Paths.get(pathStr);
            if (!Files.exists(path)||!Files.isDirectory(path)) {
                System.out.println("Invalid path");
                return;
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Input PORT and DIRECTORY NAME as arguments");
            return;
        }
        System.out.println("Storage folder: " + pathStr);

        try (ServerSocket server = new ServerSocket(port)) {
            System.out.println("Server working on localhost:" + port);

            while (true) {
                Socket socket = server.accept();

                StorageServerThread thread = new StorageServerThread(socket, pathStr);
                thread.start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}