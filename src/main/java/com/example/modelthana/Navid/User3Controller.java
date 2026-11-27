package com.example.modelthana.Navid;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;

import java.io.IOException;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.*;

public class User3Controller {
    private static final String FILE = "user3_records.txt";
    private static final String PREFIX = "PC";
    private static final List<String> ZONES = List.of("Central Zone", "Riverside Zone", "Market Zone");
    private static final String ASSIGNED_SHIFT = "Day";
    private static final Map<String, String> PENDING_DISPATCHES = Map.of("DSP-1001", "Central Zone");

    @FXML private TextField idField;
    @FXML private ComboBox<String> operationBox;
    @FXML private GridPane formFields;
    @FXML private Label messageLabel, formHint;
    @FXML private TableView<RecordRow> recordTable;
    @FXML private TableColumn<RecordRow, String> idColumn, operationColumn, subjectColumn,
            locationColumn, dateColumn, statusColumn, detailsColumn;

    private final ObservableList<RecordRow> allRecords = DashboardStore.load(FILE);
    private final Map<String, Control> controls = new LinkedHashMap<>();
    private List<Field> currentFields = List.of();

    private record Field(String key, String label, String type, List<String> options, boolean optional) {
        static Field text(String key, String label) { return new Field(key, label, "text", List.of(), false); }
        static Field optional(String key, String label) { return new Field(key, label, "text", List.of(), true); }
        static Field date(String key, String label) { return new Field(key, label, "date", List.of(), false); }
        static Field choice(String key, String label, String... options) {
            return new Field(key, label, "choice", List.of(options), false);
        }
    }

    @FXML public void initialize() {
        operationBox.setItems(FXCollections.observableArrayList(
                "Duty Assignment", "Patrol Log", "Dispatch Response", "Incident Report",
                "Arrest Record", "Backup Request", "Shift Attendance", "Stop and Search"));
        operationBox.setOnAction(event -> buildForm());
        idColumn.setCellValueFactory(cell -> cell.getValue().idProperty());
        operationColumn.setCellValueFactory(cell -> cell.getValue().operationProperty());
        subjectColumn.setCellValueFactory(cell -> cell.getValue().subjectProperty());
        locationColumn.setCellValueFactory(cell -> cell.getValue().locationProperty());
        dateColumn.setCellValueFactory(cell -> cell.getValue().dateProperty());
        statusColumn.setCellValueFactory(cell -> cell.getValue().statusProperty());
        detailsColumn.setCellValueFactory(cell -> cell.getValue().detailsProperty());
        recordTable.setItems(allRecords);
        updateId();
        formHint.setText("Assigned today: " + String.join(", ", ZONES) + " · " + ASSIGNED_SHIFT
                + " shift. Pending dispatch queue: DSP-1001 (Central Zone).");
        messageLabel.setText("Loaded " + allRecords.size() + " saved record(s).");
    }

    @FXML private void backToMainDashboard(ActionEvent event) throws IOException {
        DashboardNavigation.showMainDashboard(event);
    }

