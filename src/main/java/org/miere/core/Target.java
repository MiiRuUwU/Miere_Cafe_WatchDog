package org.miere.core;

public class Target {
    public String path;
    public long intervalMs;
    public boolean isProactive;
    public long lastActionTime;

    public Target(String path, long intervalSeconds, boolean isProactive) {
        this.path = path;
        // BUGFIX: Floor at 30s, but allow anything higher
        long seconds = Math.max(intervalSeconds, 30);
        this.intervalMs = seconds * 1000;
        this.isProactive = isProactive;
        this.lastActionTime = System.currentTimeMillis();
    }
}