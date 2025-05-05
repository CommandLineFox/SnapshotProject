package raf.aleksabuncic.core.handler;

import raf.aleksabuncic.core.NodeRuntime;
import raf.aleksabuncic.types.Message;

import java.io.ObjectInputStream;
import java.net.ServerSocket;
import java.net.Socket;

public class ConnectionHandler implements Runnable {
    private final NodeRuntime node;
    private final int port;

    public ConnectionHandler(NodeRuntime node, int port) {
        this.node = node;
        this.port = port;
    }

    @Override
    public void run() {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("[Node " + node.getId() + "] Listening on port " + port);
            while (true) {
                Socket socket = serverSocket.accept();
                new Thread(() -> handle(socket)).start();
            }
        } catch (Exception e) {
            System.err.println("[Node " + node.getId() + "] Server error:");
            e.printStackTrace();
        }
    }

    /**
     * Handles a single connection.
     *
     * @param socket Socket to handle.
     */
    private void handle(Socket socket) {
        try (ObjectInputStream in = new ObjectInputStream(socket.getInputStream())) {
            Message msg = (Message) in.readObject();
            node.handleMessage(msg);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
