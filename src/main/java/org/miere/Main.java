package org.miere;

import org.miere.core.ConfigHandler;
import org.miere.core.WatchDogEngine;
import org.miere.security.SecurityManager;
import org.miere.ui.TrayController;
import javax.swing.*;
import java.io.File;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Main entry point because the program has to start somewhere.
 */
public class Main {
    // Lock objects so the file system doesn't scream at us
    private static FileLock lock;
    private static FileChannel lockChannel;
    private static final String LOCK_FILE = ".watchdog.lock";

    /**
     * Start the app and initialize everything so it actually works.
     */
    public static void main(String[] args) {
        // Stop if it's already running because we don't need two of these
        if (!enforceSingleInstance()) {
            JOptionPane.showMessageDialog(null, "WatchDog is already running! Check your tray. 🛡️", "Active", JOptionPane.WARNING_MESSAGE);
            System.exit(0);
        }

        // Initialize security context because we need passwords to work
        SecurityManager.init();

        // Load settings and lock files so users can't delete them easily
        ConfigHandler.initAndLoad();

        // Start background monitoring so the target apps stay alive
        WatchDogEngine.start();

        // Initialize System Tray because users like clicking on icons
        TrayController.setup();
    }

    /**
     * Lock a file to make sure only one instance is running at a time.
     */
    private static boolean enforceSingleInstance() {
        try {
            File lockFile = new File(LOCK_FILE);
            lockChannel = new RandomAccessFile(lockFile, "rw").getChannel();
            lock = lockChannel.tryLock();

            if (lock != null) {
                // Hide the lock file so the desktop stays clean
                Path lockPath = Paths.get(LOCK_FILE);
                Files.setAttribute(lockPath, "dos:hidden", true);

                // Delete the file when we're done because we're not messy
                lockFile.deleteOnExit();
                return true;
            }
            return false;
        } catch (Exception e) {
            // Something went wrong, just return false and give up
            return false;
        }
    }
}