package com.monitoring.devicemontoring.client;

import com.monitoring.devicemontoring.common.DeviceStatus;
import com.monitoring.devicemontoring.common.IMonitorServer;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class ClientAppController {

    private static final Logger logger = LoggerFactory.getLogger(ClientAppController.class);
    private static final int DISCOVERY_PORT = 8888;
    private static final String DISCOVERY_RESPONSE_PREFIX = "DISCOVER_MONITOR_SERVER_RESPONSE:";
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss");

    @FXML
    private TextField ipField;
    @FXML
    private Button connectButton;
    @FXML
    private TextArea logArea;
    @FXML
    private Label statusLabel;

    private MonitorClientImpl client;
    private Thread connectionThread;
    private volatile boolean running = false;
    private String currentServerIp = null;
    private IMonitorServer connectedServer = null; // Keep track of connected server

    public void initialize() {
        log("Initializing Client...");
        try {
            // Suppress OSHI WMI warnings
            System.setProperty("org.slf4j.simpleLogger.log.oshi.util.platform.windows.WmiQueryHandler", "error");
            
            client = new MonitorClientImpl();
            log("Client Hostname: " + client.getHostname());
            
            // Auto-start discovery/connection
            startConnectionLoop();
            
        } catch (Exception e) {
            log("Error initializing client: " + e.getMessage());
            logger.error("Init failed", e);
        }
    }

    @FXML
    private void handleConnect(ActionEvent event) {
        if (running) {
            // Stop
            stopConnection();
        } else {
            // Start with manual IP if provided
            String manualIp = ipField.getText().trim();
            if (!manualIp.isEmpty()) {
                currentServerIp = manualIp;
                log("Manual IP entered: " + currentServerIp);
            } else {
                currentServerIp = null; // Trigger discovery
                log("No IP entered, switching to Auto-Discovery...");
            }
            startConnectionLoop();
        }
    }

    private void startConnectionLoop() {
        if (running) return;
        running = true;
        updateUIState(true);

        connectionThread = new Thread(() -> {
            try {
                // Set RMI Hostname
                setupRmiHostname();

                String hostname = client.getHostname();

                while (running) {
                    try {
                        // Discovery Phase
                        if (currentServerIp == null) {
                            Platform.runLater(() -> statusLabel.setText("Status: Discovering Server..."));
                            log("Listening for server broadcast...");
                            String discovered = discoverServer();
                            if (discovered != null) {
                                currentServerIp = discovered;
                                log("Server discovered at: " + currentServerIp);
                                Platform.runLater(() -> ipField.setText(currentServerIp));
                            } else {
                                log("Discovery timed out. Retrying...");
                                Thread.sleep(2000);
                                continue;
                            }
                        }

                        // Connection Phase
                        Platform.runLater(() -> statusLabel.setText("Status: Connecting to " + currentServerIp + "..."));
                        log("Attempting connection to " + currentServerIp);
                        
                        Registry registry = LocateRegistry.getRegistry(currentServerIp, 1099);
                        connectedServer = (IMonitorServer) registry.lookup("MonitorServer");

                        connectedServer.registerClient(hostname, client);
                        
                        Platform.runLater(() -> statusLabel.setText("Status: Connected"));
                        log("Connected to server!");

                        // Data Push Loop
                        while (running) {
                            try {
                                // Push current status to server
                                connectedServer.pushStatus(client.getCurrentStatus());
                            } catch (Exception e) {
                                log("Error pushing status: " + e.getMessage());
                                throw e; // Trigger re-connection logic
                            }

                            Thread.sleep(5000); // Update every 5 seconds
                        }

                    } catch (Exception e) {
                        log("Connection error: " + e.getMessage());
                        Platform.runLater(() -> statusLabel.setText("Status: Connection Failed"));
                        
                        try {
                            Thread.sleep(5000);
                        } catch (InterruptedException ie) {
                            break;
                        }
                    }
                }
            } catch (Exception e) {
                log("Fatal error in connection thread: " + e.getMessage());
            } finally {
                // Ensure we unregister if we were connected
                if (connectedServer != null) {
                    try {
                        connectedServer.unregisterClient(client.getHostname());
                    } catch (Exception ignored) {}
                    connectedServer = null;
                }
                running = false;
                Platform.runLater(() -> updateUIState(false));
            }
        });
        connectionThread.setDaemon(true);
        connectionThread.start();
    }

    public void stopConnection() {
        running = false;
        if (connectionThread != null) {
            connectionThread.interrupt();
        }
        
        // Explicitly unregister from server on stop
        if (connectedServer != null) {
            try {
                // Run in a separate thread to avoid blocking UI if network is slow
                new Thread(() -> {
                    try {
                        connectedServer.unregisterClient(client.getHostname());
                    } catch (Exception e) {
                        logger.warn("Failed to unregister client on stop", e);
                    }
                }).start();
            } catch (Exception ignored) {}
        }

        updateUIState(false);
        log("Connection stopped by user.");
        statusLabel.setText("Status: Stopped");
    }

    private void updateUIState(boolean isRunning) {
        connectButton.setText(isRunning ? "Disconnect" : "Connect");
        ipField.setDisable(isRunning);
    }

    private void setupRmiHostname() {
        try (java.net.DatagramSocket socket = new java.net.DatagramSocket()) {
            socket.connect(InetAddress.getByName("8.8.8.8"), 10002);
            String localIp = socket.getLocalAddress().getHostAddress();
            System.setProperty("java.rmi.server.hostname", localIp);
            log("RMI Hostname set to: " + localIp);
        } catch (Exception e) {
            log("Warning: Could not determine local IP for RMI.");
        }
    }

    private String discoverServer() {
        DatagramSocket socket = null;
        try {
            socket = new DatagramSocket(null);
            socket.setReuseAddress(true);
            socket.bind(new InetSocketAddress(DISCOVERY_PORT));
            socket.setSoTimeout(5000); // 5 seconds timeout for UI responsiveness

            byte[] buffer = new byte[1024];
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
            
            socket.receive(packet);
            
            String message = new String(packet.getData(), 0, packet.getLength()).trim();
            if (message.startsWith(DISCOVERY_RESPONSE_PREFIX)) {
                return packet.getAddress().getHostAddress();
            }
        } catch (java.net.SocketTimeoutException e) {
            // Expected timeout
        } catch (Exception e) {
            log("Discovery error: " + e.getMessage());
        } finally {
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
        }
        return null;
    }

    private void log(String message) {
        String timestamp = LocalDateTime.now().format(TIME_FORMATTER);
        Platform.runLater(() -> logArea.appendText("[" + timestamp + "] " + message + "\n"));
    }
}