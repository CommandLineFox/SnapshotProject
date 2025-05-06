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
        writeNodeStateToOutput();

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
     * Handles a checkpoint request.
     *
     * @param message Message containing the initiator ID.
     */
    private void handleCheckpointRequest(Message message) {
        int initiatorId = Integer.parseInt(message.content());
        int senderId = message.senderId();

        log("Received CHECKPOINT_REQUEST from Node " + senderId + " for initiator " + initiatorId);

        if (!receivedRequests.contains(initiatorId)) {
            receivedRequests.add(initiatorId);

            runtime.getRequestSourceMap().putIfAbsent(initiatorId, senderId);

            setSnapshotState(true);
            writeNodeStateToOutput();

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
     * Handles a checkpoint acknowledgement.
     *
     * @param message Message containing the initiator ID.
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
     * Finalizes a snapshot instance.
     *
     * @param initiatorId ID of the snapshot instance to finalize.
     */
    private void finalizeSnapshot(int initiatorId) {
        if (!receivedRequests.contains(initiatorId)) {
            log("Cannot finalize unknown snapshot instance.");
            return;
        }

        writeNodeStateToOutput();
        setSnapshotState(false);

        log("Snapshot complete at Node " + getNodeId() + " for snapshot " + initiatorId);
    }
}