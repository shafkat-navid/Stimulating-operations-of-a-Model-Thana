package com.example.modelthana.navid.controller;

import com.example.modelthana.navid.DashboardNavigation;
import com.example.modelthana.navid.model.DashboardStore;
import com.example.modelthana.navid.model.RecordRow;
import com.example.modelthana.navid.model.TrafficSergeantModel;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class TrafficSergeantController {
    @FXML private TextField idField;
    @FXML private ComboBox<String> operationBox;
    @FXML private GridPane formFields;
    @FXML private Label formHint;
    @FXML private Button submitButton, viewTicketButton;
    @FXML private BarChart<String, Number> reportChart;
    @FXML private TableView<RecordRow> recordTable;
    @FXML private TableColumn<RecordRow, String> idColumn, operationColumn, subjectColumn,
            locationColumn, dateColumn, statusColumn, detailsColumn;

    private final TrafficSergeantModel model = new TrafficSergeantModel();
    private final ObservableList<RecordRow> allRecords = model.getRecords();
    private final Map<String, Control> controls = new LinkedHashMap<>();
    private List<Field> currentFields = List.of();
    private String viewedTicketId;

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
                TrafficSergeantModel.TICKET, TrafficSergeantModel.ACCIDENT,
                TrafficSergeantModel.DEPLOYMENT, TrafficSergeantModel.VEHICLE_LOOKUP,
                TrafficSergeantModel.PAYMENT, TrafficSergeantModel.ROAD_STATUS,
                TrafficSergeantModel.IMPOUND, TrafficSergeantModel.REPORT));
        operationBox.setOnAction(event -> buildForm());
        idColumn.setCellValueFactory(cell -> cell.getValue().idProperty());
        operationColumn.setCellValueFactory(cell -> cell.getValue().operationProperty());
        subjectColumn.setCellValueFactory(cell -> cell.getValue().subjectProperty());
        locationColumn.setCellValueFactory(cell -> cell.getValue().locationProperty());
        dateColumn.setCellValueFactory(cell -> cell.getValue().dateProperty());
        statusColumn.setCellValueFactory(cell -> cell.getValue().statusProperty());
        detailsColumn.setCellValueFactory(cell -> cell.getValue().detailsProperty());
        recordTable.setItems(allRecords);
        reportChart.setVisible(false);
        reportChart.setManaged(false);
        viewTicketButton.setVisible(false);
        viewTicketButton.setManaged(false);
        updateId();
        formHint.setText("Vehicle registry: DHK-1234, CTG-5678, KHL-9012. Select a traffic workflow.");
    }

    private void buildForm() {
        controls.clear();
        formFields.getChildren().clear();
        currentFields = List.of();
        viewedTicketId = null;
        reportChart.setVisible(false);
        reportChart.setManaged(false);
        String operation = operationBox.getValue();
        if (operation == null) return;
        viewedTicketId = null;
        currentFields = fieldsFor(operation);
        int row = 0;
        for (Field field : currentFields) {
            Control control;
            if (field.type().equals("date")) {
                DatePicker picker = new DatePicker(LocalDate.now());
                picker.setMaxWidth(Double.MAX_VALUE);
                control = picker;
            } else if (field.type().equals("choice")) {
                ComboBox<String> box = new ComboBox<>(FXCollections.observableArrayList(field.options()));
                box.setMaxWidth(Double.MAX_VALUE);
                if (!field.options().isEmpty()) box.getSelectionModel().selectFirst();
                control = box;
            } else {
                TextField text = new TextField();
                if (field.key().equals("time") || field.key().equals("clearanceTime"))
                    text.setText(LocalTime.now().withSecond(0).withNano(0).toString());
                control = text;
            }
            controls.put(field.key(), control);
            formFields.add(new Label(field.label() + (field.optional() ? " (optional)" : "")), 0, row);
            formFields.add(control, 1, row++);
        }
        boolean payment = operation.equals(TrafficSergeantModel.PAYMENT);
        submitButton.setVisible(!payment);
        submitButton.setManaged(!payment);
        viewTicketButton.setVisible(payment);
        viewTicketButton.setManaged(payment);
        viewTicketButton.setDisable(false);
        viewTicketButton.setText("View Ticket");
        submitButton.setText(buttonText(operation));
        if (TrafficSergeantModel.DEPLOYMENT.equals(operation)) {
            recordTable.setItems(FXCollections.observableArrayList(model.getDeployments()));
        } else if (TrafficSergeantModel.ROAD_STATUS.equals(operation)) {
            recordTable.setItems(FXCollections.observableArrayList(model.getCurrentRoadStatuses()));
        } else if (!TrafficSergeantModel.REPORT.equals(operation)
                && !TrafficSergeantModel.VEHICLE_LOOKUP.equals(operation)) {
            recordTable.setItems(allRecords);
        }
        updateId();
        formHint.setText(operationHint(operation));
    }

    private List<Field> fieldsFor(String operation) {
        return switch (operation) {
            case TrafficSergeantModel.TICKET -> List.of(
                    Field.text("vehicle", "Vehicle registration number"), Field.text("driver", "Driver name"),
                    Field.text("license", "License number"),
                    Field.choice("violation", "Violation type", model.getViolationTypes().toArray(String[]::new)),
                    Field.text("location", "Violation location"), Field.date("date", "Ticket date"),
                    Field.text("time", "Ticket time (HH:mm)"));
            case TrafficSergeantModel.ACCIDENT -> List.of(
                    Field.text("location", "Accident location"), Field.date("date", "Date"),
                    Field.text("time", "Time (HH:mm)"), Field.text("vehicles", "Vehicles involved"),
                    Field.choice("severity", "Severity", "Minor", "Major", "Fatal"),
                    Field.text("injuries", "Injuries (or None)"), Field.text("description", "Description"));
            case TrafficSergeantModel.DEPLOYMENT -> List.of(
                    Field.choice("point", "Traffic point", model.getTrafficPoints().toArray(String[]::new)),
                    Field.choice("constable", "Constable on duty", model.getOnDutyConstables().toArray(String[]::new)),
                    Field.choice("shift", "Shift", "Day", "Evening", "Night"), Field.date("date", "Deployment date"));
            case TrafficSergeantModel.VEHICLE_LOOKUP -> List.of(Field.text("vehicle", "Vehicle registration number"));
            case TrafficSergeantModel.PAYMENT -> List.of(Field.text("ticketId", "Ticket number"));
            case TrafficSergeantModel.ROAD_STATUS -> List.of(
                    Field.text("road", "Road name"), Field.text("cause", "Cause"),
                    Field.choice("status", "Status", "Closed", "Congested", "Diverted"),
                    Field.optional("diversion", "Diversion route"), Field.date("clearanceDate", "Expected clearance date"),
                    Field.text("clearanceTime", "Expected clearance time (HH:mm)"));
            case TrafficSergeantModel.IMPOUND -> List.of(
                    Field.text("vehicle", "Vehicle registration number"), Field.text("reason", "Reason"),
                    Field.text("yard", "Impound yard location"));
            case TrafficSergeantModel.REPORT -> List.of(
                    Field.date("from", "Start date"), Field.date("to", "End date"),
                    Field.choice("violation", "Violation type", reportViolationTypes()));
            default -> List.of();
        };
    }

    private String[] reportViolationTypes() {
        String[] result = new String[model.getViolationTypes().size() + 1];
        result[0] = "All";
        for (int i = 0; i < model.getViolationTypes().size(); i++) result[i + 1] = model.getViolationTypes().get(i);
        return result;
    }

    @FXML
    private void submit() {
        String operation = operationBox.getValue();
        if (operation == null) {
            notify("Select a traffic workflow first.");
            return;
        }
        try {
            Map<String, String> data = collect();
            if (TrafficSergeantModel.PAYMENT.equals(operation)
                    && !Objects.equals(viewedTicketId, data.get("ticketId")))
                throw new IllegalArgumentException("View an unpaid ticket before confirming its payment.");
            switch (operation) {
                case TrafficSergeantModel.TICKET -> notify(model.issueTicket(data));
                case TrafficSergeantModel.ACCIDENT -> notify(model.saveAccidentReport(data));
                case TrafficSergeantModel.DEPLOYMENT -> notify(model.assignDeployment(data));
                case TrafficSergeantModel.PAYMENT -> notify(model.recordPayment(data));
                case TrafficSergeantModel.ROAD_STATUS -> notify(model.publishRoadStatus(data));
                case TrafficSergeantModel.IMPOUND -> notify(model.impoundVehicle(data));
                case TrafficSergeantModel.REPORT -> generateReport(data);
                case TrafficSergeantModel.VEHICLE_LOOKUP -> lookupVehicle();
                default -> throw new IllegalArgumentException("Select a valid traffic workflow.");
            }
            recordTable.setItems(allRecords);
            updateId();
        } catch (IllegalArgumentException ex) {
            showError(ex.getMessage());
        } catch (IOException ex) {
            showError("Could not save this traffic record: " + ex.getMessage());
        }
    }

    private void lookupVehicle() {
        try {
            TrafficSergeantModel.VehicleLookup lookup = model.lookupVehicle(textValue("vehicle"));
            recordTable.setItems(FXCollections.observableArrayList(lookup.rows()));
            notify(lookup.message());
        } catch (IllegalArgumentException ex) {
            showError(ex.getMessage());
        }
    }

    @FXML
    private void viewTicket() {
        try {
            TrafficSergeantModel.TicketPaymentDetails ticket = model.viewUnpaidTicket(textValue("ticketId"));
            viewedTicketId = ticket.ticketId();
            Control idControl = controls.get("ticketId");
            if (idControl instanceof TextField ticketIdField) ticketIdField.setEditable(false);
            formHint.setText("Ticket " + ticket.ticketId() + " · Vehicle " + ticket.vehicle()
                    + " · " + ticket.violation() + " · Fine " + ticket.fine().toPlainString() + " (Unpaid).");
            addPaymentFields();
            submitButton.setVisible(true);
            submitButton.setManaged(true);
            submitButton.setText("Confirm Payment");
            viewTicketButton.setText("Ticket Verified");
            viewTicketButton.setDisable(true);
            notify("Ticket details loaded. Enter the amount received and payment method.");
        } catch (IllegalArgumentException ex) {
            showError(ex.getMessage());
        }
    }

    private void addPaymentFields() {
        List<Field> fields = new java.util.ArrayList<>(currentFields);
        fields.add(Field.text("amount", "Amount received"));
        fields.add(Field.choice("method", "Payment method", "Cash", "Card", "Online transfer"));
        int row = formFields.getRowCount();
        for (Field field : fields.subList(currentFields.size(), fields.size())) {
            Control control;
            if (field.type().equals("choice")) {
                ComboBox<String> box = new ComboBox<>(FXCollections.observableArrayList(field.options()));
                box.setMaxWidth(Double.MAX_VALUE);
                box.getSelectionModel().selectFirst();
                control = box;
            } else {
                control = new TextField();
            }
            controls.put(field.key(), control);
            formFields.add(new Label(field.label()), 0, row);
            formFields.add(control, 1, row++);
        }
        currentFields = List.copyOf(fields);
    }

    private void generateReport(Map<String, String> data) {
        LocalDate from = LocalDate.parse(data.get("from"));
        LocalDate to = LocalDate.parse(data.get("to"));
        TrafficSergeantModel.ReportSummary summary =
                model.generateReport(from, to, data.get("violation"));
        recordTable.setItems(FXCollections.observableArrayList(summary.matchingTickets()));
        reportChart.getData().clear();
        XYChart.Series<String, Number> ticketCounts = new XYChart.Series<>();
        ticketCounts.setName("Tickets");
        ticketCounts.getData().add(new XYChart.Data<>("Paid", summary.paidTickets()));
        ticketCounts.getData().add(new XYChart.Data<>("Unpaid", summary.unpaidTickets()));
        reportChart.getData().add(ticketCounts);
        reportChart.setVisible(true);
        reportChart.setManaged(true);
        notify(summary.message());
    }

    @FXML
    private void filterRecords() {
        String operation = operationBox.getValue();
        recordTable.setItems(operation == null ? allRecords
                : allRecords.filtered(row -> row.getOperation().equals(operation)));
        notify("Showing " + recordTable.getItems().size() + " saved record(s).");
    }

    @FXML
    private void showLatest() {
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
        reportChart.getData().clear();
        reportChart.setVisible(false);
        reportChart.setManaged(false);
        submitButton.setVisible(true);
        submitButton.setManaged(true);
        viewTicketButton.setVisible(false);
        viewTicketButton.setManaged(false);
        viewTicketButton.setDisable(false);
        viewTicketButton.setText("View Ticket");
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

    private String buttonText(String operation) {
        return switch (operation) {
            case TrafficSergeantModel.TICKET -> "Issue Ticket";
            case TrafficSergeantModel.ACCIDENT -> "Save Accident Report";
            case TrafficSergeantModel.DEPLOYMENT -> "Assign";
            case TrafficSergeantModel.PAYMENT -> "View Ticket";
            case TrafficSergeantModel.ROAD_STATUS -> "Publish";
            case TrafficSergeantModel.IMPOUND -> "Impound";
            case TrafficSergeantModel.REPORT -> "Generate Report";
            default -> "Look Up Vehicle";
        };
    }

    private String operationHint(String operation) {
        return switch (operation) {
            case TrafficSergeantModel.TICKET -> "Fine and demerit points are retrieved from the violation table; new tickets start Unpaid.";
            case TrafficSergeantModel.ACCIDENT -> "Fatal reports trigger an FIR request to the Duty Officer; the hotspot and OC notification are updated.";
            case TrafficSergeantModel.DEPLOYMENT -> "Available traffic points and on-duty constables are loaded. Same-shift assignments are checked.";
            case TrafficSergeantModel.VEHICLE_LOOKUP -> "Lookup is read-only and shows registered owner and violation history.";
            case TrafficSergeantModel.PAYMENT -> "Only existing Unpaid tickets can be paid; the received amount must exactly match the fine.";
            case TrafficSergeantModel.ROAD_STATUS -> "Enter expected clearance time and provide a diversion route when status is Diverted.";
            case TrafficSergeantModel.IMPOUND -> "Only registered vehicles that are not already impounded can be recorded.";
            case TrafficSergeantModel.REPORT -> "The report filters saved tickets and displays ticket counts, fines and paid/unpaid totals.";
            default -> "Complete the selected workflow form.";
        };
    }

    private void updateId() {
        idField.setText("TS-" + DashboardStore.nextId(allRecords, "TS"));
    }

    private void notify(String message) {
        new Alert(Alert.AlertType.INFORMATION, message, ButtonType.OK).showAndWait();
    }

    private void showError(String message) {
        new Alert(Alert.AlertType.ERROR, message, ButtonType.OK).showAndWait();
    }
}
