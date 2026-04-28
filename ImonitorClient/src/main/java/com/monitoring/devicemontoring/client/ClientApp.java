package com.monitoring.devicemontoring.client;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;

import java.io.IOException;

/**
 * JavaFX Client Application entry point.
 */
public class ClientApp extends Application {

    private static Scene scene;
    private ClientAppController controller;

    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/monitoring/devicemontoring/client/client_app.fxml"));
        Parent root = loader.load();
        
        controller = loader.getController();

        scene = new Scene(root, 500, 400);
        stage.setScene(scene);
        stage.setTitle("Device Monitoring Client");
        
        // Set window icon
        try {
            stage.getIcons().add(new Image(getClass().getResourceAsStream("/com/monitoring/devicemontoring/client/client.ico")));
        } catch (Exception e) {
            System.err.println("Error loading client icon: " + e.getMessage());
        }

        // Ensure graceful shutdown
        stage.setOnCloseRequest(e -> {
            if (controller != null) {
                controller.stopConnection();
            }
            // Give a small delay for the unregister RMI call to go out before killing the JVM
            try {
                Thread.sleep(500); 
            } catch (InterruptedException ignored) {}

            System.exit(0); 
        });
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }

}
