package org.miere.ui;

import org.miere.core.WatchDogEngine;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;

/**
 * Handles the system tray icon
 */
public class TrayController {
    // The actual tray icon object
    private static TrayIcon trayIcon;

    /**
     * Set up the system tray and menu because an invisible app is not visible
     */
    public static void setup() {
        // Stop if the OS doesn't support tray icons for some reason, so we can't do anything about that
        if (!SystemTray.isSupported()) return;

        // Create the right-click menu because buttons are uh, buttons useful
        PopupMenu popup = new PopupMenu();
        MenuItem manageItem = new MenuItem("Open Manager UI");
        MenuItem changePassItem = new MenuItem("Change Admin Password");
        MenuItem quitItem = new MenuItem("Quit WatchDog");

        // Open the control center because that's what the user clicked
        manageItem.addActionListener(e -> ControlCenter.open());

        // Open password change window because the user wants a new password LIKE WHY AM I EVEN EXPLAINING WTF
        changePassItem.addActionListener(e -> ControlCenter.changePassword());

        // Password-protected quit logic
        quitItem.addActionListener(e -> {
            if (ControlCenter.authenticate()) {
                // Stop the engine and exit
                WatchDogEngine.stop();
                System.exit(0);
            } else {
                // Flash an error because they got the password wrong lol stuped
                JOptionPane.showMessageDialog(null, "Quit Aborted: Unauthorized. 🛡️", "WatchDog", JOptionPane.ERROR_MESSAGE);
            }
        });

        // Add items to the menu so they actually show up
        popup.add(manageItem);
        popup.add(changePassItem);
        popup.addSeparator();
        popup.add(quitItem);

        // Create the tray icon with the dog pooping image lmao
        trayIcon = new TrayIcon(createIcon(), "WatchDog Manager", popup);
        trayIcon.setImageAutoSize(true);

        try {
            // Add to the system tray so the user can see it
            SystemTray.getSystemTray().add(trayIcon);
            pushNotification("Shields Up", "WatchDog is active and locked. 🛡️");
        } catch (Exception ignored) {
            // Something broke, but we're ignoring it because I used chatGPT and idk how to code tbh
        }
    }

    /**
     * Send a notification bubble because users like to know what's happening.
     */
    public static void pushNotification(String title, String msg) {
        if (trayIcon != null) trayIcon.displayMessage(title, msg, TrayIcon.MessageType.INFO);
    }

    /**
     * Load the pooping dog icon bhahbahahahahahahah
     */
    private static Image createIcon() {
        // Grab the image from the resources folder because it's bundled in the JAR [cite: 2026-03-08]
        return Toolkit.getDefaultToolkit().getImage(TrayController.class.getResource("/doggo_poopoo.png"));
    }
}