    private void buildForm() {
        controls.clear();
        formFields.getChildren().clear();
        String operation = operationBox.getValue();
        if (operation == null) return;
        currentFields = switch (operation) {
            case "Duty Assignment" -> List.of(Field.choice("action", "Duty action", "View assigned routes", "Acknowledge assignment"),
                    Field.choice("zone", "Assigned route", ZONES.toArray(String[]::new)), Field.date("date", "Duty date"));
            case "Patrol Log" -> List.of(Field.choice("zone", "Assigned zone", ZONES.toArray(String[]::new)),
                    Field.date("date", "Patrol date"),
                    Field.text("start", "Start time (HH:mm)"), Field.text("end", "End time (HH:mm)"),
                    Field.text("observations", "Patrol observations"));
            case "Dispatch Response" -> List.of(Field.text("dispatchId", "Pending dispatch ID"));
            case "Incident Report" -> List.of(Field.date("date", "Incident date"), Field.text("location", "Location"),
                    Field.choice("type", "Incident type", "Theft", "Assault", "Public disturbance", "Other"),
                    Field.choice("severity", "Severity", "Low", "Moderate", "High"), Field.text("details", "Incident details"));
            case "Arrest Record" -> List.of(Field.text("name", "Person arrested"), Field.text("age", "Age (numeric)"),
                    Field.text("location", "Location"), Field.date("date", "Arrest date"),
                    Field.optional("fir", "Existing FIR ID (optional)"), Field.text("details", "Arrest / detention details"));
            case "Backup Request" -> List.of(Field.text("location", "Request location"), Field.text("count", "Additional officers (positive number)"),
                    Field.text("reason", "Reason"));
            case "Shift Attendance" -> List.of(Field.choice("action", "Attendance action", "Check in", "Check out"),
                    Field.choice("shift", "Assigned shift", "Day", "Night"), Field.date("date", "Shift date"));
            case "Stop and Search" -> List.of(Field.date("date", "Search date"), Field.text("location", "Location"),
                    Field.text("person", "Person / vehicle searched"), Field.text("reason", "Reason"),
                    Field.choice("outcome", "Outcome", "No action", "Warning", "Seizure", "Arrest"));
            default -> List.of();
        };
        int row = 0;
        for (Field field : currentFields) {
            Label label = new Label(field.label() + (field.optional() ? " (optional)" : ""));
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
                if (field.key().equals("dispatchId")) text.setText("DSP-1001");
                if (field.key().equals("start")) text.setText(LocalTime.now().withSecond(0).withNano(0).toString());
                if (field.key().equals("end")) text.setText(LocalTime.now().withSecond(0).withNano(0).toString());
                control = text;
            }
            controls.put(field.key(), control);
            formFields.add(label, 0, row);
            formFields.add(control, 1, row++);
        }
        updateId();
    }

    @FXML private void submit() {
        String operation = operationBox.getValue();
        if (operation == null) { notify("Select an operation first."); return; }
        try {
            Map<String, String> data = collect();
            switch (operation) {
                case "Duty Assignment" -> duty(data);
                case "Patrol Log" -> patrol(data);
                case "Dispatch Response" -> dispatch(data);
                case "Incident Report" -> incident(data);
                case "Arrest Record" -> arrest(data);
                case "Backup Request" -> backup(data);
                case "Shift Attendance" -> attendance(data);
                case "Stop and Search" -> stopSearch(data);
                default -> notify("Choose a supported patrol workflow.");
            }
        } catch (IllegalArgumentException ex) {
            notify(ex.getMessage());
        } catch (IOException ex) {
            notify("Could not save this record: " + ex.getMessage());
        }
    }

    private Map<String, String> collect() {
        Map<String, String> values = new LinkedHashMap<>();
        for (Field field : currentFields) {
            Control control = controls.get(field.key());
            String value = "";
            if (control instanceof TextField text) value = text.getText().trim();
            else if (control instanceof DatePicker picker) value = picker.getValue() == null ? "" : picker.getValue().toString();
            else if (control instanceof ComboBox<?> box) value = Objects.toString(box.getValue(), "");
            if (!field.optional() && value.isBlank()) throw new IllegalArgumentException("Complete the required field: " + field.label() + ".");
            values.put(field.key(), value);
        }
        return values;
    }

    private void duty(Map<String, String> data) throws IOException {
        LocalDate date = LocalDate.parse(data.get("date"));
        String zone = data.get("zone");
        if (data.get("action").equals("View assigned routes")) {
            notify("Assigned route: " + zone + " · " + ASSIGNED_SHIFT + " shift · " + date + ". Route acknowledged status: "
                    + (DashboardStore.hasValue(allRecords, "Duty Assignment", "ack", zone + "|" + date) ? "acknowledged." : "not yet acknowledged."));
            return;
        }
        data.put("ack", zone + "|" + date);
        if (DashboardStore.hasValue(allRecords, "Duty Assignment", "ack", zone + "|" + date))
            throw new IllegalArgumentException("This assigned route has already been acknowledged.");
        save("Duty Assignment", zone, zone, date, "Acknowledged", data,
                "Assignment acknowledged: " + zone + " on " + date + ".");
    }

    private void patrol(Map<String, String> data) throws IOException {
        if (!ZONES.contains(data.get("zone"))) throw new IllegalArgumentException("Patrol logs are allowed only for an assigned zone.");
        LocalTime start = parseTime(data.get("start"), "Start time");
        LocalTime end = parseTime(data.get("end"), "End time");
        if (!end.isAfter(start)) throw new IllegalArgumentException("End time must be after start time.");
        save("Patrol Log", data.get("observations"), data.get("zone"), LocalDate.parse(data.get("date")), "Completed", data,
                "Patrol log saved for " + data.get("zone") + ".");
    }

    private void dispatch(Map<String, String> data) throws IOException {
        String dispatchId = data.get("dispatchId").trim();
        if (!dispatchId.matches("DSP-[A-Za-z0-9-]+")) throw new IllegalArgumentException("Enter a valid dispatch ID (for example DSP-1001).");
        if (!PENDING_DISPATCHES.containsKey(dispatchId))
            throw new IllegalArgumentException("This dispatch is not in the pending queue.");
        if (DashboardStore.hasValue(allRecords, "Dispatch Response", "dispatchId", dispatchId))
            throw new IllegalArgumentException("This dispatch has already been accepted or completed; only pending dispatches can be accepted.");
        data.put("dispatchId", dispatchId);
        save("Dispatch Response", dispatchId, PENDING_DISPATCHES.get(dispatchId), LocalDate.now(), "En Route", data,
                "Dispatch " + dispatchId + " accepted. Status: En Route.");
    }

    private void incident(Map<String, String> data) throws IOException {
        LocalDate date = LocalDate.parse(data.get("date"));
        String fir = nextFirId();
        data.put("firId", fir);
        save("Incident Report", data.get("type"), data.get("location"), date, "Forwarded to Duty Officer · " + fir, data,
                "Incident forwarded to Duty Officer. FIR reference: " + fir + ".");
    }

    private void arrest(Map<String, String> data) throws IOException {
        int age = parsePositiveOrZero(data.get("age"), "Age");
        if (age < 18 || age > 120) throw new IllegalArgumentException("Enter a valid age from 18 to 120.");
        if (!data.get("fir").isBlank() && !firExists(data.get("fir")))
            throw new IllegalArgumentException("That FIR ID does not exist in saved incident history.");
        data.put("age", Integer.toString(age));
        save("Arrest Record", data.get("name"), data.get("location"), LocalDate.parse(data.get("date")),
                "Detention recorded · Duty Officer notified", data, "Arrest / detention recorded. Duty Officer notified.");
    }

    private void backup(Map<String, String> data) throws IOException {
        int count = parsePositiveOrZero(data.get("count"), "Additional officer count");
        if (count < 1) throw new IllegalArgumentException("Request at least one additional officer.");
        data.put("count", Integer.toString(count));
        save("Backup Request", count + " officer(s)", data.get("location"), LocalDate.now(),
                "Alert sent · Pending", data, "Backup request saved and Duty Officer alerted for " + count + " officer(s).");
    }

    private void attendance(Map<String, String> data) throws IOException {
        LocalDate date = LocalDate.parse(data.get("date"));
        if (!data.get("shift").equals(ASSIGNED_SHIFT) || !date.equals(LocalDate.now()))
            throw new IllegalArgumentException("Attendance is restricted to your assigned " + ASSIGNED_SHIFT + " shift for today.");
        String action = data.get("action");
        boolean checkIn = action.equals("Check in");
        boolean hasCheckIn = allRecords.stream().anyMatch(row -> row.getOperation().equals("Shift Attendance")
                && row.getStatus().startsWith("Checked in") && row.getDate().equals(date.toString())
                && "Day".equals(DashboardStore.decode(row.getDetails()).get("shift")));
        boolean hasCheckOut = allRecords.stream().anyMatch(row -> row.getOperation().equals("Shift Attendance")
                && row.getStatus().startsWith("Checked out") && row.getDate().equals(date.toString())
                && "Day".equals(DashboardStore.decode(row.getDetails()).get("shift")));
        if (checkIn && hasCheckIn) throw new IllegalArgumentException("You have already checked in for this shift.");
        if (!checkIn && (!hasCheckIn || hasCheckOut)) throw new IllegalArgumentException("Check in first; check-out can be recorded only once.");
        LocalTime now = LocalTime.now().withSecond(0).withNano(0);
        data.put("time", now.toString());
        String status = checkIn ? "Checked in · " + now : "Checked out · " + now;
        String confirmation = "Shift " + action.toLowerCase(Locale.ROOT) + " recorded at " + now + ".";
        if (!checkIn) {
            RecordRow prior = allRecords.stream().filter(row -> row.getOperation().equals("Shift Attendance")
                    && row.getStatus().startsWith("Checked in") && row.getDate().equals(date.toString())).findFirst().orElseThrow();
            LocalTime in = LocalTime.parse(DashboardStore.decode(prior.getDetails()).get("time"));
            long minutes = Duration.between(in, now).toMinutes();
            if (minutes <= 0) throw new IllegalArgumentException("Check-out time must be after check-in.");
            status += " · " + (minutes / 60) + "h " + (minutes % 60) + "m";
            confirmation += " Hours worked: " + (minutes / 60) + "h " + (minutes % 60) + "m.";
        }
        save("Shift Attendance", action, data.get("shift"), date, status, data, confirmation);
    }

    private void stopSearch(Map<String, String> data) throws IOException {
        save("Stop and Search", data.get("person"), data.get("location"), LocalDate.parse(data.get("date")),
                data.get("outcome"), data, "Stop-and-search record saved with outcome: " + data.get("outcome") + ".");
    }

    private void save(String operation, String subject, String location, LocalDate date, String status,
                      Map<String, String> data, String confirmation) throws IOException {
        RecordRow row = DashboardStore.record(allRecords, PREFIX, operation, subject, location, date, status, data, FILE);
        recordTable.setItems(allRecords);
        recordTable.getSelectionModel().select(row);
        recordTable.scrollTo(row);
        updateId();
        notify(confirmation);
    }

    private boolean firExists(String fir) {
        return allRecords.stream().filter(row -> row.getOperation().equals("Incident Report"))
                .map(row -> DashboardStore.decode(row.getDetails()).get("firId"))
                .anyMatch(fir::equals);
    }

    private String nextFirId() {
        int next = 1;
        for (RecordRow row : allRecords) {
            String fir = DashboardStore.decode(row.getDetails()).get("firId");
            if (fir == null || !fir.startsWith("FIR-")) continue;
            try { next = Math.max(next, Integer.parseInt(fir.substring(4)) + 1); }
            catch (NumberFormatException ignored) { }
        }
        return "FIR-" + next;
    }

    @FXML private void filterRecords() {
        String operation = operationBox.getValue();
        if (operation == null) recordTable.setItems(allRecords);
        else recordTable.setItems(allRecords.filtered(row -> row.getOperation().equals(operation)));
        notify("Showing " + recordTable.getItems().size() + " saved record(s).");
    }

    @FXML private void showLatest() {
        recordTable.setItems(allRecords);
        if (allRecords.isEmpty()) { notify("There are no saved records yet."); return; }
        RecordRow row = allRecords.get(allRecords.size() - 1);
        recordTable.getSelectionModel().select(row);
        recordTable.scrollTo(row);
        notify("Most recent: " + row.getId() + " · " + row.getOperation());
    }

    @FXML private void clearForm() {
        operationBox.getSelectionModel().clearSelection();
        controls.clear();
        formFields.getChildren().clear();
        currentFields = List.of();
        updateId();
        notify("Form cleared.");
    }

    private void updateId() { idField.setText(PREFIX + "-" + DashboardStore.nextId(allRecords, PREFIX)); }
    private void notify(String message) { messageLabel.setText(message); }
    private static LocalTime parseTime(String value, String label) {
        try { return LocalTime.parse(value); }
        catch (DateTimeParseException ex) { throw new IllegalArgumentException(label + " must use 24-hour HH:mm format."); }
    }
    private static int parsePositiveOrZero(String value, String label) {
        try { return Integer.parseInt(value); }
        catch (NumberFormatException ex) { throw new IllegalArgumentException(label + " must be a whole number."); }
    }
}
