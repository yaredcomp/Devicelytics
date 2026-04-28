package com.monitoring.devicemontoring.server.ui.controller;

import com.monitoring.devicemontoring.common.DeviceStatus;
import com.monitoring.devicemontoring.common.ProcessInfo;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;

/**
 * Controller for the detailed analytics window of a specific device.
 *
 * <p>This view displays granular metrics such as process listings, hardware UUIDs, active users,
 * and detailed resource utilization charts.
 *
 * @author Devicelytics Team
 */
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

  /** Initializes the process table and custom cell factories for formatting. */
  @FXML
  public void initialize() {
    procTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
    colPid.setCellValueFactory(new PropertyValueFactory<>("pid"));
    colProcName.setCellValueFactory(new PropertyValueFactory<>("name"));
    colProcCpu.setCellValueFactory(new PropertyValueFactory<>("cpuPercent"));
    colProcMem.setCellValueFactory(new PropertyValueFactory<>("memUsed"));
    colProcState.setCellValueFactory(new PropertyValueFactory<>("status"));

    colProcCpu.setCellFactory(
        col ->
            new TableCell<>() {
              @Override
              protected void updateItem(Double item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                  setText(null);
                } else {
                  setText(String.format("%.2f %%", item));
                }
              }
            });

    colProcMem.setCellFactory(
        col ->
            new TableCell<>() {
              @Override
              protected void updateItem(Long item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                  setText(null);
                } else {
                  setText(String.format("%.1f MB", item / 1024.0 / 1024.0));
                }
              }
            });
  }

  /**
   * Populates the view with data from a DeviceStatus snapshot.
   *
   * @param status the status object containing the metrics to display
   */
  public void setDeviceStatus(DeviceStatus status) {
    if (status == null) {
      return;
    }

    lblTitle.setText("Device Analytics: " + status.hostname);
    lblHostname.setText(status.hostname);
    lblIp.setText(status.ipAddress);
    lblUuid.setText(status.uuid != null ? status.uuid : "N/A");
    lblOs.setText(status.osName + " " + status.osVersion);
    lblUptime.setText(formatUptime(status.uptimeSecs));
    lblUsers.setText(status.activeUsers != null ? String.join(", ", status.activeUsers) : "None");

    // Performance
    if (status.loadAverage != null && status.loadAverage.length >= 3) {
      lblLoadAvg.setText(
          String.format(
              "%.2f / %.2f / %.2f",
              status.loadAverage[0], status.loadAverage[1], status.loadAverage[2]));
    } else {
      lblLoadAvg.setText(String.format("%.1f %% Load", status.cpuLoad));
    }

    lblMemory.setText(status.getFormattedRamUsage());

    // Storage
    double totalStorageGb = status.totalStorage / (1024.0 * 1024.0 * 1024.0);
    double usedStorageGb = status.usedStorage / (1024.0 * 1024.0 * 1024.0);
    double progress = (status.totalStorage > 0) ? (double) status.usedStorage / status.totalStorage : 0;
    lblStorage.setText(
        String.format("%.1f / %.1f GB (%.1f%%)", usedStorageGb, totalStorageGb, progress * 100));
    storageProgress.setProgress(progress);

    lblNetwork.setText(
        String.format(
            "↑ %.1f KB/s | ↓ %.1f KB/s", status.netTxSpeed / 1024.0, status.netRxSpeed / 1024.0));

    if (status.batteryPercentage >= 0) {
      lblBattery.setText(status.batteryPercentage + "%");
    } else {
      lblBattery.setText("A/C Power");
    }

    if (status.topProcesses != null) {
      procTable.setItems(FXCollections.observableArrayList(status.topProcesses));
    }
  }

  /**
   * Formats a raw second count into a human-readable duration string.
   *
   * @param seconds the total number of seconds
   * @return a formatted string (e.g., "2 d, 4 h, 15 m")
   */
  private String formatUptime(long seconds) {
    long days = seconds / 86400;
    long hours = (seconds % 86400) / 3600;
    long minutes = (seconds % 3600) / 60;
    return String.format("%d d, %d h, %d m", days, hours, minutes);
  }
}
