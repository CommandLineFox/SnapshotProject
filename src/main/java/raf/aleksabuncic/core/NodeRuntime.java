package raf.aleksabuncic.core;

import raf.aleksabuncic.types.Message;
import raf.aleksabuncic.types.Node;

import java.util.Map;

public class NodeRuntime {
    private final Node nodeModel;
    private final Map<Integer, Integer> neighborPortMap;

    public NodeRuntime(Node nodeModel, Map<Integer, Integer> neighborPortMap) {
        this.nodeModel = nodeModel;
        this.neighborPortMap = neighborPortMap;
    }

    public void start() {
        int port = 5000 + nodeModel.getId();
        new Thread(new ConnectionHandler(this, port)).start();
    }

    public synchronized boolean trySendBitcakes(int neighborId, int amount) {
        if (!nodeModel.getNeighbors().contains(neighborId)) {
            log("Cannot send to Node " + neighborId + ": not a neighbor.");
            return false;
        }

        if (nodeModel.getBitcake() < amount) {
            log("Not enough bitcakes.");
            return false;
        }

        nodeModel.setBitcake(nodeModel.getBitcake() - amount);
        Message msg = new Message("TRANSFER", nodeModel.getId(), String.valueOf(amount));
        int port = neighborPortMap.get(neighborId);
        Sender.sendMessage("localhost", port, msg);
        log("Sent " + amount + " bitcakes to Node " + neighborId);
        return true;
    }

    public synchronized void receiveBitcakes(int amount, int senderId) {
        nodeModel.setBitcake(nodeModel.getBitcake() + amount);
        log("Received " + amount + " bitcakes from Node " + senderId);
    }

    public void handleMessage(Message message) {
        int senderId = message.getSenderId();
        if (!nodeModel.getNeighbors().contains(senderId)) {
            log("Rejected message from non-neighbor Node " + senderId);
            return;
        }

        if ("TRANSFER".equals(message.getType())) {
            int amount = Integer.parseInt(message.getContent());
            receiveBitcakes(amount, senderId);
        } else {
            log("Unknown message type: " + message);
        }
    }

    public int getId() {
        return nodeModel.getId();
    }

    public int getBitcake() {
        return nodeModel.getBitcake();
    }

    private void log(String msg) {
        System.out.println("[Node " + getId() + "] " + msg);
    }
}