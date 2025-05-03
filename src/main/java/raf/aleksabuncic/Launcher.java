
package raf.aleksabuncic;

import java.io.*;
import java.util.List;

public class Launcher {
    public static void main(String[] args) throws Exception {
        String os = System.getProperty("os.name").toLowerCase();
        String commandsFile;

        if (os.contains("win")) {
            commandsFile = "/instructions/commands_windows.txt";
        } else if (os.contains("mac")) {
            commandsFile = "/instructions/commands_mac.txt";
        } else {
            commandsFile = "/instructions/commands_linux.txt";
        }

        try (InputStream is = Launcher.class.getResourceAsStream(commandsFile)) {
            if (is == null) {
                throw new FileNotFoundException("Commands file not found in resources: " + commandsFile);
            }
            List<String> commands = new BufferedReader(new InputStreamReader(is))
                    .lines()
                    .toList();

            for (String command : commands) {
                System.out.println("Launching: " + command);
                new ProcessBuilder("cmd.exe", "/c", command).start();
            }
        }
    }
}