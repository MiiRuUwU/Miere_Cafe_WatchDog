package org.miere.core;

import java.io.File;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class WatchDogEngine {
    private static final Map<String, Target> targetMap = new ConcurrentHashMap<>();
    private static volatile boolean keepRunning = true;

    public static void start() {
        new Thread(() -> {
            while (keepRunning) {
                long now = System.currentTimeMillis();
                for (Target t : targetMap.values()) {
                    String fileName = new File(t.path).getName();
                    boolean isRunning = isAlreadyRunning(fileName);

                    // Immediate restart if closed
                    if (!isRunning) {
                        spawn(t.path);
                        t.lastActionTime = now; // Reset pulse timer on manual crash/close
                    }
                    //THE PULSE: Redundant restart every interval if enabled
                    else if (t.isProactive && (now - t.lastActionTime >= t.intervalMs)) {
                        spawn(t.path);
                        t.lastActionTime = now;
                    }
                }
                try { Thread.sleep(500); } // Fast check
                catch (InterruptedException e) { Thread.currentThread().interrupt(); }
            }
        }).start();
    }

    private static void spawn(String path) {
        try { new ProcessBuilder(path).start(); } catch (Exception ignored) {}
    }

    // Standard tasklist check
    private static boolean isAlreadyRunning(String exeName) {
        try {
            // Using a filter to keep it lightweight
            ProcessBuilder pb = new ProcessBuilder("tasklist", "/FI", "IMAGENAME eq " + exeName);
            java.util.Scanner s = new java.util.Scanner(pb.start().getInputStream());
            while (s.hasNextLine()) {
                if (s.nextLine().toLowerCase().contains(exeName.toLowerCase())) return true;
            }
        } catch (Exception e) {
            return false; // Assume not running if check fails
        }
        return false;
    }

    public static Map<String, Target> getTargetMap() { return targetMap; }
    public static void stop() { keepRunning = false; }
}