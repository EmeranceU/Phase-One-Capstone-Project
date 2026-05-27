module com.app.igirepay {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.sql;

    requires org.controlsfx.controls;
    requires com.dlsc.formsfx;
    requires org.kordamp.bootstrapfx.core;

    opens com.app.igirepay to javafx.fxml;
    opens com.app.igirepay.lab3.controller to javafx.fxml;
    exports com.app.igirepay;
    exports com.app.igirepay.lab3.app;
}