package raf.aleksabuncic.manual;

import raf.aleksabuncic.core.NodeRuntime;

import java.util.Scanner;

public class CliThread extends Thread {
    private final NodeRuntime runtime;

    public CliThread(NodeRuntime runtime) {
        this.runtime = runtime;
    }

    @Override
    public void run() {
        Scanner scanner = new Scanner(System.in);
        printHelp();

        while (true) {
            String input = scanner.nextLine().trim();
            if (input.isEmpty()) continue;

            String[] parts = input.split("\\s+");
            String command = parts[0].toLowerCase();

            switch (command) {
                case "send":
                    if (parts.length != 3) {
                        System.out.println("Usage: send <targetNodeId> <amount>");
                        break;
                    }
                    try {
                        int targetId = Integer.parseInt(parts[1]);
                        int amount = Integer.parseInt(parts[2]);
                        runtime.trySendBitcakes(targetId, amount);
                    } catch (NumberFormatException e) {
                        System.out.println("Invalid number format.");
                    }
                    break;

                case "snapshot":
                    try {
                        runtime.startSnapshot();
                        System.out.println("Snapshot initiated successfully.");
                    } catch (Exception e) {
                        System.out.println("Failed to initiate snapshot: " + e.getMessage());
                    }
                    break;

                case "print":
                    System.out.println("Bitcake balance: " + runtime.getBitcake());
                    break;

                case "exit":
                    System.out.println("Exiting...");
                    System.exit(0);
                    break;

                default:
                    System.out.println("Unknown command.");
                    printHelp();
            }
        }
    }

    /**
     * Prints available commands.
     */
    private void printHelp() {
        System.out.println("Available commands:");
        System.out.println("  send <targetNodeId> <amount> - Send bitcakes to neighbor");
        System.out.println("  snapshot - Initiate snapshot");
        System.out.println("  print - Print current bitcake state");
        System.out.println("  exit - Exit node");
    }
}