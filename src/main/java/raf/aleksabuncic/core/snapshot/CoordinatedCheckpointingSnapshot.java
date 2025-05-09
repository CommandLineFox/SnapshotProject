package raf.aleksabuncic.core.snapshot;

import raf.aleksabuncic.core.NodeRuntime;
import raf.aleksabuncic.types.Message;
import raf.aleksabuncic.types.Snapshot;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class CoordinatedCheckpointingSnapshot extends Snapshot {
    private final Set<Integer> receivedRequests = new HashSet<>();
    private final Map<Integer, Set<Integer>> acksReceivedPerInitiator = new HashMap<>();
    private final Map<Integer, Set<Integer>> requestsSentPerInitiator = new HashMap<>();
    private final Set<Integer> stateRecordedForInitiator = new HashSet<>(); // NEW

    public CoordinatedCheckpointingSnapshot(NodeRuntime runtime) {
        super(runtime);
    }

    @Override
    public synchronized void initiate() {
        int initiatorId = getNodeId();
        if (receivedRequests.contains(initiatorId)) {
            log("Already initiated snapshot with this ID. Ignoring.");
            return;
        }

        log("Initiating Coordinated Checkpoint...");

        receivedRequests.add(initiatorId);
        setSnapshotState(true);

        // Write state only once per initiator
        if (!stateRecordedForInitiator.contains(initiatorId)) {
            writeNodeStateToOutput();
            stateRecordedForInitiator.add(initiatorId);
        }

        Set<Integer> neighbors = new HashSet<>(runtime.getNodeModel().getNeighbors());
        requestsSentPerInitiator.put(initiatorId, neighbors);
        acksReceivedPerInitiator.put(initiatorId, new HashSet<>());

        for (int neighborId : neighbors) {
            runtime.sendMessageTo(neighborId, new Message("CHECKPOINT_REQUEST", getNodeId(), String.valueOf(initiatorId)));
        }

        if (neighbors.isEmpty()) {
            finalizeSnapshot(initiatorId);
        }
    }

    @Override
    public synchronized void handleMessage(Message message) {
        switch (message.type()) {
            case "CHECKPOINT_REQUEST" -> handleCheckpointRequest(message);
            case "CHECKPOINT_ACK" -> handleCheckpointAck(message);
            case "TRANSFER" -> {
                int amount = Integer.parseInt(message.content());
                log("Received TRANSFER of " + amount + " from Node " + message.senderId());
            }
        }
    }

    /**
     * Handle receiving checkpoint request
     *
     * @param message Message to handle
     */
    private void handleCheckpointRequest(Message message) {
        int initiatorId = Integer.parseInt(message.content());
        int senderId = message.senderId();

        log("Received CHECKPOINT_REQUEST from Node " + senderId + " for initiator " + initiatorId);

        if (!receivedRequests.contains(initiatorId)) {
            receivedRequests.add(initiatorId);
            runtime.getRequestSourceMap().putIfAbsent(initiatorId, senderId);

            setSnapshotState(true);

            if (!stateRecordedForInitiator.contains(initiatorId)) {
                writeNodeStateToOutput();
                stateRecordedForInitiator.add(initiatorId);
            }

            Set<Integer> neighborsToNotify = new HashSet<>();
            for (int neighborId : runtime.getNodeModel().getNeighbors()) {
                if (neighborId != senderId) {
                    runtime.sendMessageTo(neighborId, new Message("CHECKPOINT_REQUEST", getNodeId(), String.valueOf(initiatorId)));
                    neighborsToNotify.add(neighborId);
                }
            }

            requestsSentPerInitiator.put(initiatorId, neighborsToNotify);
            acksReceivedPerInitiator.put(initiatorId, new HashSet<>());

            if (neighborsToNotify.isEmpty()) {
                runtime.sendMessageTo(senderId, new Message("CHECKPOINT_ACK", getNodeId(), String.valueOf(initiatorId)));
            }
        } else {
            log("Already processed this snapshot instance. Ignoring...");
        }
    }

    /**
     * Handle receiving checkpoint acknowledgement
     *
     * @param message Message to handle
     */
    private void handleCheckpointAck(Message message) {
        int initiatorId = Integer.parseInt(message.content());
        int senderId = message.senderId();

        log("Received CHECKPOINT_ACK from Node " + senderId + " for initiator " + initiatorId);

        Set<Integer> receivedAcks = acksReceivedPerInitiator.get(initiatorId);
        if (receivedAcks == null) {
            log("Unexpected ACK for unknown snapshot instance. Ignoring...");
            return;
        }

        receivedAcks.add(senderId);

        Set<Integer> expectedAcks = requestsSentPerInitiator.getOrDefault(initiatorId, Set.of());
        if (!receivedAcks.containsAll(expectedAcks)) {
            return;
        }

        finalizeSnapshot(initiatorId);

        if (getNodeId() != initiatorId) {
            Integer sourceNode = runtime.getRequestSourceMap().get(initiatorId);
            if (sourceNode != null) {
                runtime.sendMessageTo(sourceNode, new Message("CHECKPOINT_ACK", getNodeId(), String.valueOf(initiatorId)));
            } else {
                log("No route to initiator " + initiatorId + " for ACK");
            }
        }
    }

    /**
     * Completing of snapshot
     *
     * @param initiatorId ID of the initiator
     */
    private void finalizeSnapshot(int initiatorId) {
        if (!receivedRequests.contains(initiatorId)) {
            log("Cannot finalize unknown snapshot instance.");
            return;
        }

        log("Snapshot complete at Node " + getNodeId() + " for snapshot " + initiatorId);
        setSnapshotState(false);
    }
}
