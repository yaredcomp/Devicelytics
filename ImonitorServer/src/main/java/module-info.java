module com.monitoring.server {
    // RMI is required for networking
    requires java.rmi;

    // JavaFX is required for the Dashboard UI
    requires javafx.controls;
    requires javafx.fxml;
    
    // Icons
    requires org.kordamp.ikonli.core;
    requires org.kordamp.ikonli.javafx;
    requires org.kordamp.ikonli.fontawesome5;
    
    // Logging
    requires org.slf4j;

    exports com.monitoring.devicemontoring.server;
    exports com.monitoring.devicemontoring.common;

    // Allow JavaFX to start the application
    opens com.monitoring.devicemontoring.server to javafx.graphics, javafx.fxml;
    
    // IMPORTANT: Allow JavaFX TableView to read private fields/getters in DeviceStatus
    opens com.monitoring.devicemontoring.common to javafx.base;
}