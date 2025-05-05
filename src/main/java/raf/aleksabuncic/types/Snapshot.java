package raf.aleksabuncic.types;

import raf.aleksabuncic.core.NodeRuntime;

public abstract class Snapshot {
    protected final NodeRuntime runtime;

    public Snapshot(NodeRuntime runtime) {
        this.runtime = runtime;
    }

    public abstract void initiate();

    public abstract void handleMessage(Message message);
}
