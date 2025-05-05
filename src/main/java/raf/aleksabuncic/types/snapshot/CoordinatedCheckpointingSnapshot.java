package raf.aleksabuncic.types.snapshot;

import raf.aleksabuncic.core.NodeRuntime;
import raf.aleksabuncic.types.Message;

public class CoordinatedCheckpointingSnapshot extends Snapshot {
    public CoordinatedCheckpointingSnapshot(NodeRuntime runtime) {
        super(runtime);
    }

    @Override
    public void initiate() {

    }

    @Override
    public void handleMessage(Message message) {

    }
}
