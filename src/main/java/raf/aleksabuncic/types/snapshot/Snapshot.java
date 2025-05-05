package raf.aleksabuncic.types.snapshot;

import raf.aleksabuncic.core.NodeRuntime;
import raf.aleksabuncic.types.Message;

public abstract class Snapshot {
    protected final NodeRuntime runtime;

    public Snapshot(NodeRuntime runtime) {
        this.runtime = runtime;
    }

    public abstract void initiate();

    public abstract void handleMessage(Message message);
}
