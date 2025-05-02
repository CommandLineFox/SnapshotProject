package raf.aleksabuncic.types;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;

@Getter
public class Node {
    private int id;
    private int port;
    private NodeState state;
    @Setter
    private int bitcake;
    private ArrayList<Integer> neighbors;

    public Node(int id, int port, int bitcake, ArrayList<Integer> neighbors) {
        this.id = id;
        this.port = port;
        this.state = NodeState.AVAILABLE;
        this.bitcake = bitcake;
        this.neighbors = neighbors;
    }
}