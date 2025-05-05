package raf.aleksabuncic;

import raf.aleksabuncic.cli.CliThread;
import raf.aleksabuncic.core.ConfigHandler;
import raf.aleksabuncic.core.NodeRuntime;
import raf.aleksabuncic.types.Node;
import raf.aleksabuncic.types.snapshot.AcharyaBadrinathSnapshot;
import raf.aleksabuncic.types.snapshot.AlagarVenkatesanSnapshot;
import raf.aleksabuncic.types.snapshot.CoordinatedCheckpointingSnapshot;

import java.util.HashMap;
import java.util.Map;

public class Starter {
    public static void main(String[] args) {
        if (args.length < 2) {
            System.out.println("Usage: java Starter <nodeId> <pathToConfig>");
            return;
        }

        int nodeId = Integer.parseInt(args[0]);
        String configPath = args[1];

        ConfigHandler.ConfigResult config = ConfigHandler.load(configPath);
        Node selfNode = config.allNodes.get(nodeId);

        if (selfNode == null) {
            System.out.println("No node with id " + nodeId + " found in config.");
            return;
        }

        Map<Integer, Integer> neighborPortMap = new HashMap<>();
        for (int neighborId : selfNode.getNeighbors()) {
            Node neighbor = config.allNodes.get(neighborId);
            if (neighbor != null) {
                neighborPortMap.put(neighborId, neighbor.getPort());
            }
        }

        NodeRuntime runtime = new NodeRuntime(selfNode, neighborPortMap);

        switch (config.snapshotType) {
            case "acharya":
                runtime.setSnapshot(new AcharyaBadrinathSnapshot(runtime));
                break;
            case "alagar":
                runtime.setSnapshot(new AlagarVenkatesanSnapshot(runtime));
                break;
            case "checkpoint":
                runtime.setSnapshot(new CoordinatedCheckpointingSnapshot(runtime));
                break;
            default:
                System.out.println("Unknown snapshot type.");
        }

        runtime.start();

        CliThread cliThread = new CliThread(runtime);
        cliThread.start();
    }
}