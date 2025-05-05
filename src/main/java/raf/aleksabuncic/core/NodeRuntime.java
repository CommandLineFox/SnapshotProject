package raf.aleksabuncic.core;

import raf.aleksabuncic.types.Snapshot;
import raf.aleksabuncic.types.Message;
import raf.aleksabuncic.types.Node;

import java.util.Map;

public class NodeRuntime {
    private final Node nodeModel;
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
        if (!nodeModel.isAvailable()) {
            log("Cannot send bitcakes while in SNAPSHOT state.");
            return;
        }

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
        if (!nodeModel.isAvailable()) {
            log("Ignoring bitcakes received while in SNAPSHOT state.");
            return;
        }

        nodeModel.setBitcake(nodeModel.getBitcake() + amount);
        log("Received " + amount + " bitcakes from Node " + senderId);
    }

    /**
     * Handles a single message.
     *
     * @param message Message to handle.
     */
    public void handleMessage(Message message) {
        int senderId = message.senderId();
        if (!nodeModel.getNeighbors().contains(senderId)) {
            log("Rejected message from non-neighbor Node " + senderId);
            return;
        }

        if ("TRANSFER".equals(message.type())) {
            int amount = Integer.parseInt(message.content());
            receiveBitcakes(amount, senderId);
        } else if (message.type().startsWith("SNAPSHOT")) {
            if (activeSnapshot != null) {
                activeSnapshot.handleMessage(message);
            }
        } else {
            log("Unknown message type: " + message);
        }
    }

    /**
     * Set the current snapshot type
     *
     * @param snapshot Snapshot to set
     */
    public synchronized void setSnapshot(Snapshot snapshot) {
        this.activeSnapshot = snapshot;
    }

    /**
     * Start snapshot
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