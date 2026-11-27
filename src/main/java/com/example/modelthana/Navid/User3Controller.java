package com.example.modelthana.Navid;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;

public class User3Controller {
 @FXML private TextField idField, subjectField, locationField;
 @FXML private ComboBox<String> operationBox, statusBox;
 @FXML private DatePicker datePicker;
 @FXML private TextArea detailsArea;
 @FXML private CheckBox urgentCheck;
 @FXML private Label messageLabel;
 @FXML private TableView<RecordRow> recordTable;
 @FXML private TableColumn<RecordRow,String> idColumn, operationColumn, subjectColumn, locationColumn, dateColumn, statusColumn;

 private ObservableList<RecordRow> allRecords = FXCollections.observableArrayList();
 private int nextId = 1;

 @FXML public void initialize() {
  operationBox.setItems(FXCollections.observableArrayList("Duty Assignment", "Patrol Log", "Dispatch Response", "Incident Report", "Arrest Record", "Backup Request", "Shift Attendance", "Stop and Search"));
  statusBox.setItems(FXCollections.observableArrayList("Pending","In Progress","Completed"));
  datePicker.setValue(LocalDate.now());
  idColumn.setCellValueFactory(cell -> cell.getValue().idProperty());
  operationColumn.setCellValueFactory(cell -> cell.getValue().operationProperty());
  subjectColumn.setCellValueFactory(cell -> cell.getValue().subjectProperty());
  locationColumn.setCellValueFactory(cell -> cell.getValue().locationProperty());
  dateColumn.setCellValueFactory(cell -> cell.getValue().dateProperty());
  statusColumn.setCellValueFactory(cell -> cell.getValue().statusProperty());
  recordTable.setItems(allRecords);
  updateId();
 }

 @FXML private void addRecord() {
  if (operationBox.getValue() == null || subjectField.getText().trim().isEmpty()
      || locationField.getText().trim().isEmpty() || datePicker.getValue() == null
      || statusBox.getValue() == null || detailsArea.getText().trim().isEmpty()) {
   messageLabel.setText("Please complete all fields before adding a record.");
   return;
  }
  String id = "PC-" + nextId++;
  String status = statusBox.getValue();
  if (urgentCheck.isSelected()) status = status + " / Priority";
  RecordRow row = new RecordRow(id, operationBox.getValue(), subjectField.getText().trim(),
      locationField.getText().trim(), datePicker.getValue().toString(), status);
  allRecords.add(row);
  recordTable.setItems(allRecords);
  recordTable.getSelectionModel().select(row);
  recordTable.scrollTo(row);
  saveRecord(row);
  messageLabel.setText("Record added successfully. Total records: " + allRecords.size());
  updateId();
  clearEntryFields();
 }

 @FXML private void filterRecords() {
  if (operationBox.getValue() == null) {
   recordTable.setItems(allRecords);
   messageLabel.setText("Showing all " + allRecords.size() + " records.");
   return;
  }
  ObservableList<RecordRow> filtered = FXCollections.observableArrayList();
  for (RecordRow row : allRecords) {
   if (row.getOperation().equals(operationBox.getValue())) filtered.add(row);
  }
  recordTable.setItems(filtered);
  messageLabel.setText("Found " + filtered.size() + " record(s) for " + operationBox.getValue() + ".");
 }

 @FXML private void showLatest() {
  if (allRecords.isEmpty()) {
   messageLabel.setText("There are no records yet.");
   return;
  }
  RecordRow latest = allRecords.get(allRecords.size()-1);
  recordTable.setItems(allRecords);
  recordTable.getSelectionModel().select(latest);
  recordTable.scrollTo(latest);
  messageLabel.setText("Most recent: " + latest.getId() + " - " + latest.getOperation());
 }

 @FXML private void clearForm() {
  operationBox.getSelectionModel().clearSelection();
  subjectField.clear(); locationField.clear(); detailsArea.clear();
  datePicker.setValue(LocalDate.now()); statusBox.getSelectionModel().clearSelection();
  urgentCheck.setSelected(false); messageLabel.setText("Form cleared.");
 }

 private void clearEntryFields() {
  subjectField.clear(); locationField.clear(); detailsArea.clear();
  urgentCheck.setSelected(false);
 }

 private void updateId() {
  idField.setText("PC-" + nextId);
 }

 private void saveRecord(RecordRow row) {
  try {
   FileWriter writer = new FileWriter("user3_records.txt", true);
   writer.write(row.getId()+" | "+row.getOperation()+" | "+row.getSubject()+" | "+
       row.getLocation()+" | "+row.getDate()+" | "+row.getStatus()+System.lineSeparator());
   writer.close();
  } catch (IOException e) {
   messageLabel.setText("Record added to table, but file save failed: " + e.getMessage());
  }
 }
}
