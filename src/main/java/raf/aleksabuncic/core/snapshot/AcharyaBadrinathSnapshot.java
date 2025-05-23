package raf.aleksabuncic.core.snapshot;

import raf.aleksabuncic.core.NodeRuntime;
import raf.aleksabuncic.types.Message;
import raf.aleksabuncic.types.NodeState;
import raf.aleksabuncic.types.Snapshot;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class AcharyaBadrinathSnapshot extends Snapshot {
    private boolean recorded = false;
    private final Map<Integer, Integer> channelStates = new HashMap<>();
    private final Set<Integer> receivedFrom = new HashSet<>();

    public AcharyaBadrinathSnapshot(NodeRuntime runtime) {
        super(runtime);
    }

    @Override
    public synchronized void initiate() {
        if (!canInitiateSnapshot()) {
            log("Cannot initiate snapshot: already in SNAPSHOT state or invalid node state.");
            return;
        }

        log("Initiating Acharya-Badrinath snapshot locally...");
        recorded = true;
        setSnapshotState(true);
        writeNodeStateToOutput();

        for (int neighborId : runtime.getNodeModel().getNeighbors()) {
            Message trigger = new Message("SNAPSHOT_TRIGGER", runtime.getId(), "AB");
            runtime.sendMessageTo(neighborId, trigger);
        }
    }

    @Override
    public synchronized void handleMessage(Message message) {
        int senderId = message.senderId();

        switch (message.type()) {
            case "SNAPSHOT_TRIGGER" -> handleSnapshotTrigger(senderId);
            case "TRANSFER" -> handleTransfer(senderId, Integer.parseInt(message.content()));
            default -> log("Unknown message type received: " + message.type());
        }
    }

    /**
     * Handles an incoming SNAPSHOT_TRIGGER.
     * If this is the first trigger, initiates the snapshot and sends it to neighbors.
     * If not, simply records the sender and continues.
     *
     * @param senderId ID of the node that sent the trigger.
     */
    private void handleSnapshotTrigger(int senderId) {
        log("Received SNAPSHOT_TRIGGER from Node " + senderId);

        boolean firstTime = !recorded;

        if (firstTime) {
            initiate();
        } else {
            for (int neighborId : runtime.getNodeModel().getNeighbors()) {
                if (neighborId != senderId && !receivedFrom.contains(neighborId)) {
                    Message trigger = new Message("SNAPSHOT_TRIGGER", runtime.getId(), "AB");
                    runtime.sendMessageTo(neighborId, trigger);
                }
            }
        }

        receivedFrom.add(senderId);

        if (isSnapshotComplete(receivedFrom)) {
            log("Snapshot complete. Writing channel states...");
            writeChannelStatesToOutput();
            resetSnapshot();
        }
    }

    /**
     * Handles a TRANSFER message during the snapshot.
     * Buffers the amount if it arrives from a channel not yet recorded.
     *
     * @param senderId ID of the sender node.
     * @param amount   Amount of bitcakes received.
     */
    private void handleTransfer(int senderId, int amount) {
        if (recorded && !receivedFrom.contains(senderId)) {
            channelStates.merge(senderId, amount, Integer::sum);
            log("Buffered " + amount + " from Node " + senderId + " during snapshot");
        }
    }

    /**
     * Writes the recorded channel states to the output.
     */
    private void writeChannelStatesToOutput() {
        for (Map.Entry<Integer, Integer> entry : channelStates.entrySet()) {
            writeToOutput("CHANNEL_STATE from Node " + entry.getKey() + ": " + entry.getValue() + " bitcakes");
        }
    }

    /**
     * Resets the internal snapshot state.
     */
    private void resetSnapshot() {
        setSnapshotState(false);
        recorded = false;
        receivedFrom.clear();
        channelStates.clear();
    }

    /**
     * Checks whether the node is eligible to initiate a snapshot.
     *
     * @return True if a snapshot can be initiated, false otherwise.
     */
    private boolean canInitiateSnapshot() {
        return runtime.getNodeModel().getState() == NodeState.AVAILABLE && !recorded;
    }
}