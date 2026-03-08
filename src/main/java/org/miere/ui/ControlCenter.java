package org.miere.ui;

import org.miere.core.ConfigHandler;
import org.miere.core.WatchDogEngine;
import org.miere.security.SecurityManager;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.Map;

/**
 * The main UI window for managing what apps we are actually watching.
 */
public class ControlCenter {

    /**
     * Open the management window if the user isn't an impostor... SUSSYBAKA.
     */
    public static void open() {
        if (!authenticate()) return;

        // Create the window frame and set the size
        JFrame frame = new JFrame("WatchDog Control Center");
        frame.setSize(700, 450);
        frame.setLayout(new BorderLayout());

        // Set up the table columns
        String[] cols = {"EXE Path", "Delay (ms)"};
        DefaultTableModel model = new DefaultTableModel(cols, 0);
        WatchDogEngine.getTargetMap().forEach((p, d) -> model.addRow(new Object[]{p, d}));

        // Initialize the table and buttons
        JTable table = new JTable(model);
        JPanel btnPanel = new JPanel();
        JButton addBtn = new JButton("Add Target");
        JButton remBtn = new JButton("Remove Selected");
        JButton saveBtn = new JButton("Save & Apply");

        // Action to pick an EXE file because we can't guess what they want to watch DUMBAHH
        addBtn.addActionListener(e -> {
            FileDialog fd = new FileDialog(frame, "Select Executable", FileDialog.LOAD);
            fd.setVisible(true);
            if (fd.getFile() != null) {
                String path = fd.getDirectory() + fd.getFile();
                String delayStr = JOptionPane.showInputDialog("Restart delay (ms):", "2000");
                if (delayStr != null) {
                    try { model.addRow(new Object[]{path, Integer.parseInt(delayStr)}); }
                    catch (Exception ex) { JOptionPane.showMessageDialog(frame, "Invalid Delay."); }
                }
            }
        });

        // Remove the selected row
        remBtn.addActionListener(e -> {
            int row = table.getSelectedRow();
            if (row != -1) model.removeRow(row);
        });

        // Write the changes to the config files because we want them to stay with me I love u so much pls don't leave me (I have anxious attachment)
        saveBtn.addActionListener(e -> {
            Map<String, Integer> targets = WatchDogEngine.getTargetMap();
            targets.clear();
            for (int i = 0; i < model.getRowCount(); i++) {
                targets.put(model.getValueAt(i, 0).toString(), Integer.parseInt(model.getValueAt(i, 1).toString()));
            }
            try {
                ConfigHandler.saveState(targets, ConfigHandler.getStoredPasswordHash());
                JOptionPane.showMessageDialog(frame, "Shields Updated! 🛡️");
            } catch (Exception ex) { JOptionPane.showMessageDialog(frame, "Error saving config."); }
        });

        // Add everything to the frame and make it visible [cite: 2026-03-08]
        btnPanel.add(addBtn); btnPanel.add(remBtn); btnPanel.add(saveBtn);
        frame.add(new JScrollPane(table), BorderLayout.CENTER);
        frame.add(btnPanel, BorderLayout.SOUTH);
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }

    /**
     * Change the admin password with a double-entry check to avoid typos.
     */
    public static void changePassword() {
        // Verify they know the current password before letting them change it [cite: 2026-03-08]
        if (!authenticate()) return;


        JPasswordField pf1 = new JPasswordField();
        Object[] msg1 = {"Enter NEW Password:", pf1};
        if (JOptionPane.showConfirmDialog(null, msg1, "Update Password", JOptionPane.OK_CANCEL_OPTION) != JOptionPane.OK_OPTION) return;
        String pass1 = new String(pf1.getPassword());

        // Don't let them have a blank password because that's fckin stuped
        if (pass1.trim().isEmpty()) {
            JOptionPane.showMessageDialog(null, "Password cannot be empty!", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        // Make them type it again lol
        JPasswordField pf2 = new JPasswordField();
        Object[] msg2 = {"Confirm NEW Password:", pf2};
        if (JOptionPane.showConfirmDialog(null, msg2, "Update Password", JOptionPane.OK_CANCEL_OPTION) != JOptionPane.OK_OPTION) return;
        String pass2 = new String(pf2.getPassword());

        // Save if they match, complain if they don't ez
        if (pass1.equals(pass2)) {
            try {
                String newHash = SecurityManager.hashPassword(pass1);
                ConfigHandler.saveState(WatchDogEngine.getTargetMap(), newHash);
                JOptionPane.showMessageDialog(null, "Password successfully updated! 🔐");
            } catch (Exception e) {
                JOptionPane.showMessageDialog(null, "Error saving new password.");
            }
        } else {
            JOptionPane.showMessageDialog(null, "Passwords do not match! Aborting change.", "Typo Detected", JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * Ask for the password and return true if it matches the stored hash, I feel like this is obvious
     */
    public static boolean authenticate() {
        JPasswordField pf = new JPasswordField();
        Object[] message = {"Enter Admin Password:", pf};

        int result = JOptionPane.showConfirmDialog(null, message, "Security Check", JOptionPane.OK_CANCEL_OPTION);

        if (result == JOptionPane.OK_OPTION) {
            String input = new String(pf.getPassword());
            boolean isValid = SecurityManager.verifyPassword(input, ConfigHandler.getStoredPasswordHash());

            // Show a skull because they are noob and they deserve to get testicular torsion
            if (!isValid) {
                JOptionPane.showMessageDialog(null, "Access Denied: Incorrect Password. 💀", "Auth Failure", JOptionPane.ERROR_MESSAGE);
            }
            return isValid;
        }
        return false; // User bounced lolol
    }
}