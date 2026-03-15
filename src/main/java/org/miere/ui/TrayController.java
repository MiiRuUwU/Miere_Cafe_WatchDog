package org.miere.ui;

import java.awt.*;

public class TrayController {
    public static void setup() {
        if (!SystemTray.isSupported()) return;

        PopupMenu popup = new PopupMenu();

        //Live Dashboard
        MenuItem statusItem = new MenuItem("View Active Shields (Dashboard)");
        statusItem.addActionListener(e -> StatusWindow.open());
        popup.add(statusItem);

        popup.addSeparator();

        //Password Change Functionality
        MenuItem changePassItem = new MenuItem("Change Admin Password");
        changePassItem.addActionListener(e -> ControlCenter.changePassword());
        popup.add(changePassItem);

        //Configuration
        MenuItem manageItem = new MenuItem("Configure Targets");
        manageItem.addActionListener(e -> ControlCenter.open());
        popup.add(manageItem);

        popup.addSeparator();

        //Secure Quit
        MenuItem quitItem = new MenuItem("Quit WatchDog");
        quitItem.addActionListener(e -> {
            if (ControlCenter.authenticate()) System.exit(0);
        });
        popup.add(quitItem);

        TrayIcon trayIcon = new TrayIcon(
                Toolkit.getDefaultToolkit().getImage(TrayController.class.getResource("/doggo_poopoo.png")),
                "WatchDog Manager",
                popup
        );
        trayIcon.setImageAutoSize(true);
        trayIcon.addActionListener(e -> StatusWindow.open());

        try { SystemTray.getSystemTray().add(trayIcon); } catch (Exception ignored) {}
    }
}