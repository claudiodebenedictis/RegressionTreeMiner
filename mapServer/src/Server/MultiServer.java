package Server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class MultiServer {
    private int PORT = 8080;

    public MultiServer(int port) {
        PORT = port;
        run();
    }

    private void run() {
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            while (true) {
                Socket socket = serverSocket.accept();

                try {
                    new ServerOneClient(socket);
                } catch (IOException exception) {
                    try {
                        socket.close();
                    } catch (IOException closeException) {
                        // The connection is already closing.
                    }

                    System.out.println(exception.toString());
                }
            }
        } catch (IOException exception) {
            System.out.println(exception.toString());
        }
    }

    public static void main(String args[]) {
        new MultiServer(8080);
    }
}
