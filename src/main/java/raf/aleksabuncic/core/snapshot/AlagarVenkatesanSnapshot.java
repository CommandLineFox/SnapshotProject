package raf.aleksabuncic.core.snapshot;

import raf.aleksabuncic.core.NodeRuntime;
import raf.aleksabuncic.types.Message;
import raf.aleksabuncic.types.Snapshot;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class AlagarVenkatesanSnapshot extends Snapshot {
    private boolean recorded = false;
    private int recordedBitcake;
    private final Set<Integer> neighbors = new HashSet<>();
    private final Map<Integer, Integer> channelStates = new HashMap<>();

    public AlagarVenkatesanSnapshot(NodeRuntime runtime) {
        super(runtime);
        this.neighbors.addAll(runtime.getNodeModel().getNeighbors());
    }

    @Override
    public synchronized void initiate() {
        if (recorded) return;

        log("Initiating Alagar-Venkatesan snapshot...");
        recorded = true;
        setSnapshotState(true);

        recordedBitcake = getBitcake();
        writeToOutput("SNAPSHOT " + getNodeId() + ": RECORDED " + recordedBitcake);

        sendMarkerToAllNeighbors("AV_MARKER");
    }

    @Override
    public synchronized void handleMessage(Message message) {
        int senderId = message.senderId();

        switch (message.type()) {
            case "AV_MARKER" -> {
                log("Received AV_MARKER from Node " + senderId);
                neighbors.remove(senderId);

                if (!recorded) initiate();

                if (neighbors.isEmpty()) {
                    writeToOutput("SNAPSHOT " + getNodeId() + ": COMPLETE " +
                            recordedBitcake + " IN_CHANNELS=" + channelStates);
                    log("Snapshot complete.");
                    setSnapshotState(false);
                }
            }
            case "TRANSFER" -> {
                int amount = Integer.parseInt(message.content());
                if (recorded && neighbors.contains(senderId)) {
                    channelStates.merge(senderId, amount, Integer::sum);
                    log("Buffered " + amount + " from Node " + senderId + " during snapshot");
                }
            }
        }
    }
}