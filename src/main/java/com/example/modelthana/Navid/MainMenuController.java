package com.example.modelthana.Navid;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.Node;
import javafx.stage.Stage;
import javafx.event.ActionEvent;
public class MainMenuController {
 @FXML private void openUser3(ActionEvent event) throws Exception {
  openPage(event,"User3Dashboard.fxml","User 3 - Patrol Constable");
 }
 @FXML private void openUser4(ActionEvent event) throws Exception {
  openPage(event,"User4Dashboard.fxml","User 4 - Traffic Sergeant");
 }
 private void openPage(ActionEvent event,String file,String title) throws Exception {
  FXMLLoader loader=new FXMLLoader(getClass().getResource(file));
  Scene scene=new Scene(loader.load(),1050,720);
  scene.getStylesheets().add(getClass().getResource("style.css").toExternalForm());
  Stage stage=(Stage)((Node)event.getSource()).getScene().getWindow();
  stage.setTitle(title); stage.setScene(scene);
 }
}
