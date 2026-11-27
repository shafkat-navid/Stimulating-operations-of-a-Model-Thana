package com.example.modelthana.Navid;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
public class ModelThanaApplication extends Application {
 @Override public void start(Stage stage) throws Exception {
  FXMLLoader loader = new FXMLLoader(getClass().getResource("MainMenu.fxml"));
  Scene scene = new Scene(loader.load(), 1050, 720);
  scene.getStylesheets().add(getClass().getResource("style.css").toExternalForm());
  stage.setTitle("Model Thana"); stage.setScene(scene); stage.show();
 }
 public static void main(String[] args) { launch(args); }
}
