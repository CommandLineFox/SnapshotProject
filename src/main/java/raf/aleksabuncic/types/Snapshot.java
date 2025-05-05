package raf.aleksabuncic.types;

import raf.aleksabuncic.core.NodeRuntime;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.concurrent.locks.ReentrantLock;

public abstract class Snapshot {
    protected final NodeRuntime runtime;
    private static final ReentrantLock fileLock = new ReentrantLock(); // globalno za output.txt

    public Snapshot(NodeRuntime runtime) {
        this.runtime = runtime;
    }

    /**
     * Initializes snapshot.
     */
    public abstract void initiate();

    /**
     * Handles a snapshot-related message
     *
     * @param message Message to handle.
     */
    public abstract void handleMessage(Message message);

    /**
     * Logging into an output file
     *
     * @param line Line to log
     */
    protected void writeToOutput(String line) {
        fileLock.lock();
        try (PrintWriter writer = new PrintWriter(new FileWriter("output.txt", true))) {
            writer.println(line);
        } catch (IOException e) {
            System.err.println("Failed to write to output.txt: " + e.getMessage());
        } finally {
            fileLock.unlock();
        }
    }

    /**
     * Logs a message.
     *
     * @param msg Message to log.
     */
    protected void log(String msg) {
        System.out.println("[Snapshot@Node " + runtime.getId() + "] " + msg);
    }

    /**
     * Returns the current bitcake balance.
     *
     * @return Bitcake balance.
     */
    protected int getBitcake() {
        return runtime.getBitcake();
    }

    /**
     * Returns the ID of the node.
     *
     * @return Node ID.
     */
    protected int getNodeId() {
        return runtime.getId();
    }

    /**
     * Logs a message and buffers it for later sending.
     *
     * @param message Message to buffer.
     */
    protected void bufferMessage(Message message) {
        log("Buffered message: " + message);
    }
}
