package raf.aleksabuncic.core.snapshot;

import raf.aleksabuncic.core.NodeRuntime;
import raf.aleksabuncic.types.Message;
import raf.aleksabuncic.types.Snapshot;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class AcharyaBadrinathSnapshot extends Snapshot {
    private boolean recorded = false;
    private int recordedBitcake;
    private final Map<Integer, Integer> channelStates = new HashMap<>();
    private final Set<Integer> receivedMarkers = new HashSet<>();

    public AcharyaBadrinathSnapshot(NodeRuntime runtime) {
        super(runtime);
    }

    @Override
    public synchronized void initiate() {
        if (recorded) return;

        log("Initiating Acharya-Badrinath snapshot...");
        recorded = true;
        setSnapshotState(true);

        recordedBitcake = getBitcake();
        writeToOutput("SNAPSHOT " + getNodeId() + ": RECORDED " + recordedBitcake);

        sendMarkerToAllNeighbors("SNAPSHOT_MARKER");
    }

    @Override
    public synchronized void handleMessage(Message message) {
        int senderId = message.senderId();

        switch (message.type()) {
            case "SNAPSHOT_MARKER" -> {
                log("Received MARKER from Node " + senderId);
                if (!recorded) initiate();
                receivedMarkers.add(senderId);

                if (isSnapshotComplete(receivedMarkers)) {
                    log("Snapshot complete. Final state:");
                    writeToOutput("SNAPSHOT " + getNodeId() + ": COMPLETE " +
                            recordedBitcake + " IN_CHANNELS=" + channelStates);
                    setSnapshotState(false);
                }
            }
            case "TRANSFER" -> {
                int amount = Integer.parseInt(message.content());
                if (recorded && !receivedMarkers.contains(senderId)) {
                    channelStates.merge(senderId, amount, Integer::sum);
                    log("Buffered " + amount + " from Node " + senderId + " during snapshot");
                }
            }
        }
    }
}