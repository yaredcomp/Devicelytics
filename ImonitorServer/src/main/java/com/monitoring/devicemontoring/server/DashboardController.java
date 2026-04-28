package com.monitoring.devicemontoring.server;

import com.monitoring.devicemontoring.common.DeviceStatus;
import javafx.application.Platform;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetAddress;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

public class DashboardController {

    private static final Logger logger = LoggerFactory.getLogger(DashboardController.class);

    @FXML private VBox sidebar;
    @FXML private Button btnToggleSidebar;
    @FXML private Label lblSidebarTitle;
    
    @FXML private Button btnOverview;
    @FXML private Button btnClients;
    @FXML private Button btnSettings;
    @FXML private Button btnLogs;
    
    @FXML private StackPane contentPane;
    @FXML private VBox viewOverview;
    @FXML private VBox viewSettings;
    @FXML private VBox viewLogs;
    private VBox viewClients; 
    
    @FXML private TextArea txtLogs;
    @FXML private Label lblServerIp;
    @FXML private TableView<DeviceStatus> clientTable;
    @FXML private TableColumn<DeviceStatus, String> colHostname;
    @FXML private TableColumn<DeviceStatus, String> colIp;
    @FXML private TableColumn<DeviceStatus, String> colOs;
    @FXML private TableColumn<DeviceStatus, Double> colCpu;
    @FXML private TableColumn<DeviceStatus, String> colRam;
    @FXML private TableColumn<DeviceStatus, String> colNet;
    
    @FXML private Label lblTotalClients;
    @FXML private Label lblAvgCpu;
    @FXML private Label lblAvgRam;
    @FXML private Label lblTotalNetwork;
    @FXML private BarChart<String, Number> cpuChart;

    private final ObservableList<DeviceStatus> clientList = FXCollections.observableArrayList();
    private XYChart.Series<String, Number> cpuSeries;
    private boolean isSidebarExpanded = true;
    private MonitorServerImpl server;
    private ClientsController clientsController;

    private final Map<String, DetailController> openDetailControllers = new HashMap<>();
    private final DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss");

    @FXML
    public void initialize() {
        try {
            lblServerIp.setText(InetAddress.getLocalHost().getHostAddress());
        } catch (Exception e) {
            lblServerIp.setText("Unknown");
        }

        cpuSeries = new XYChart.Series<>();
        cpuSeries.setName("CPU Load");
        cpuChart.getData().add(cpuSeries);
        cpuChart.setAnimated(false);

        clientTable.setItems(clientList);
        colHostname.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().hostname));
        colIp.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().ipAddress));
        colOs.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().osName));
        
        colCpu.setCellValueFactory(data -> new SimpleObjectProperty<>(data.getValue().cpuLoad));
        colCpu.setCellFactory(column -> new TableCell<DeviceStatus, Double>() {
            @Override
            protected void updateItem(Double item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(String.format("%.1f %%", item));
                }
            }
        });

        colRam.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getFormattedRamUsage()));
        colNet.setCellValueFactory(data -> {
            DeviceStatus s = data.getValue();
            return new SimpleStringProperty(String.format("↑ %d ↓ %d KB/s", s.netTxSpeed / 1024, s.netRxSpeed / 1024));
        });

        clientTable.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2 && clientTable.getSelectionModel().getSelectedItem() != null) {
                openDetailView(clientTable.getSelectionModel().getSelectedItem());
            }
        });
        
        loadClientsView();
        showOverview();
        updateAnalytics();
    }

    private void loadClientsView() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/monitoring/devicemontoring/server/clients.fxml"));
            viewClients = loader.load();
            clientsController = loader.getController();
            contentPane.getChildren().add(viewClients);
            viewClients.setVisible(false);
        } catch (IOException e) {
            logger.error("Failed to load clients view", e);
        }
    }

    public void setServer(MonitorServerImpl server) {
        this.server = server;
        if (clientsController != null) {
            clientsController.setServer(server);
        }
    }

    @FXML
    private void toggleSidebar() {
        sidebar.setPrefWidth(isSidebarExpanded ? 60 : 200);
        lblSidebarTitle.setVisible(!isSidebarExpanded);
        isSidebarExpanded = !isSidebarExpanded;
    }

    @FXML private void showOverview() { switchView(viewOverview, btnOverview); }
    @FXML private void showClients() { 
        switchView(viewClients, btnClients);
        if (clientsController != null) clientsController.updateClients(clientList);
    }
    @FXML private void showSettings() { switchView(viewSettings, btnSettings); }
    @FXML private void showLogs() { switchView(viewLogs, btnLogs); }

    private void switchView(VBox view, Button btn) {
        viewOverview.setVisible(false);
        viewSettings.setVisible(false);
        viewLogs.setVisible(false);
        if (viewClients != null) viewClients.setVisible(false);
        
        if (view != null) view.setVisible(true);
        
        btnOverview.getStyleClass().remove("sidebar-btn-active");
        btnClients.getStyleClass().remove("sidebar-btn-active");
        btnSettings.getStyleClass().remove("sidebar-btn-active");
        btnLogs.getStyleClass().remove("sidebar-btn-active");
        btn.getStyleClass().add("sidebar-btn-active");
    }

    public void updateClient(DeviceStatus status) {
        Platform.runLater(() -> {
            boolean found = false;
            for (int i = 0; i < clientList.size(); i++) {
                if (clientList.get(i).hostname.equals(status.hostname)) {
                    clientList.set(i, status);
                    found = true;
                    break;
                }
            }
            if (!found) clientList.add(status);
            updateAnalytics();
            if (clientsController != null) clientsController.updateClients(clientList);
            if (openDetailControllers.containsKey(status.hostname)) {
                openDetailControllers.get(status.hostname).setDeviceStatus(status);
            }
        });
    }

    public void removeClient(String hostname) {
        Platform.runLater(() -> {
            clientList.removeIf(c -> c.hostname.equals(hostname));
            updateAnalytics();
            if (clientsController != null) clientsController.updateClients(clientList);
            openDetailControllers.remove(hostname);
        });
    }

    private void updateAnalytics() {
        int total = clientList.size();
        lblTotalClients.setText(String.valueOf(total));
        if (total == 0) return;

        double totalCpu = 0;
        cpuSeries.getData().clear();
        for (DeviceStatus s : clientList) {
            totalCpu += s.cpuLoad;
            cpuSeries.getData().add(new XYChart.Data<>(s.hostname, s.cpuLoad));
        }
        lblAvgCpu.setText(String.format("%.1f %%", totalCpu / total));
    }

    private void openDetailView(DeviceStatus status) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/monitoring/devicemontoring/server/detail.fxml"));
            Parent root = loader.load();
            DetailController controller = loader.getController();
            controller.setDeviceStatus(status);
            openDetailControllers.put(status.hostname, controller);
            Stage stage = new Stage();
            stage.setTitle("Details: " + status.hostname);
            stage.setScene(new Scene(root));
            stage.setOnHidden(e -> openDetailControllers.remove(status.hostname));
            stage.show();
        } catch (IOException e) {
            logger.error("Failed to open detail view", e);
        }
    }
}
