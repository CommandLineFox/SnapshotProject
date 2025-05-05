package raf.aleksabuncic.core.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import raf.aleksabuncic.types.Node;

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.*;

public class ConfigHandler {
    public static class ConfigResult {
        public String snapshotType;
        public Map<Integer, Node> allNodes = new HashMap<>();
    }

    /**
     * Loads configuration from a JSON file.
     *
     * @param resourcePath Path to JSON file.
     * @return ConfigResult object.
     */
    public static ConfigResult load(String resourcePath) {
        ConfigResult result = new ConfigResult();
        try {
            ObjectMapper mapper = new ObjectMapper();

            InputStream is = ConfigHandler.class.getClassLoader().getResourceAsStream(resourcePath);
            if (is == null) {
                throw new FileNotFoundException("Resource not found: " + resourcePath);
            }

            JsonNode root = mapper.readTree(is);
            result.snapshotType = root.get("snapshotType").asText();

            for (JsonNode nodeEntry : root.get("nodeList")) {
                int id = nodeEntry.get("id").asInt();
                int port = nodeEntry.get("port").asInt();
                int bitcake = nodeEntry.get("bitcake").asInt();
                ArrayList<Integer> neighbors = new ArrayList<>();
                for (JsonNode n : nodeEntry.get("neighbors")) {
                    neighbors.add(n.asInt());
                }

                Node node = new Node(id, port, bitcake, neighbors);
                result.allNodes.put(id, node);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return result;
    }
}