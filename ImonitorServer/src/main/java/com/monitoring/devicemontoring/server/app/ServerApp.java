package com.monitoring.devicemontoring.server.app;

import com.monitoring.devicemontoring.server.core.MonitorServerImpl;
import com.monitoring.devicemontoring.server.ui.controller.DashboardController;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Main entry point for the Devicelytics Server Dashboard application.
 *
 * <p>This class initializes the JavaFX environment, starts the RMI monitoring server, and begins
 * UDP broadcasting for automatic client discovery.
 *
 * @author Devicelytics Team
 */
public class ServerApp extends Application {

  private static final Logger logger = LoggerFactory.getLogger(ServerApp.class);
  private MonitorServerImpl serverImpl;
  private DashboardController dashboardController;
  private boolean running = true;

  /**
   * Main entry point for the Java application.
   *
   * @param args command line arguments
   */
  public static void main(String[] args) {
    try {
      String ip = InetAddress.getLocalHost().getHostAddress();
      System.setProperty("java.rmi.server.hostname", ip);
    } catch (Exception e) {
      logger.error("Could not determine local IP for RMI hostname", e);
    }
    launch(args);
  }

  /**
   * Initializes the JavaFX stage, RMI server, and network broadcasting.
   *
   * @param primaryStage the primary stage for this application
   */
  @Override
  public void start(Stage primaryStage) {
    try {
      // 1. Load FXML and initialize controller
      FXMLLoader loader =
          new FXMLLoader(
              getClass().getResource("/com/monitoring/devicemontoring/server/ui/view/dashboard.fxml"));
      Parent root = loader.load();
      dashboardController = loader.getController();

      // 2. Start RMI Monitoring Server
      startServer();

      // 3. Link model to controller
      dashboardController.setServer(serverImpl);

      // 4. Start Discovery Broadcast
      startBroadcast();

      // 5. Show Dashboard
      Scene scene = new Scene(root, 1000, 700);
      primaryStage.setTitle("Devicelytics - Monitoring Server");
      primaryStage.setScene(scene);
      primaryStage.show();

    } catch (Exception e) {
      logger.error("Failed to initialize Devicelytics Server application", e);
    }
  }

  /**
   * Starts the RMI server instance and binds it to the registry.
   */
  private void startServer() {
    try {
      serverImpl = new MonitorServerImpl();

      // Hook up server events to the dashboard controller
      serverImpl.setListeners(
          status -> dashboardController.updateClient(status),
          hostname -> dashboardController.removeClient(hostname));

      Registry registry = LocateRegistry.createRegistry(1099);
      registry.rebind("MonitorServer", serverImpl);
      logger.info("RMI Server bound to registry on port 1099");

    } catch (Exception e) {
      logger.error("Failed to start RMI monitoring server", e);
    }
  }

  /**
   * Starts a background thread to broadcast the server's presence via UDP.
   * This allows clients to automatically discover and connect to the server.
   */
  private void startBroadcast() {
    Thread t =
        new Thread(
            () -> {
              try (DatagramSocket socket = new DatagramSocket()) {
                socket.setBroadcast(true);
                while (running) {
                  try {
                    String message =
                        "DISCOVER_MONITOR_SERVER_RESPONSE:"
                            + InetAddress.getLocalHost().getHostAddress()
                            + ":1099";
                    byte[] buffer = message.getBytes();
                    DatagramPacket packet =
                        new DatagramPacket(
                            buffer,
                            buffer.length,
                            InetAddress.getByName("255.255.255.255"),
                            8888);
                    socket.send(packet);
                    Thread.sleep(2000);
                  } catch (Exception ignored) {
                    // Ignore broadcast tick errors
                  }
                }
              } catch (Exception e) {
                logger.error("UDP Discovery Broadcast failed", e);
              }
            });
    t.setDaemon(true);
    t.start();
  }

  /**
   * Gracefully shuts down the application and its background threads.
   */
  @Override
  public void stop() {
    running = false;
    logger.info("Devicelytics Server shutting down...");
    System.exit(0);
  }
}
