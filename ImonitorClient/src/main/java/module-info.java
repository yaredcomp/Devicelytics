module com.monitoring.client {
    // RMI is required for networking
    requires java.rmi;
    
    // OSHI is required for hardware info
    requires com.github.oshi;
    
    // Logging
    requires org.slf4j;

    // JavaFX
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.graphics;

    // Export packages so RMI can access the implementation and interfaces
    exports com.monitoring.devicemontoring.client;
    exports com.monitoring.devicemontoring.common;

    // Allow JavaFX to access the controller and load resources (like the icon)
    opens com.monitoring.devicemontoring.client to javafx.fxml, javafx.graphics;
    
    // IMPORTANT: Allow JavaFX TableView to read private fields/getters in DeviceStatus
    opens com.monitoring.devicemontoring.common to javafx.base;
}