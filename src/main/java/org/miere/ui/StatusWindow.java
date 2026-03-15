package org.miere.ui;

import org.miere.core.Target;
import org.miere.core.WatchDogEngine;
import javax.swing.*;
import javax.swing.filechooser.FileSystemView;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class StatusWindow extends JFrame {
    private final JTable table;
    private final DefaultTableModel model;
    private final Map<String, Icon> iconCache = new HashMap<>();

    public StatusWindow() {
        setTitle("WatchDog Live Dashboard 🛡️");
        setSize(600, 400);
        setLayout(new BorderLayout());

        String[] cols = {"Task", "Status", "Next Pulse"};
        model = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };

        table = new JTable(model);
        table.setRowHeight(32);

        // Custom renderer to show icons and names together
        table.getColumnModel().getColumn(0).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                JLabel label = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                String path = (String) value;
                File file = new File(path);
                label.setText(file.getName());
                // Cache icons so we don't lag the UI
                label.setIcon(iconCache.computeIfAbsent(path, p -> FileSystemView.getFileSystemView().getSystemIcon(new File(p))));
                return label;
            }
        });

        add(new JScrollPane(table), BorderLayout.CENTER);
        setLocationRelativeTo(null);

        // UI Refresh Heartbeat (1 second)
        new Timer(1000, e -> updateTable()).start();
    }

    private void updateTable() {
        model.setRowCount(0);
        long now = System.currentTimeMillis();

        WatchDogEngine.getTargetMap().values().forEach(t -> {
            long remMs = (t.lastActionTime + t.intervalMs) - now;
            String nextPulse = !t.isProactive ? "---" : formatTime(remMs);
            String status = t.isProactive ? "🛡️ Guarded + ⚡ Pulse" : "🛡️ Guarded Only";

            model.addRow(new Object[]{t.path, status, nextPulse});
        });
    }

    private String formatTime(long ms) {
        if (ms <= 0) return "Starting...";
        long sec = ms / 1000;
        if (sec < 60) return sec + "s";
        return (sec / 60) + "m " + (sec % 60) + "s";
    }

    private static StatusWindow instance;
    public static void open() {
        if (instance == null) instance = new StatusWindow();
        instance.setVisible(true);
        instance.toFront();
    }
}