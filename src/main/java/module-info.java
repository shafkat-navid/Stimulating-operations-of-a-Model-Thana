module com.example.modelthana {
    requires javafx.controls;
    requires javafx.fxml;


    opens com.example.modelthana to javafx.fxml;
    exports com.example.modelthana;
}