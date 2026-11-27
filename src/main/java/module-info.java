module com.example.modelthana {
    requires javafx.controls;
    requires javafx.fxml;

    opens com.example.modelthana.Navid to javafx.fxml;
    exports com.example.modelthana.Navid;
}