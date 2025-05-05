package raf.aleksabuncic.types;

import raf.aleksabuncic.core.NodeRuntime;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Set;
import java.util.concurrent.locks.ReentrantLock;

public abstract class Snapshot {
    protected final NodeRuntime runtime;
    private static final ReentrantLock fileLock = new ReentrantLock();

    public Snapshot(NodeRuntime runtime) {
        this.runtime = runtime;
    }

    /**
     * Initializes snapshot.
     */
    public abstract void initiate();

    /**
     * Handles a snapshot-related message.
     *
     * @param message Message to handle.
     */
    public abstract void handleMessage(Message message);

    /**
     * Writes a line to output.txt in a thread-safe manner.
     *
     * @param line Line to write.
     */
    protected void writeToOutput(String line) {
        fileLock.lock();
        try (PrintWriter writer = new PrintWriter(new FileWriter("output.txt", true))) {
            writer.println(line);
        } catch (IOException e) {
            System.err.println("Failed to write to output.txt: " + e.getMessage());
        } finally {
            fileLock.unlock();
        }
    }

    /**
     * Logs a message to the console.
     *
     * @param msg Message to log.
     */
    protected void log(String msg) {
        System.out.println("[Snapshot@Node " + runtime.getId() + "] " + msg);
    }

    /**
     * Gets current bitcake balance.
     *
     * @return Bitcake value.
     */
    protected int getBitcake() {
        return runtime.getBitcake();
    }

    /**
     * Gets this node's ID.
     *
     * @return Node ID.
     */
    protected int getNodeId() {
        return runtime.getId();
    }

    /**
     * Buffers a message (if needed for future extensions).
     *
     * @param message Message to buffer.
     */
    protected void bufferMessage(Message message) {
        log("Buffered message: " + message);
    }

    /**
     * Sends marker message to all neighbors.
     *
     * @param markerType Type of marker message (e.g., "SNAPSHOT_MARKER")
     */
    protected void sendMarkerToAllNeighbors(String markerType) {
        for (int neighborId : runtime.getNodeModel().getNeighbors()) {
            Message marker = new Message(markerType, getNodeId(), "");
            runtime.sendMessageTo(neighborId, marker);
        }
    }

    /**
     * Checks if snapshot is complete (all neighbors have sent a marker).
     *
     * @param receivedMarkers Set of node IDs from which marker was received.
     * @return true if snapshot is complete, false otherwise.
     */
    protected boolean isSnapshotComplete(Set<Integer> receivedMarkers) {
        return receivedMarkers.containsAll(runtime.getNodeModel().getNeighbors());
    }

    /**
     * Sets node state based on snapshot status.
     *
     * @param active true if snapshot is active, false to reset to available.
     */
    protected void setSnapshotState(boolean active) {
        runtime.getNodeModel().setState(active ? NodeState.SNAPSHOT : NodeState.AVAILABLE);
    }
}