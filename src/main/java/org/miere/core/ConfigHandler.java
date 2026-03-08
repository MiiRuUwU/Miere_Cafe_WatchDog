package org.miere.core;

import org.json.JSONArray;
import org.json.JSONObject;
import org.miere.security.SecurityManager;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Map;

/**
 * Handle files
 */
public class ConfigHandler {
    // Filenames for the stuff we're hiding from customers [cite: 2026-03-08]
    private static final String CONFIG_FILE = "config.json";
    private static final String VAULT_FILE = ".sys_vault.dat";
    private static final String PASS_FILE = ".sys_pass.dat";

    // RandomAccessFiles acting as bouncers to stop people from deleting our stuff [cite: 2026-03-08]
    private static RandomAccessFile configBouncer, vaultBouncer, passBouncer;
    private static String storedPasswordHash = "";

    /**
     * Set up the files and load everything so the app isn't amnesiac.
     */
    public static void initAndLoad() {
        try {
            File configFile = new File(CONFIG_FILE);
            File passFile = new File(PASS_FILE);

            // Sync Vault to Config if needed because users like to delete visible files and I fucking HATE THEM IOAETGKJAGIEGLwer;atjoGiwkg
            if (!configFile.exists() && new File(VAULT_FILE).exists()) {
                Files.copy(Paths.get(VAULT_FILE), Paths.get(CONFIG_FILE));
            }

            // Initial setup for new users (Default: cafe123) ez password hack that shi lololol
            if (!passFile.exists()) {
                storedPasswordHash = SecurityManager.hashPassword("cafe123");
                saveState(WatchDogEngine.getTargetMap(), storedPasswordHash);
            } else {
                // Read the hash
                storedPasswordHash = Files.readString(Paths.get(PASS_FILE)).trim();
            }

            loadConfig();
            applyFileLocks();
        } catch (Exception ignored) {
            // it crashed lol good luck
        }
    }

    /**
     * Read the JSON
     */
    private static void loadConfig() throws IOException {
        if (!new File(CONFIG_FILE).exists()) return;

        String content = Files.readString(Paths.get(CONFIG_FILE));
        JSONObject json = new JSONObject(content);
        JSONArray array = json.getJSONArray("targets");

        // Clear the map and refill it so no duplicates
        WatchDogEngine.getTargetMap().clear();
        for (int i = 0; i < array.length(); i++) {
            JSONObject obj = array.getJSONObject(i);
            WatchDogEngine.getTargetMap().put(obj.getString("exe_path"), obj.getInt("delay_ms"));
        }
    }

    /**
     * Save everything to three different places because we don't trust the user.
     */
    public static void saveState(Map<String, Integer> targets, String newHash) throws IOException {
        // Unlock files so we can actually write to them
        releaseLocks();
        storedPasswordHash = newHash;

        JSONObject json = new JSONObject();
        JSONArray array = new JSONArray();

        // Convert the map to JSON because that's the format we chose
        targets.forEach((path, delay) -> {
            JSONObject obj = new JSONObject();
            obj.put("exe_path", path);
            obj.put("delay_ms", delay);
            array.put(obj);
        });

        json.put("targets", array);

        // Write to all files so we have backups
        Files.writeString(Paths.get(CONFIG_FILE), json.toString(4));
        Files.writeString(Paths.get(VAULT_FILE), json.toString(4));
        Files.writeString(Paths.get(PASS_FILE), storedPasswordHash);

        // Lock them back up immediately
        applyFileLocks();
    }

    /**
     * Lock the files and hide them so the computer shop customers stay confused.
     */
    private static void applyFileLocks() {
        try {
            // Re-open bouncers to prevent external deletion by the OS or users
            configBouncer = new RandomAccessFile(CONFIG_FILE, "rw");
            vaultBouncer = new RandomAccessFile(VAULT_FILE, "rw");
            passBouncer = new RandomAccessFile(PASS_FILE, "rw");

            // Make them hidden because out of sight, out of mind
            Files.setAttribute(Paths.get(VAULT_FILE), "dos:hidden", true);
            Files.setAttribute(Paths.get(PASS_FILE), "dos:hidden", true);
        } catch (Exception ignored) {}
    }

    /**
     * Close the bouncers so we don't get 'File in use' errors when saving.
     */
    public static void releaseLocks() {
        try {
            if (configBouncer != null) configBouncer.close();
            if (vaultBouncer != null) vaultBouncer.close();
            if (passBouncer != null) passBouncer.close();
        } catch (Exception ignored) {}
    }

    public static String getStoredPasswordHash() { return storedPasswordHash; }
}