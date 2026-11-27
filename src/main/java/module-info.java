module com.example.modelthana {
    requires javafx.controls;
    requires javafx.fxml;

    opens com.example.modelthana.navid.controller to javafx.fxml;
    exports com.example.modelthana.navid;
    exports com.example.modelthana.navid.model;
}