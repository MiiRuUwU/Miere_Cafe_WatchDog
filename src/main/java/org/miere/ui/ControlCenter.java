package org.miere.ui;

import org.miere.core.ConfigHandler;
import org.miere.core.Target;
import org.miere.core.WatchDogEngine;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.Map;

public class ControlCenter {

    public static void open() {
        if (!authenticate()) return;

        JFrame frame = new JFrame("WatchDog Control Center");
        frame.setSize(800, 500);
        frame.setLayout(new BorderLayout());

        // Header shows Seconds for clarity
        String[] cols = {"EXE Path", "Interval (Seconds)", "Proactive?"};
        DefaultTableModel model = new DefaultTableModel(cols, 0) {
            @Override
            public Class<?> getColumnClass(int columnIndex) {
                return columnIndex == 2 ? Boolean.class : String.class;
            }

            // ALLOW EDITING: Column 1 (Seconds) and 2 (Proactive) can now be edited directly
            @Override
            public boolean isCellEditable(int row, int column) {
                return column != 0; // Path remains locked to prevent accidental mess-ups
            }
        };

        //Convert internal ms back to seconds for the display
        WatchDogEngine.getTargetMap().values().forEach(t ->
                model.addRow(new Object[]{t.path, (int)(t.intervalMs / 1000), t.isProactive})
        );

        JTable table = new JTable(model);
        JPanel btnPanel = new JPanel();
        JButton addBtn = new JButton("Add Target");
        JButton remBtn = new JButton("Remove Selected");
        JButton saveBtn = new JButton("Save & Apply");

        addBtn.addActionListener(e -> {
            FileDialog fd = new FileDialog(frame, "Select Executable", FileDialog.LOAD);
            fd.setVisible(true);
            if (fd.getFile() != null) {
                String path = fd.getDirectory() + fd.getFile();
                JTextField secField = new JTextField("30");
                JCheckBox proBox = new JCheckBox("Enable Proactive Mode (Pulse)");
                Object[] message = {"Restart interval (Seconds):", secField, proBox};

                if (JOptionPane.showConfirmDialog(frame, message, "Add Shield", JOptionPane.OK_CANCEL_OPTION) == JOptionPane.OK_OPTION) {
                    try {
                        int seconds = Integer.parseInt(secField.getText());
                        model.addRow(new Object[]{path, seconds, proBox.isSelected()});
                    } catch (Exception ex) { JOptionPane.showMessageDialog(frame, "Invalid number entered."); }
                }
            }
        });

        remBtn.addActionListener(e -> {
            int row = table.getSelectedRow();
            if (row != -1) model.removeRow(row);
        });

        saveBtn.addActionListener(e -> {
            // If a user is still typing in a cell, stop editing to commit the value
            if (table.isEditing()) {
                table.getCellEditor().stopCellEditing();
            }

            Map<String, Target> targets = WatchDogEngine.getTargetMap();
            targets.clear();

            for (int i = 0; i < model.getRowCount(); i++) {
                try {
                    String path = model.getValueAt(i, 0).toString();
                    // Parse from table as seconds
                    long sec = Long.parseLong(model.getValueAt(i, 1).toString());
                    boolean isPro = (boolean) model.getValueAt(i, 2);

                    // Re-instantiate targets (Target constructor handles the 30s floor)
                    targets.put(path, new Target(path, sec, isPro));
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(frame, "Error in row " + (i + 1) + ": Invalid interval.");
                }
            }

            try {
                ConfigHandler.saveState(targets, ConfigHandler.getStoredPasswordHash());
                JOptionPane.showMessageDialog(frame, "Shields Updated! 🛡️");

                // Refresh table to show auto-corrected values
                model.setRowCount(0);
                targets.values().forEach(t ->
                        model.addRow(new Object[]{t.path, (int)(t.intervalMs / 1000), t.isProactive})
                );
            } catch (Exception ex) { JOptionPane.showMessageDialog(frame, "Error saving config."); }
        });

        btnPanel.add(addBtn); btnPanel.add(remBtn); btnPanel.add(saveBtn);
        frame.add(new JScrollPane(table), BorderLayout.CENTER);
        frame.add(btnPanel, BorderLayout.SOUTH);
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }

    public static void changePassword() {
        if (!authenticate()) return;

        JPasswordField pf1 = new JPasswordField();
        Object[] msg1 = {"Enter NEW Password:", pf1};
        if (JOptionPane.showConfirmDialog(null, msg1, "Update Password", JOptionPane.OK_CANCEL_OPTION) != JOptionPane.OK_OPTION) return;
        String pass1 = new String(pf1.getPassword());

        if (pass1.trim().isEmpty()) {
            JOptionPane.showMessageDialog(null, "Password cannot be empty!", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        JPasswordField pf2 = new JPasswordField();
        Object[] msg2 = {"Confirm NEW Password:", pf2};
        if (JOptionPane.showConfirmDialog(null, msg2, "Update Password", JOptionPane.OK_CANCEL_OPTION) != JOptionPane.OK_OPTION) return;

        if (pass1.equals(new String(pf2.getPassword()))) {
            try {
                String newHash = org.miere.security.SecurityManager.hashPassword(pass1);
                ConfigHandler.saveState(WatchDogEngine.getTargetMap(), newHash);
                JOptionPane.showMessageDialog(null, "Password successfully updated! 🔐");
            } catch (Exception e) { JOptionPane.showMessageDialog(null, "Error saving password."); }
        } else {
            JOptionPane.showMessageDialog(null, "Passwords do not match!", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    public static boolean authenticate() {
        JPasswordField pf = new JPasswordField();
        if (JOptionPane.showConfirmDialog(null, new Object[]{"Enter Admin Password:", pf}, "Security Check", JOptionPane.OK_CANCEL_OPTION) == JOptionPane.OK_OPTION) {
            boolean isValid = org.miere.security.SecurityManager.verifyPassword(new String(pf.getPassword()), ConfigHandler.getStoredPasswordHash());
            if (!isValid) JOptionPane.showMessageDialog(null, "Access Denied. 💀", "Auth Failure", JOptionPane.ERROR_MESSAGE);
            return isValid;
        }
        return false;
    }
}