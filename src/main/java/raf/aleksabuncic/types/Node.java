package raf.aleksabuncic.types;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;

@Getter
public class Node {
    private final int id;
    private final int port;
    @Setter
    private NodeState state;
    private final ArrayList<Integer> neighbors;
    @Setter
    private int bitcake;

    public Node(int id, int port, int bitcake, ArrayList<Integer> neighbors) {
        this.id = id;
        this.port = port;
        this.state = NodeState.AVAILABLE;
        this.bitcake = bitcake;
        this.neighbors = neighbors;
    }

    /**
     * Check if the node is available
     *
     * @return True if available, false if not
     */
    public boolean isAvailable() {
        return this.state == NodeState.AVAILABLE;
    }

}