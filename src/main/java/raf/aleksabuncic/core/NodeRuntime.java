package raf.aleksabuncic.core;

import lombok.Getter;
import raf.aleksabuncic.core.handler.ConnectionHandler;
import raf.aleksabuncic.core.handler.Sender;
import raf.aleksabuncic.types.Snapshot;
import raf.aleksabuncic.types.Message;
import raf.aleksabuncic.types.Node;

import java.util.HashMap;
import java.util.Map;

public class NodeRuntime {
    @Getter
    private final Map<Integer, Integer> requestSourceMap = new HashMap<>();
    @Getter
    private final Node nodeModel;
    @Getter
    private final Map<Integer, Integer> neighborPortMap;
    private Snapshot activeSnapshot;

    public NodeRuntime(Node nodeModel, Map<Integer, Integer> neighborPortMap) {
        this.nodeModel = nodeModel;
        this.neighborPortMap = neighborPortMap;
    }

    /**
     * Starts the node runtime.
     */
    public void start() {
        int port = nodeModel.getPort();
        new Thread(new ConnectionHandler(this, port)).start();
    }

    /**
     * Tries to send bitcakes to a neighbor.
     *
     * @param neighborId ID of the neighbor to send to.
     * @param amount     Amount of bitcakes to send.
     */
    public synchronized void trySendBitcakes(int neighborId, int amount) {
        if (!nodeModel.getNeighbors().contains(neighborId)) {
            log("Cannot send to Node " + neighborId + ": not a neighbor.");
            return;
        }

        if (nodeModel.getBitcake() < amount) {
            log("Not enough bitcakes.");
            return;
        }

        nodeModel.setBitcake(nodeModel.getBitcake() - amount);
        Message msg = new Message("TRANSFER", nodeModel.getId(), String.valueOf(amount));
        int port = neighborPortMap.get(neighborId);
        Sender.sendMessage("localhost", port, msg);
        log("Sent " + amount + " bitcakes to Node " + neighborId);
    }

    /**
     * Receives bitcakes from a neighbor.
     *
     * @param amount   Amount of bitcakes received.
     * @param senderId ID of the neighbor that sent the bitcakes.
     */
    public synchronized void receiveBitcakes(int amount, int senderId) {
        Message m = new Message("TRANSFER", senderId, String.valueOf(amount));

        if (activeSnapshot != null) {
            activeSnapshot.handleMessage(m);
        }

        nodeModel.setBitcake(nodeModel.getBitcake() + amount);
        log("Received " + amount + " bitcakes from Node " + senderId);
    }

    public void handleMessage(Message message) {
        int senderId = message.senderId();

        switch (message.type()) {
            case "TRANSFER" -> {
                int amount = Integer.parseInt(message.content());
                receiveBitcakes(amount, senderId);
            }
            case "CHECKPOINT_REQUEST", "CHECKPOINT_ACK", "SNAPSHOT_MARKER" -> {
                if (activeSnapshot != null) {
                    activeSnapshot.handleMessage(message);
                } else {
                    log("No active snapshot to handle " + message.type() + ": " + message);
                }
            }
            default -> log("Unknown message type: " + message.type());
        }
    }


    /**
     * Sends a message to a neighbor node.
     *
     * @param neighborId ID of the neighbor node.
     * @param message    Message to send.
     */
    public void sendMessageTo(int neighborId, Message message) {
        if (!neighborPortMap.containsKey(neighborId)) {
            log("Cannot send to unknown neighbor: " + neighborId);
            return;
        }
        int port = neighborPortMap.get(neighborId);
        try {
            Sender.sendMessage("localhost", port, message);
            log("Sent message to Node " + neighborId + ": " + message);
        } catch (Exception e) {
            log("Failed to send message to Node " + neighborId + ": " + e.getMessage());
        }
    }

    /**
     * Set the current snapshot type.
     *
     * @param snapshot Snapshot to set.
     */
    public synchronized void setSnapshot(Snapshot snapshot) {
        this.activeSnapshot = snapshot;
    }

    /**
     * Start snapshot.
     */
    public synchronized void startSnapshot() {
        if (activeSnapshot != null) {
            activeSnapshot.initiate();
        } else {
            log("No snapshot strategy set.");
        }
    }

    /**
     * Returns the ID of the node.
     *
     * @return Node ID.
     */
    public int getId() {
        return nodeModel.getId();
    }

    /**
     * Returns the current bitcake balance.
     *
     * @return Bitcake balance.
     */
    public int getBitcake() {
        return nodeModel.getBitcake();
    }

    /**
     * Logs a message.
     *
     * @param msg Message to log.
     */
    private void log(String msg) {
        System.out.println("[Node " + getId() + "] " + msg);
    }
}