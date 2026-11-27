package com.example.modelthana.navid.controller;

import com.example.modelthana.navid.DashboardNavigation;
import com.example.modelthana.navid.model.DashboardStore;
import com.example.modelthana.navid.model.PatrolConstableModel;
import com.example.modelthana.navid.model.RecordRow;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class PatrolConstableController {
    @FXML private TextField idField;
    @FXML private ComboBox<String> operationBox;
    @FXML private GridPane formFields;
    @FXML private Label messageLabel, formHint, routeDetailsLabel;
    @FXML private Button submitButton, acknowledgeButton, viewDispatchButton, acceptDispatchButton;
    @FXML private TableView<RecordRow> recordTable;
    @FXML private TableColumn<RecordRow, String> idColumn, operationColumn, subjectColumn,
            locationColumn, dateColumn, statusColumn, detailsColumn;

    private final PatrolConstableModel model = new PatrolConstableModel();
    private final ObservableList<RecordRow> allRecords = model.getRecords();
    private final Map<String, Control> controls = new LinkedHashMap<>();
    private List<Field> currentFields = List.of();

    private record Field(String key, String label, String type, List<String> options, boolean optional) {
        static Field text(String key, String label) {
            return new Field(key, label, "text", List.of(), false);
        }

        static Field optional(String key, String label) {
            return new Field(key, label, "text", List.of(), true);
        }

        static Field date(String key, String label) {
            return new Field(key, label, "date", List.of(), false);
        }

        static Field choice(String key, String label, String... options) {
            return new Field(key, label, "choice", List.of(options), false);
        }
    }

    @FXML
    public void initialize() {
        operationBox.setItems(FXCollections.observableArrayList(
                PatrolConstableModel.DUTY, PatrolConstableModel.PATROL_LOG,
                PatrolConstableModel.DISPATCH, PatrolConstableModel.INCIDENT,
                PatrolConstableModel.ARREST, PatrolConstableModel.BACKUP,
                PatrolConstableModel.ATTENDANCE, PatrolConstableModel.STOP_SEARCH));
        operationBox.setOnAction(event -> buildForm());
        idColumn.setCellValueFactory(cell -> cell.getValue().idProperty());
        operationColumn.setCellValueFactory(cell -> cell.getValue().operationProperty());
        subjectColumn.setCellValueFactory(cell -> cell.getValue().subjectProperty());
        locationColumn.setCellValueFactory(cell -> cell.getValue().locationProperty());
        dateColumn.setCellValueFactory(cell -> cell.getValue().dateProperty());
        statusColumn.setCellValueFactory(cell -> cell.getValue().statusProperty());
        detailsColumn.setCellValueFactory(cell -> cell.getValue().detailsProperty());
        recordTable.setItems(allRecords);
        recordTable.getSelectionModel().selectedItemProperty().addListener((observable, oldRow, row) -> {
            if (isDutyView() && row != null) routeDetailsLabel.setText(model.getRouteDetails(row));
        });
        updateId();
        formHint.setText("Assigned day shift roster and pending dispatch alerts are loaded.");
        messageLabel.setText("Loaded " + allRecords.size() + " saved record(s).");
        updateActionButtons("");
    }

    private void buildForm() {
        controls.clear();
        formFields.getChildren().clear();
        currentFields = List.of();
        routeDetailsLabel.setText("");
        String operation = operationBox.getValue();
        if (operation == null) return;
        currentFields = fieldsFor(operation);
        int row = 0;
        for (Field field : currentFields) {
            Control control;
            if (field.type().equals("date")) {
                DatePicker picker = new DatePicker(LocalDate.now());
                picker.setMaxWidth(Double.MAX_VALUE);
                picker.valueProperty().addListener((observable, oldDate, newDate) -> {
                    if (isDutyView()) refreshDutyRows(newDate);
                    if (newDate != null) updateZoneChoices(newDate);
                });
                control = picker;
            } else if (field.type().equals("choice")) {
                ComboBox<String> box = new ComboBox<>(FXCollections.observableArrayList(field.options()));
                box.setMaxWidth(Double.MAX_VALUE);
                if (!field.options().isEmpty()) box.getSelectionModel().selectFirst();
                control = box;
            } else {
                TextField text = new TextField();
                if (field.key().equals("start")) text.setText(LocalTime.now().withSecond(0).withNano(0).toString());
                if (field.key().equals("end")) text.setText(LocalTime.now().withSecond(0).withNano(0).toString());
                if (field.key().equals("dispatchId") && !model.getPendingDispatchIds().isEmpty())
                    text.setText(model.getPendingDispatchIds().get(0));
                control = text;
            }
            controls.put(field.key(), control);
            formFields.add(new Label(field.label() + (field.optional() ? " (optional)" : "")), 0, row);
            formFields.add(control, 1, row++);
        }
        updateActionButtons(operation);
        updateId();
        if (operation.equals(PatrolConstableModel.DUTY)) {
            setDutyTableHeaders();
            refreshDutyRows(LocalDate.now());
        } else {
            setHistoryTableHeaders();
            recordTable.setItems(allRecords);
            if (operation.equals(PatrolConstableModel.DISPATCH)) {
                formHint.setText("Pending dispatches: " + String.join(", ", model.getPendingDispatchIds()));
            } else {
                formHint.setText("Complete the selected workflow form. Required fields are validated before saving.");
            }
        }
    }

    private List<Field> fieldsFor(String operation) {
        return switch (operation) {
            case PatrolConstableModel.DUTY -> List.of(Field.date("date", "Duty date"));
            case PatrolConstableModel.PATROL_LOG -> List.of(
                    Field.choice("zone", "Assigned patrol zone", model.getAssignedZones(LocalDate.now()).toArray(String[]::new)),
                    Field.date("date", "Patrol date"), Field.text("start", "Start time (HH:mm)"),
                    Field.text("end", "End time (HH:mm)"), Field.text("observations", "Observations"),
                    Field.text("incidents", "Incidents noted"));
            case PatrolConstableModel.DISPATCH -> List.of(Field.text("dispatchId", "Pending dispatch ID"));
            case PatrolConstableModel.INCIDENT -> List.of(
                    Field.choice("type", "Incident type", "Theft", "Assault", "Public disturbance", "Other"),
                    Field.choice("severity", "Severity", "Low", "Moderate", "High"),
                    Field.text("location", "Location"), Field.text("description", "Description"),
                    Field.date("date", "Incident date"));
            case PatrolConstableModel.ARREST -> List.of(
                    Field.text("name", "Suspect name"), Field.text("age", "Age (numeric)"),
                    Field.text("address", "Address"), Field.text("location", "Arrest location"),
                    Field.text("reason", "Reason"), Field.optional("fir", "Existing FIR number (optional)"));
            case PatrolConstableModel.BACKUP -> List.of(
                    Field.text("location", "Location"),
                    Field.choice("urgency", "Urgency", "Low", "Medium", "High"),
                    Field.text("count", "Number of officers needed"));
            case PatrolConstableModel.ATTENDANCE -> List.of(
                    Field.choice("action", "Check-in/out action", "Check In", "Check Out"),
                    Field.choice("shift", "Current assigned shift", "Day"));
            case PatrolConstableModel.STOP_SEARCH -> List.of(
                    Field.text("person", "Person or vehicle details"), Field.text("location", "Location"),
                    Field.text("reason", "Reason"),
                    Field.choice("outcome", "Outcome", "Clear", "Item Seized", "Detained"),
                    Field.date("date", "Search date"));
            default -> List.of();
        };
    }

    private void updateZoneChoices(LocalDate date) {
        Control control = controls.get("zone");
        if (!(control instanceof ComboBox<?>)) return;
        @SuppressWarnings("unchecked")
        ComboBox<String> zones = (ComboBox<String>) control;
        String selected = zones.getValue();
        zones.setItems(FXCollections.observableArrayList(model.getAssignedZones(date)));
        if (selected != null && model.getAssignedZones(date).contains(selected)) zones.setValue(selected);
        else zones.getSelectionModel().selectFirst();
    }

    private void updateActionButtons(String operation) {
        boolean duty = operation.equals(PatrolConstableModel.DUTY);
        boolean dispatch = operation.equals(PatrolConstableModel.DISPATCH);
        submitButton.setVisible(!duty && !dispatch);
        submitButton.setManaged(!duty && !dispatch);
        acknowledgeButton.setVisible(duty);
        acknowledgeButton.setManaged(duty);
        viewDispatchButton.setVisible(dispatch);
        viewDispatchButton.setManaged(dispatch);
        acceptDispatchButton.setVisible(dispatch);
        acceptDispatchButton.setManaged(dispatch);
        submitButton.setText(switch (operation) {
            case PatrolConstableModel.PATROL_LOG -> "Submit Log";
            case PatrolConstableModel.INCIDENT -> "Submit Incident";
            case PatrolConstableModel.ARREST -> "Save Arrest Record";
            case PatrolConstableModel.BACKUP -> "Request Backup";
            case PatrolConstableModel.ATTENDANCE -> "Record Check In / Out";
            case PatrolConstableModel.STOP_SEARCH -> "Save Stop & Search";
            default -> "Submit";
        });
        setDutyActions(duty);
    }

    private void refreshDutyRows(LocalDate date) {
        if (date == null) {
            recordTable.setItems(FXCollections.observableArrayList());
            formHint.setText("Choose a duty date.");
            return;
        }
        ObservableList<RecordRow> dutyRows = FXCollections.observableArrayList(model.getDutyRows(date));
        recordTable.setItems(dutyRows);
        recordTable.getSelectionModel().clearSelection();
        routeDetailsLabel.setText("");
        formHint.setText(dutyRows.isEmpty() ? "No assigned patrol duties for " + date + "."
                : "Showing " + dutyRows.size() + " assigned duty/duties for " + date
                + ". Select a row to view its route.");
    }

    @FXML
    private void submit() {
        String operation = operationBox.getValue();
        if (operation == null) {
            notify("Select a patrol workflow first.");
            return;
        }
        try {
            String confirmation = model.submit(operation, collect());
            showHistory();
            updateId();
            notify(confirmation);
        } catch (IllegalArgumentException ex) {
            notify(ex.getMessage());
        } catch (IOException ex) {
            notify("Could not save this patrol record: " + ex.getMessage());
        }
    }

    @FXML
    private void acknowledgeDuty() {
        RecordRow selected = recordTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            notify("Select an assigned duty before acknowledging it.");
            return;
        }
        try {
            notify(model.acknowledgeDuty(selected));
            LocalDate date = dateFieldValue();
            refreshDutyRows(date);
            updateId();
        } catch (IllegalArgumentException ex) {
            notify(ex.getMessage());
        } catch (IOException ex) {
            notify("Could not save the duty acknowledgement: " + ex.getMessage());
        }
    }

    @FXML
    private void viewDispatch() {
        try {
            String dispatchId = textValue("dispatchId");
            notify(model.viewDispatch(dispatchId));
        } catch (IllegalArgumentException ex) {
            notify(ex.getMessage());
        }
    }

    @FXML
    private void acceptDispatch() {
        try {
            notify(model.acceptDispatch(textValue("dispatchId")));
            showHistory();
            updateId();
            formHint.setText("Pending dispatches: " + String.join(", ", model.getPendingDispatchIds()));
        } catch (IllegalArgumentException ex) {
            notify(ex.getMessage());
        } catch (IOException ex) {
            notify("Could not save the dispatch response: " + ex.getMessage());
        }
    }

    @FXML
    private void filterRecords() {
        if (isDutyView()) {
            refreshDutyRows(dateFieldValue());
            return;
        }
        String operation = operationBox.getValue();
        recordTable.setItems(operation == null ? allRecords
                : allRecords.filtered(row -> row.getOperation().equals(operation)));
        notify("Showing " + recordTable.getItems().size() + " saved record(s).");
    }

    @FXML
    private void showLatest() {
        setHistoryTableHeaders();
        recordTable.setItems(allRecords);
        if (allRecords.isEmpty()) {
            notify("There are no saved records yet.");
            return;
        }
        RecordRow latest = allRecords.get(allRecords.size() - 1);
        recordTable.getSelectionModel().select(latest);
        recordTable.scrollTo(latest);
        notify("Most recent: " + latest.getId() + " · " + latest.getOperation());
    }

    @FXML
    private void clearForm() {
        operationBox.getSelectionModel().clearSelection();
        controls.clear();
        formFields.getChildren().clear();
        currentFields = List.of();
        routeDetailsLabel.setText("");
        setDutyActions(false);
        submitButton.setVisible(true);
        submitButton.setManaged(true);
        viewDispatchButton.setVisible(false);
        viewDispatchButton.setManaged(false);
        acceptDispatchButton.setVisible(false);
        acceptDispatchButton.setManaged(false);
        setHistoryTableHeaders();
        recordTable.setItems(allRecords);
        updateId();
        notify("Form cleared.");
    }

    @FXML
    private void backToMainDashboard(ActionEvent event) throws IOException {
        DashboardNavigation.showMainDashboard(event);
    }

    private Map<String, String> collect() {
        Map<String, String> values = new LinkedHashMap<>();
        for (Field field : currentFields) {
            Control control = controls.get(field.key());
            String value;
            if (control instanceof TextField text) value = text.getText().trim();
            else if (control instanceof DatePicker picker)
                value = picker.getValue() == null ? "" : picker.getValue().toString();
            else value = Objects.toString(((ComboBox<?>) control).getValue(), "");
            if (!field.optional() && value.isBlank())
                throw new IllegalArgumentException("Complete the required field: " + field.label() + ".");
            values.put(field.key(), value);
        }
        return values;
    }

    private String textValue(String key) {
        Control control = controls.get(key);
        return control instanceof TextField text ? text.getText().trim() : "";
    }

    private LocalDate dateFieldValue() {
        Control control = controls.get("date");
        return control instanceof DatePicker picker ? picker.getValue() : LocalDate.now();
    }

    private boolean isDutyView() {
        return PatrolConstableModel.DUTY.equals(operationBox.getValue());
    }

    private void setDutyActions(boolean duty) {
        acknowledgeButton.setDisable(!duty);
        acknowledgeButton.setVisible(duty);
        acknowledgeButton.setManaged(duty);
    }

    private void showHistory() {
        setHistoryTableHeaders();
        recordTable.setItems(allRecords);
    }

    private void setDutyTableHeaders() {
        idColumn.setText("Date");
        operationColumn.setText("Shift");
        subjectColumn.setText("Patrol Zone");
        locationColumn.setText("Partner Constable");
        dateColumn.setText("Vehicle");
        statusColumn.setText("Acknowledgement");
        detailsColumn.setText("Route details");
    }

    private void setHistoryTableHeaders() {
        idColumn.setText("ID");
        operationColumn.setText("Workflow");
        subjectColumn.setText("Subject");
        locationColumn.setText("Location / Zone");
        dateColumn.setText("Date");
        statusColumn.setText("Status");
        detailsColumn.setText("Saved form data");
    }

    private void updateId() {
        idField.setText(PatrolConstableModel.PATROL_LOG.equals(operationBox.getValue())
                ? model.getNextPatrolLogId()
                : "PC-" + DashboardStore.nextId(allRecords, "PC"));
    }

    private void notify(String message) {
        messageLabel.setText(message);
    }
}
