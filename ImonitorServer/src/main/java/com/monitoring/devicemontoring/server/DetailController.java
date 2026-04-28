package com.monitoring.devicemontoring.server;

import com.monitoring.devicemontoring.common.DeviceStatus;
import com.monitoring.devicemontoring.common.ProcessInfo;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;

public class DetailController {

    @FXML private Label lblTitle;
    @FXML private Label lblHostname;
    @FXML private Label lblIp;
    @FXML private Label lblUuid;
    @FXML private Label lblOs;
    @FXML private Label lblUptime;
    @FXML private Label lblUsers;
    
    @FXML private Label lblLoadAvg;
    @FXML private Label lblMemory;
    @FXML private Label lblStorage;
    @FXML private ProgressBar storageProgress;
    @FXML private Label lblNetwork;
    @FXML private Label lblBattery;

    @FXML private TableView<ProcessInfo> procTable;
    @FXML private TableColumn<ProcessInfo, Integer> colPid;
    @FXML private TableColumn<ProcessInfo, String> colProcName;
    @FXML private TableColumn<ProcessInfo, Double> colProcCpu;
    @FXML private TableColumn<ProcessInfo, Long> colProcMem;
    @FXML private TableColumn<ProcessInfo, String> colProcState;

    @FXML
    public void initialize() {
        procTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        colPid.setCellValueFactory(new PropertyValueFactory<>("pid"));
        colProcName.setCellValueFactory(new PropertyValueFactory<>("name"));
        colProcCpu.setCellValueFactory(new PropertyValueFactory<>("cpuPercent"));
        colProcMem.setCellValueFactory(new PropertyValueFactory<>("memUsed"));
        colProcState.setCellValueFactory(new PropertyValueFactory<>("status"));
        
        colProcCpu.setCellFactory(col -> new javafx.scene.control.TableCell<>() {
            @Override
            protected void updateItem(Double item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) setText(null);
                else setText(String.format("%.2f %%", item));
            }
        });
        
        colProcMem.setCellFactory(col -> new javafx.scene.control.TableCell<>() {
            @Override
            protected void updateItem(Long item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) setText(null);
                else setText(String.format("%.1f MB", item / 1024.0 / 1024.0));
            }
        });
    }

    public void setDeviceStatus(DeviceStatus status) {
        if (status == null) return;

        lblTitle.setText("Device Analytics: " + status.hostname);
        lblHostname.setText(status.hostname);
        lblIp.setText(status.ipAddress);
        lblUuid.setText(status.uuid != null ? status.uuid : "N/A");
        lblOs.setText(status.osName + " " + status.osVersion);
        lblUptime.setText(formatUptime(status.uptimeSecs));
        lblUsers.setText(status.activeUsers != null ? String.join(", ", status.activeUsers) : "None");

        // Performance
        if (status.loadAverage != null && status.loadAverage.length >= 3) {
            lblLoadAvg.setText(String.format("%.2f / %.2f / %.2f", status.loadAverage[0], status.loadAverage[1], status.loadAverage[2]));
        } else {
            lblLoadAvg.setText(String.format("%.1f %% Load", status.cpuLoad));
        }

        lblMemory.setText(status.getFormattedRamUsage());
        
        // Storage
        double totalStorageGB = status.totalStorage / (1024.0 * 1024.0 * 1024.0);
        double usedStorageGB = status.usedStorage / (1024.0 * 1024.0 * 1024.0);
        double progress = (status.totalStorage > 0) ? (double) status.usedStorage / status.totalStorage : 0;
        lblStorage.setText(String.format("%.1f / %.1f GB (%.1f%%)", usedStorageGB, totalStorageGB, progress * 100));
        storageProgress.setProgress(progress);
        
        lblNetwork.setText(String.format("↑ %.1f KB/s | ↓ %.1f KB/s", status.netTxSpeed/1024.0, status.netRxSpeed/1024.0));
        
        if (status.batteryPercentage >= 0) {
            lblBattery.setText(status.batteryPercentage + "%");
        } else {
            lblBattery.setText("A/C Power");
        }

        if (status.topProcesses != null) {
            procTable.setItems(FXCollections.observableArrayList(status.topProcesses));
        }
    }

    private String formatUptime(long seconds) {
        long days = seconds / 86400;
        long hours = (seconds % 86400) / 3600;
        long minutes = (seconds % 3600) / 60;
        return String.format("%d d, %d h, %d m", days, hours, minutes);
    }
}
