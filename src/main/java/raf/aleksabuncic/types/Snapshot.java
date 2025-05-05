package raf.aleksabuncic.types;

import raf.aleksabuncic.core.NodeRuntime;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Set;

public abstract class Snapshot {
    protected final NodeRuntime runtime;

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
     * Writes a line to output/output.txt in a thread-safe manner.
     *
     * @param line Line to write.
     */
    protected void writeToOutput(String line) {
        File outputFile = new File("output/output.txt");
        synchronized (Snapshot.class) {
            try {
                outputFile.getParentFile().mkdirs();
                if (!outputFile.exists()) {
                    outputFile.createNewFile();
                }
                try (FileWriter writer = new FileWriter(outputFile, true)) {
                    writer.write(line + System.lineSeparator());
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
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

    /**
     * Writes the current state of the node (Node ID and bitcake balance) to output.txt.
     */
    protected void writeNodeStateToOutput() {
        String state = "SNAPSHOT NODE_STATE: Node " + getNodeId() + " | Bitcakes: " + getBitcake();
        writeToOutput(state);
    }
}