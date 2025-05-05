package raf.aleksabuncic.core;

import raf.aleksabuncic.types.Message;
import raf.aleksabuncic.types.Snapshot;

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
