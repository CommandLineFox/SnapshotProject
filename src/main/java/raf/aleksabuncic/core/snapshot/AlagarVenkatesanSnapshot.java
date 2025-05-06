package raf.aleksabuncic.core.snapshot;

import raf.aleksabuncic.core.NodeRuntime;
import raf.aleksabuncic.types.Message;
import raf.aleksabuncic.types.NodeState;
import raf.aleksabuncic.types.Snapshot;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class AlagarVenkatesanSnapshot extends Snapshot {
    private boolean recorded = false;
    private final Map<Integer, Integer> channelStates = new HashMap<>();
    private final Set<Integer> receivedMarkers = new HashSet<>();

    public AlagarVenkatesanSnapshot(NodeRuntime runtime) {
        super(runtime);
    }

    @Override
    public synchronized void initiate() {
        if (!canInitiateSnapshot()) {
            log("Cannot initiate snapshot: already in SNAPSHOT state or invalid node state.");
            return;
        }

        log("Initiating Alagar-Venkatesan snapshot...");
        recorded = true;
        setSnapshotState(true);

        writeNodeStateToOutput();

        sendMarkerToAllNeighbors("SNAPSHOT_MARKER");
    }

    @Override
    public synchronized void handleMessage(Message message) {
        switch (message.type()) {
            case "SNAPSHOT_MARKER" -> handleMarkerMessage(message.senderId());
            case "TRANSFER" -> handleTransferMessage(message.senderId(), Integer.parseInt(message.content()));
            default -> log("Unknown message type received: " + message.type());
        }
    }

    /**
     * Handles a marker message.
     *
     * @param senderId ID of the sender.
     */
    private void handleMarkerMessage(int senderId) {
        log("Received MARKER from Node " + senderId);

        if (!recorded) {
            initiate();
        }

        receivedMarkers.add(senderId);

        if (isSnapshotComplete(receivedMarkers)) {
            log("Snapshot complete. Writing channel states...");
            writeChannelStatesToOutput();
            resetSnapshot();
        }
    }

    /**
     * Handles a transfer message.
     *
     * @param senderId ID of the sender.
     * @param amount   Amount of bitcakes transferred.
     */
    private void handleTransferMessage(int senderId, int amount) {
        if (recorded && !receivedMarkers.contains(senderId)) {
            channelStates.merge(senderId, amount, Integer::sum);
            log("Buffered " + amount + " from Node " + senderId + " during snapshot.");
        }
    }

    /**
     * Reset snapshot values
     */
    private void resetSnapshot() {
        setSnapshotState(false);
        recorded = false;
        receivedMarkers.clear();
        channelStates.clear();
    }

    /**
     * Check if it is the snapshot can be initialized
     *
     * @return True if it can be, false if not
     */
    private boolean canInitiateSnapshot() {
        return runtime.getNodeModel().getState() == NodeState.AVAILABLE && !recorded;
    }

    /**
     * Write channel state to output
     */
    private void writeChannelStatesToOutput() {
        for (Map.Entry<Integer, Integer> entry : channelStates.entrySet()) {
            writeToOutput("CHANNEL_STATE from Node " + entry.getKey() + ": " + entry.getValue() + " bitcakes");
        }
    }
}