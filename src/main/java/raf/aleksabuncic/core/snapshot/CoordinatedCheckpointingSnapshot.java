package raf.aleksabuncic.core.snapshot;

import raf.aleksabuncic.core.NodeRuntime;
import raf.aleksabuncic.types.Message;
import raf.aleksabuncic.types.NodeState;
import raf.aleksabuncic.types.Snapshot;

import java.util.HashSet;
import java.util.Set;

public class CoordinatedCheckpointingSnapshot extends Snapshot {
    private boolean waitingForAcks = false;
    private final Set<Integer> acksReceived = new HashSet<>();

    public CoordinatedCheckpointingSnapshot(NodeRuntime runtime) {
        super(runtime);
    }

    @Override
    public synchronized void initiate() {
        if (waitingForAcks) return;

        log("Initiating Coordinated Checkpoint...");

        runtime.getNodeModel().setState(NodeState.SNAPSHOT);
        writeToOutput("SNAPSHOT " + getNodeId() + ": CHECKPOINT_STARTED");

        for (int neighborId : runtime.getNodeModel().getNeighbors()) {
            runtime.sendMessageTo(neighborId, new Message("CHECKPOINT_REQUEST", getNodeId(), ""));
        }

        waitingForAcks = true;
    }

    @Override
    public synchronized void handleMessage(Message message) {
        switch (message.type()) {
            case "CHECKPOINT_REQUEST" -> {
                log("Received CHECKPOINT_REQUEST from Node " + message.senderId());
                runtime.getNodeModel().setState(NodeState.SNAPSHOT);
                writeToOutput("SNAPSHOT " + getNodeId() + ": CHECKPOINT_ACCEPTED from " + message.senderId());
                runtime.sendMessageTo(message.senderId(), new Message("CHECKPOINT_ACK", getNodeId(), ""));
            }
            case "CHECKPOINT_ACK" -> {
                log("Received CHECKPOINT_ACK from Node " + message.senderId());
                acksReceived.add(message.senderId());

                if (acksReceived.containsAll(runtime.getNodeModel().getNeighbors())) {
                    writeToOutput("SNAPSHOT " + getNodeId() + ": CHECKPOINT_COMPLETE");
                    runtime.getNodeModel().setState(NodeState.AVAILABLE);
                    waitingForAcks = false;
                }
            }
            case "TRANSFER" -> {
                int amount = Integer.parseInt(message.content());
                log("Received TRANSFER of " + amount + " from Node " + message.senderId());
            }
        }
    }
}