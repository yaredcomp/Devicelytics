package com.monitoring.devicemontoring.server;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * JavaFX Dashboard for the Monitor Server.
 */
public class ServerApp extends Application {

    private static final Logger logger = LoggerFactory.getLogger(ServerApp.class);
    private MonitorServerImpl serverImpl;
    private DashboardController dashboardController;
    private boolean running = true;

    public static void main(String[] args) {
        try {
            String ip = InetAddress.getLocalHost().getHostAddress();
            System.setProperty("java.rmi.server.hostname", ip);
        } catch (Exception e) {
            logger.error("Could not determine local IP", e);
        }
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/monitoring/devicemontoring/server/dashboard.fxml"));
            Parent root = loader.load();
            dashboardController = loader.getController();

            startServer();
            dashboardController.setServer(serverImpl); // Pass server instance to controller
            
            startBroadcast();

            Scene scene = new Scene(root, 1000, 700);
            primaryStage.setTitle("Devicelytics - Monitoring Server");
            primaryStage.setScene(scene);
            primaryStage.show();
        } catch (Exception e) {
            logger.error("Failed to start app", e);
        }
    }

    private void startServer() {
        try {
            serverImpl = new MonitorServerImpl();
            serverImpl.setListeners(
                status -> dashboardController.updateClient(status),
                hostname -> dashboardController.removeClient(hostname)
            );
            Registry registry = LocateRegistry.createRegistry(1099);
            registry.rebind("MonitorServer", serverImpl);
        } catch (Exception e) {
            logger.error("Failed to start server", e);
        }
    }
    
    private void startBroadcast() {
        Thread t = new Thread(() -> {
            try (DatagramSocket socket = new DatagramSocket()) {
                socket.setBroadcast(true);
                while (running) {
                    try {
                        String message = "DISCOVER_MONITOR_SERVER_RESPONSE:" + InetAddress.getLocalHost().getHostAddress() + ":1099";
                        byte[] buffer = message.getBytes();
                        DatagramPacket packet = new DatagramPacket(buffer, buffer.length, InetAddress.getByName("255.255.255.255"), 8888);
                        socket.send(packet);
                        Thread.sleep(2000);
                    } catch (Exception ignored) {}
                }
            } catch (Exception ignored) {}
        });
        t.setDaemon(true);
        t.start();
    }

    @Override
    public void stop() {
        running = false;
        System.exit(0);
    }
}
