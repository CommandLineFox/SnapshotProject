package raf.aleksabuncic.automatic;

import raf.aleksabuncic.core.*;
import raf.aleksabuncic.core.handler.ConfigHandler;
import raf.aleksabuncic.core.snapshot.AcharyaBadrinathSnapshot;
import raf.aleksabuncic.core.snapshot.AlagarVenkatesanSnapshot;
import raf.aleksabuncic.core.snapshot.CoordinatedCheckpointingSnapshot;
import raf.aleksabuncic.types.Node;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AutoLauncher {
    private final Map<Integer, NodeRuntime> runtimes = new HashMap<>();

    public static void main(String[] args) throws Exception {
        new AutoLauncher().launch("instructions/config.json", "instructions/instructions.txt");
    }

    /**
     * Launches the nodes and executes the instructions.
     *
     * @param configPath       Path to the configuration file.
     * @param instructionsPath Path to the instructions file.
     * @throws Exception If an error occurs during the launch or parsing of the instructions.
     */
    public void launch(String configPath, String instructionsPath) throws Exception {
        ConfigHandler.ConfigResult config = ConfigHandler.load(configPath);

        for (Node node : config.allNodes.values()) {
            Map<Integer, Integer> neighborPorts = new HashMap<>();
            for (int neighborId : node.getNeighbors()) {
                Node neighbor = config.allNodes.get(neighborId);
                if (neighbor != null) {
                    neighborPorts.put(neighborId, neighbor.getPort());
                }
            }

            NodeRuntime runtime = new NodeRuntime(node, neighborPorts);

            switch (config.snapshotType.toLowerCase()) {
                case "ab" -> runtime.setSnapshot(new AcharyaBadrinathSnapshot(runtime));
                case "av" -> runtime.setSnapshot(new AlagarVenkatesanSnapshot(runtime));
                case "kc" -> runtime.setSnapshot(new CoordinatedCheckpointingSnapshot(runtime));
                default -> throw new IllegalArgumentException("Unknown snapshot type: " + config.snapshotType);
            }

            runtime.start();
            runtimes.put(node.getId(), runtime);
        }

        Thread.sleep(1000);

        List<NodeCommand> commands = parseInstructions(instructionsPath);
        for (NodeCommand cmd : commands) {
            if (cmd instanceof SendCommand send) {
                runtimes.get(send.from()).trySendBitcakes(send.to(), send.amount());
            } else if (cmd instanceof SnapshotCommand snap) {
                runtimes.get(snap.initiator()).startSnapshot();
            }
            Thread.sleep(500);
        }
    }

    public sealed interface NodeCommand permits SendCommand, SnapshotCommand {
    }

    public record SendCommand(int from, int to, int amount) implements NodeCommand {
    }

    public record SnapshotCommand(int initiator) implements NodeCommand {
    }

    /**
     * Parses the instructions file and returns a list of commands.
     *
     * @param resourcePath Path to the instructions file.
     * @return List of commands.
     * @throws IOException If an error occurs during the parsing of the instructions.
     */
    public static List<NodeCommand> parseInstructions(String resourcePath) throws IOException {
        List<NodeCommand> commands = new ArrayList<>();
        InputStream is = AutoLauncher.class.getClassLoader().getResourceAsStream(resourcePath);
        if (is == null) throw new FileNotFoundException("Could not find: " + resourcePath);

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(is))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.trim().split("\\s+");
                if (parts.length == 0) continue;
                switch (parts[0].toLowerCase()) {
                    case "send" -> {
                        if (parts.length != 4) continue;
                        int from = Integer.parseInt(parts[1]);
                        int to = Integer.parseInt(parts[2]);
                        int amount = Integer.parseInt(parts[3]);
                        commands.add(new SendCommand(from, to, amount));
                    }
                    case "snapshot" -> {
                        if (parts.length != 2) continue;
                        int initiator = Integer.parseInt(parts[1]);
                        commands.add(new SnapshotCommand(initiator));
                    }
                }
            }
        }
        return commands;
    }
}