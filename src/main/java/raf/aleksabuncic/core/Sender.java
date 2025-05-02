package raf.aleksabuncic.core;

import raf.aleksabuncic.types.Message;

import java.io.ObjectOutputStream;
import java.net.Socket;

public class Sender {
    public static void sendMessage(String host, int port, Message message) {
        try (
                Socket socket = new Socket(host, port);
                ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream())
        ) {
            out.writeObject(message);
            out.flush();
        } catch (Exception e) {
            System.err.println("Failed to send message to " + host + ":" + port);
            e.printStackTrace();
        }
    }
}
