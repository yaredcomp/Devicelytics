package com.monitoring.devicemontoring.server;

import com.monitoring.devicemontoring.common.DeviceStatus;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

public class ClientsController {

    @FXML private TableView<DeviceStatus> clientTable;
    @FXML private TableColumn<DeviceStatus, String> colHostname;
    @FXML private TableColumn<DeviceStatus, String> colIp;
    @FXML private TableColumn<DeviceStatus, String> colStatus;

    @FXML private TextField txtCommand;
    @FXML private Label lblResult;
    @FXML private ScrollPane terminalScroll;

    private MonitorServerImpl server;
    private final ObservableList<DeviceStatus> displayList = FXCollections.observableArrayList();
    private final DateTimeFormatter dtf = DateTimeFormatter.ofPattern("HH:mm:ss");
    
    // Remember selection
    private DeviceStatus selectedClient;

    @FXML
    public void initialize() {
        clientTable.setItems(displayList);
        colHostname.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().hostname));
        colIp.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().ipAddress));
        colStatus.setCellValueFactory(d -> new SimpleStringProperty("ONLINE"));
        
        terminalScroll.vvalueProperty().bind(lblResult.heightProperty());

        // Update selection reference whenever user clicks
        clientTable.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                selectedClient = newVal;
                appendTerminal("Target switched to: " + selectedClient.hostname);
            }
        });
    }

    public void setServer(MonitorServerImpl server) {
        this.server = server;
    }

    public void updateClients(ObservableList<DeviceStatus> masterList) {
        Platform.runLater(() -> {
            displayList.setAll(masterList);
            // Re-select if previously selected client is still there
            if (selectedClient != null) {
                for (DeviceStatus s : displayList) {
                    if (s.hostname.equals(selectedClient.hostname)) {
                        clientTable.getSelectionModel().select(s);
                        break;
                    }
                }
            }
        });
    }

    @FXML
    private void handleExecute() {
        String input = txtCommand.getText().trim();
        if (input.isEmpty()) return;
        
        // Use 'shell' as the default command type for free-text input
        executeRemote("shell", input);
        txtCommand.clear();
    }

    @FXML private void quickPing() { executeRemote("ping", ""); }
    @FXML private void quickInfo() { executeRemote("info", ""); }
    @FXML private void quickProcesses() { 
        // Auto-detect command based on OS for the user
        String cmd = (selectedClient != null && selectedClient.osName.toLowerCase().contains("win")) 
                     ? "tasklist" : "ps -aux";
        executeRemote("shell", cmd); 
    }

    private void executeRemote(String cmdType, String payload) {
        if (selectedClient == null) {
            appendTerminal("Error: Select a device from the table first.");
            return;
        }

        appendTerminal("> " + cmdType + " " + payload);
        
        new Thread(() -> {
            try {
                String result = server.sendCommand(selectedClient.hostname, cmdType, payload);
                Platform.runLater(() -> appendTerminal(result));
            } catch (Exception e) {
                Platform.runLater(() -> appendTerminal("Server Error: " + e.getMessage()));
            }
        }).start();
    }

    private void appendTerminal(String text) {
        String timestamp = LocalTime.now().format(dtf);
        String currentText = lblResult.getText();
        if (currentText.startsWith("Terminal ready")) currentText = "";
        lblResult.setText(currentText + "\n[" + timestamp + "] " + text);
    }
}
