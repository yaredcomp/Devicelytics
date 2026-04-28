module com.monitoring.server {
    // RMI is required for networking
    requires java.rmi;

    // JavaFX is required for the Dashboard UI
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.graphics; // Added for StackPane and other UI elements

    // Icons
    requires org.kordamp.ikonli.core;
    requires org.kordamp.ikonli.javafx;
    requires org.kordamp.ikonli.fontawesome5;
    
    // Logging
    requires org.slf4j;

    // Export packages that contain public APIs or entry points
    exports com.monitoring.devicemontoring.common; // Common DTOs and Interfaces
    exports com.monitoring.devicemontoring.server.app; // ServerApp entry point
    exports com.monitoring.devicemontoring.server.core; // RMI Server implementation

    // Open packages for JavaFX reflective access (FXML, @FXML fields, etc.)
    opens com.monitoring.devicemontoring.server.app to javafx.graphics, javafx.fxml;
    opens com.monitoring.devicemontoring.server.ui.controller to javafx.graphics, javafx.fxml;
    
    // IMPORTANT: Allow JavaFX TableView to read private fields/getters in DeviceStatus
    opens com.monitoring.devicemontoring.common to javafx.base;
}
