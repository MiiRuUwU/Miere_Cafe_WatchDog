package org.miere.core;

import org.json.JSONArray;
import org.json.JSONObject;
import java.io.*;
import java.nio.file.*;
import java.util.Map;

public class ConfigHandler {
    private static final String CONFIG_FILE = "config.json";
    private static final String VAULT_FILE = ".sys_vault.dat";
    private static final String PASS_FILE = ".sys_pass.dat";
    private static RandomAccessFile configBouncer, vaultBouncer, passBouncer;
    private static String storedPasswordHash = "";

    public static void initAndLoad() {
        try {
            if (!new File(CONFIG_FILE).exists() && new File(VAULT_FILE).exists()) {
                Files.copy(Paths.get(VAULT_FILE), Paths.get(CONFIG_FILE));
            }
            // Load the hash or set default
            File passFile = new File(PASS_FILE);
            if (!passFile.exists()) {
                storedPasswordHash = org.miere.security.SecurityManager.hashPassword("cafe123");
                saveState(WatchDogEngine.getTargetMap(), storedPasswordHash);
            } else {
                storedPasswordHash = Files.readString(Paths.get(PASS_FILE)).trim();
            }
            loadConfig();
            applyFileLocks();
        } catch (Exception ignored) {}
    }

    private static void loadConfig() throws IOException {
        if (!new File(CONFIG_FILE).exists()) return;
        String content = Files.readString(Paths.get(CONFIG_FILE));
        JSONObject json = new JSONObject(content);
        JSONArray array = json.getJSONArray("targets");

        WatchDogEngine.getTargetMap().clear();
        for (int i = 0; i < array.length(); i++) {
            JSONObject obj = array.getJSONObject(i);
            String path = obj.getString("exe_path");
            int delay = obj.getInt("delay_ms");
            // Use optBoolean so old configs don't crash the app
            boolean proactive = obj.optBoolean("is_proactive", false);
            WatchDogEngine.getTargetMap().put(path, new Target(path, delay, proactive));
        }
    }

    public static void saveState(Map<String, Target> targets, String newHash) throws IOException {
        releaseLocks();
        storedPasswordHash = newHash;
        JSONObject json = new JSONObject();
        JSONArray array = new JSONArray();

        targets.forEach((path, target) -> {
            JSONObject obj = new JSONObject();
            obj.put("exe_path", target.path);
            obj.put("delay_ms", (int)target.intervalMs);
            obj.put("is_proactive", target.isProactive);
            array.put(obj);
        });

        json.put("targets", array);
        Files.writeString(Paths.get(CONFIG_FILE), json.toString(4));
        Files.writeString(Paths.get(VAULT_FILE), json.toString(4));
        Files.writeString(Paths.get(PASS_FILE), storedPasswordHash);
        applyFileLocks();
    }

    private static void applyFileLocks() {
        try {
            configBouncer = new RandomAccessFile(CONFIG_FILE, "rw");
            vaultBouncer = new RandomAccessFile(VAULT_FILE, "rw");
            passBouncer = new RandomAccessFile(PASS_FILE, "rw");
            Files.setAttribute(Paths.get(VAULT_FILE), "dos:hidden", true);
            Files.setAttribute(Paths.get(PASS_FILE), "dos:hidden", true);
        } catch (Exception ignored) {}
    }

    public static void releaseLocks() {
        try {
            if (configBouncer != null) configBouncer.close();
            if (vaultBouncer != null) vaultBouncer.close();
            if (passBouncer != null) passBouncer.close();
        } catch (Exception ignored) {}
    }

    public static String getStoredPasswordHash() { return storedPasswordHash; }
}