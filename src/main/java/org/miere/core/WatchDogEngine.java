package org.miere.core;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The brain of the app, now with 100% less accidental explosions.
 */
public class WatchDogEngine {
    private static final Map<String, Integer> targetMap = new ConcurrentHashMap<>();
    private static final Map<String, Process> activeProcesses = new ConcurrentHashMap<>();
    private static volatile boolean keepRunning = true;

    public static void start() {
        new Thread(() -> {
            while (keepRunning) {
                for (String path : targetMap.keySet()) {
                    // 1. Get just the filename (e.g., "Timer.exe")
                    String fileName = new File(path).getName();

                    // 2. Ask Windows if this specific .exe is already in the Task Manager
                    if (isAlreadyRunning(fileName)) {
                        continue; // It's already there, leave it alone
                    }

                    // 3. If Windows says it's NOT running, then we spawn it
                    try {
                        activeProcesses.put(path, new ProcessBuilder(path).start());
                    } catch (Exception ignored) {}
                }

                try {
                    // Use the custom delay from the map or default to 2s
                    Thread.sleep(2000);
                } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
            }
        }).start();
    }

    /**
     * Checks the Windows Tasklist so we don't spawn 500 copies of an app.
     */
    private static boolean isAlreadyRunning(String exeName) {
        try {
            ProcessBuilder pb = new ProcessBuilder("tasklist", "/FI", "IMAGENAME eq " + exeName);
            Process checkProcess = pb.start();
            BufferedReader reader = new BufferedReader(new InputStreamReader(checkProcess.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.contains(exeName)) return true; // Found it!
            }
        } catch (Exception e) {
            return false; // If the check fails, assume it's not running
        }
        return false;
    }

    public static void stop() { keepRunning = false; }
    public static Map<String, Integer> getTargetMap() { return targetMap; }
}