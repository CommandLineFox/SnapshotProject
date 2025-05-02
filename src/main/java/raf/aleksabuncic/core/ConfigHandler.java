package raf.aleksabuncic.core;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import raf.aleksabuncic.types.Node;

import java.io.File;
import java.util.*;

public class ConfigHandler {
    public static class ConfigResult {
        public String snapshotType;
        public Map<Integer, Node> allNodes = new HashMap<>();
    }

    public static ConfigResult load(String path) {
        ConfigResult result = new ConfigResult();
        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(new File(path));

            result.snapshotType = root.get("snapshotType").asText();

            for (JsonNode nodeEntry : root.get("nodeList")) {
                int id = nodeEntry.get("id").asInt();
                int port = nodeEntry.get("port").asInt();
                int bitcake = nodeEntry.get("bitcake").asInt();
                List<Integer> neighbors = new ArrayList<>();
                for (JsonNode n : nodeEntry.get("neighbors")) {
                    neighbors.add(n.asInt());
                }

                Node node = new Node(id, port, bitcake, new ArrayList<>(neighbors));
                result.allNodes.put(id, node);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return result;
    }
}