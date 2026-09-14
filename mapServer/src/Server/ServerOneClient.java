package Server;

import data.Data;
import data.TrainingDataException;
import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.net.SocketException;
import tree.RegressionTree;

public class ServerOneClient extends Thread {
    private Socket socket;
    private ObjectInputStream in;
    private ObjectOutputStream out;

    public ServerOneClient(Socket s) throws IOException {
        socket = s;
        out = new ObjectOutputStream(socket.getOutputStream());
        out.flush();
        in = new ObjectInputStream(socket.getInputStream());
        start();
    }

    @Override
    public void run() {
        Data trainingSet = null;
        RegressionTree regressionTree = null;

        try {
            while (true) {
                int command = (Integer) in.readObject();

                switch (command) {
                    case 0:
                        try {
                            trainingSet = new Data((String) in.readObject());
                            out.writeObject("OK");
                        } catch (TrainingDataException exception) {
                            trainingSet = null;
                            out.writeObject(exception.toString());
                        }
                        out.flush();
                        break;

                    case 1:
                        regressionTree = new RegressionTree(trainingSet);
                        out.writeObject("OK");
                        out.flush();
                        break;

                    case 2:
                        try {
                            regressionTree = RegressionTree.carica(
                                (String) in.readObject()
                            );
                        } catch (IOException | ClassNotFoundException exception) {
                            regressionTree = null;
                            out.writeObject(exception.toString());
                            out.flush();
                            break;
                        }

                        out.writeObject("OK");
                        out.flush();
                        break;

                    case 3:
                        try {
                            Double predictedClass = regressionTree.predictClass(in, out);
                            out.writeObject("OK");
                            out.writeObject(predictedClass);
                        } catch (UnknownValueException exception) {
                            out.writeObject(exception.toString());
                        }
                        out.flush();
                        break;
                }
            }
        } catch (EOFException | SocketException exception) {
            // The client closed the connection.
        } catch (IOException | ClassNotFoundException exception) {
            System.out.println(exception.toString());
        } finally {
            closeConnection();
        }
    }

    private void closeConnection() {
        try {
            in.close();
        } catch (IOException exception) {
            // The connection is already closing.
        }

        try {
            out.close();
        } catch (IOException exception) {
            // The connection is already closing.
        }

        try {
            socket.close();
        } catch (IOException exception) {
            // The connection is already closing.
        }
    }
}
