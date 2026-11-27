package com.example.modelthana.Navid;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.*;

public class User4Controller {
    private static final String FILE = "user4_records.txt";
    private static final String PREFIX = "TS";
    private static final List<String> VIOLATIONS = List.of("Speeding", "Illegal parking", "Signal violation", "No helmet", "Other");
    private static final Map<String, String> VEHICLES = Map.of(
            "DHK-1234", "A. Rahman", "CTG-5678", "S. Chowdhury", "KHL-9012", "M. Islam");

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
        static Field date(String key, String label) { return new Field(key, label, "date", List.of(), false); }
        static Field optional(String key, String label) { return new Field(key, label, "text", List.of(), true); }
        static Field choice(String key, String label, String... options) {
            return new Field(key, label, "choice", List.of(options), false);
        }
    }

    @FXML public void initialize() {
        operationBox.setItems(FXCollections.observableArrayList(
                "Traffic Ticket", "Accident Report", "Traffic Deployment", "Vehicle Lookup",
                "Fine Payment", "Road Status", "Vehicle Impound", "Traffic Violation Report"));
        operationBox.setOnAction(event -> buildForm());
        idColumn.setCellValueFactory(cell -> cell.getValue().idProperty());
        operationColumn.setCellValueFactory(cell -> cell.getValue().operationProperty());
        subjectColumn.setCellValueFactory(cell -> cell.getValue().subjectProperty());
        locationColumn.setCellValueFactory(cell -> cell.getValue().locationProperty());
        dateColumn.setCellValueFactory(cell -> cell.getValue().dateProperty());
        statusColumn.setCellValueFactory(cell -> cell.getValue().statusProperty());
        detailsColumn.setCellValueFactory(cell -> cell.getValue().detailsProperty());
        recordTable.setItems(allRecords);
        refreshPaidTicketStatuses();
        updateId();
        formHint.setText("Registry records: DHK-1234, CTG-5678, KHL-9012. Select a traffic workflow to open its form.");
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
            case "Traffic Ticket" -> List.of(Field.text("vehicle", "Vehicle registration"),
                    Field.text("owner", "Owner name"), Field.choice("violation", "Violation type", VIOLATIONS.toArray(String[]::new)),
                    Field.date("date", "Ticket date"), Field.text("fine", "Fine amount"), Field.text("demerits", "Demerit points"),
                    Field.text("location", "Violation location"));
            case "Accident Report" -> List.of(Field.date("date", "Accident date"), Field.text("location", "Location"),
                    Field.text("parties", "Parties / vehicles involved"), Field.choice("severity", "Severity", "Non-fatal", "Fatal"),
                    Field.text("details", "Accident details"));
            case "Traffic Deployment" -> List.of(Field.date("date", "Deployment date"), Field.text("point", "Deployment point"),
                    Field.text("constable", "Constable ID"), Field.choice("shift", "Shift", "Day", "Evening", "Night"));
            case "Vehicle Lookup" -> List.of(Field.text("vehicle", "Vehicle registration"));
            case "Fine Payment" -> List.of(Field.text("ticketId", "Existing ticket ID"), Field.text("amount", "Exact unpaid amount"),
                    Field.choice("method", "Payment method", "Cash", "Card", "Online transfer"));
            case "Road Status" -> List.of(Field.date("date", "Status date"), Field.text("location", "Road / location"),
                    Field.choice("condition", "Road status", "Clear", "Congested", "Closed"),
                    Field.optional("diversion", "Diversion details (if applicable)"));
            case "Vehicle Impound" -> List.of(Field.text("vehicle", "Existing vehicle registration"),
                    Field.date("date", "Impound date"), Field.text("time", "Impound time (HH:mm)"),
                    Field.text("reason", "Impound reason"));
            case "Traffic Violation Report" -> List.of(Field.date("from", "From date"), Field.date("to", "To date"),
                    Field.choice("violation", "Violation type", concat(new String[]{"All"}, VIOLATIONS.toArray(String[]::new))));
            default -> List.of();
        };
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
                if (field.key().equals("date")) text.setText(LocalDate.now().toString());
                if (field.key().equals("time")) text.setText(LocalTime.now().withSecond(0).withNano(0).toString());
                control = text;
            }
            controls.put(field.key(), control);
            formFields.add(new Label(field.label() + (field.optional() ? " (optional)" : "")), 0, row);
            formFields.add(control, 1, row++);
        }
        updateId();
    }

    @FXML private void submit() {
        String operation = operationBox.getValue();
        if (operation == null) { notify("Select a traffic workflow first."); return; }
        try {
            Map<String, String> data = collect();
            switch (operation) {
                case "Traffic Ticket" -> issueTicket(data);
                case "Accident Report" -> accident(data);
                case "Traffic Deployment" -> deploy(data);
                case "Vehicle Lookup" -> lookup(data);
                case "Fine Payment" -> pay(data);
                case "Road Status" -> roadStatus(data);
                case "Vehicle Impound" -> impound(data);
                case "Traffic Violation Report" -> report(data);
                default -> notify("Choose a supported traffic workflow.");
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
            String value;
            if (control instanceof TextField text) value = text.getText().trim();
            else if (control instanceof DatePicker picker) value = picker.getValue() == null ? "" : picker.getValue().toString();
            else value = Objects.toString(((ComboBox<?>) control).getValue(), "");
            if (!field.optional() && value.isBlank()) throw new IllegalArgumentException("Complete the required field: " + field.label() + ".");
            values.put(field.key(), value);
        }
        return values;
    }

    private void issueTicket(Map<String, String> data) throws IOException {
        String vehicle = normalizeVehicle(data.get("vehicle"));
        if (!knownVehicle(vehicle)) throw new IllegalArgumentException("Vehicle is not in the registered vehicle list or saved ticket history.");
        if (data.get("owner").isBlank()) throw new IllegalArgumentException("Owner name is required.");
        String registeredOwner = registeredOwner(vehicle);
        if (!registeredOwner.isBlank() && !registeredOwner.equalsIgnoreCase(data.get("owner")))
            throw new IllegalArgumentException("Owner name does not match the registered vehicle owner.");
        LocalDate date = LocalDate.parse(data.get("date"));
        if (date.isAfter(LocalDate.now())) throw new IllegalArgumentException("Ticket date cannot be in the future.");
        BigDecimal fine = amount(data.get("fine"), true);
        int demerits = wholeNumber(data.get("demerits"), "Demerit points");
        if (demerits < 0 || demerits > 100) throw new IllegalArgumentException("Demerit points must be between 0 and 100.");
        data.put("vehicle", vehicle);
        data.put("fine", fine.toPlainString());
        data.put("demerits", Integer.toString(demerits));
        save("Traffic Ticket", vehicle, data.get("location"), date, "Unpaid · Fine " + fine.toPlainString(), data,
                "Ticket issued: " + idField.getText() + " · " + vehicle + " · unpaid fine " + fine.toPlainString() + ".");
    }

    private void accident(Map<String, String> data) throws IOException {
        LocalDate date = LocalDate.parse(data.get("date"));
        if (date.isAfter(LocalDate.now())) throw new IllegalArgumentException("Accident date cannot be in the future.");
        boolean fatal = data.get("severity").equals("Fatal");
        String status = fatal ? "FIR requested · Duty Officer notified" : "Accident recorded";
        save("Accident Report", data.get("parties"), data.get("location"), date, status, data,
                fatal ? "Fatal accident recorded. Duty Officer notified and FIR creation requested."
                        : "Accident report recorded.");
    }

    private void deploy(Map<String, String> data) throws IOException {
        LocalDate date = LocalDate.parse(data.get("date"));
        if (date.isBefore(LocalDate.now())) throw new IllegalArgumentException("Deployment date cannot be in the past.");
        for (RecordRow row : allRecords) {
            if (!row.getOperation().equals("Traffic Deployment")) continue;
            Map<String, String> old = DashboardStore.decode(row.getDetails());
            if (date.toString().equals(old.get("date")) && data.get("shift").equals(old.get("shift"))
                    && (data.get("constable").equalsIgnoreCase(old.get("constable"))
                    || data.get("point").equalsIgnoreCase(old.get("point"))))
                throw new IllegalArgumentException("That constable or deployment point is already assigned for this date and shift.");
        }
        save("Traffic Deployment", data.get("constable"), data.get("point"), date, data.get("shift"), data,
                "Deployment assigned to " + data.get("constable") + " at " + data.get("point") + ".");
    }

    private void lookup(Map<String, String> data) {
        String vehicle = normalizeVehicle(data.get("vehicle"));
        if (!knownVehicle(vehicle)) { notify("Vehicle not found in registry or saved ticket history."); return; }
        String owner = VEHICLES.getOrDefault(vehicle, "");
        List<String> violations = new ArrayList<>();
        for (RecordRow row : allRecords) {
            if (row.getOperation().equals("Traffic Ticket") && vehicle.equalsIgnoreCase(row.getSubject())) {
                Map<String, String> ticket = DashboardStore.decode(row.getDetails());
                if (owner.isBlank()) owner = ticket.getOrDefault("owner", "");
                violations.add(row.getId() + " " + ticket.getOrDefault("violation", "Violation")
                        + " (" + (isPaid(row.getId()) ? "paid" : "unpaid") + ")");
            }
        }
        notify("Vehicle " + vehicle + " · owner: " + (owner.isBlank() ? "not recorded" : owner)
                + " · violations: " + (violations.isEmpty() ? "none" : String.join(", ", violations)));
    }

    private void pay(Map<String, String> data) throws IOException {
        RecordRow ticket = allRecords.stream().filter(row -> row.getOperation().equals("Traffic Ticket")
                && row.getId().equalsIgnoreCase(data.get("ticketId"))).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Ticket ID was not found."));
        if (isPaid(ticket.getId())) throw new IllegalArgumentException("This ticket is already paid.");
        Map<String, String> ticketData = DashboardStore.decode(ticket.getDetails());
        BigDecimal due = amount(ticketData.get("fine"), true);
        BigDecimal paid = amount(data.get("amount"), true);
        if (paid.compareTo(due) != 0) throw new IllegalArgumentException("Payment must match the exact unpaid amount: " + due.toPlainString() + ".");
        data.put("ticketId", ticket.getId());
        data.put("amount", paid.toPlainString());
        data.put("vehicle", ticketData.getOrDefault("vehicle", ticket.getSubject()));
        String receipt = nextReceiptId();
        data.put("receipt", receipt);
        save("Fine Payment", ticket.getId(), data.get("method"), LocalDate.now(), "Paid · Receipt " + receipt, data,
                "Payment accepted. Receipt " + receipt + " · " + paid.toPlainString() + ".");
        ticket.setStatus("Paid · Receipt " + receipt);
    }

    private void roadStatus(Map<String, String> data) throws IOException {
        LocalDate date = LocalDate.parse(data.get("date"));
        String condition = data.get("condition");
        if (condition.equals("Closed") && data.get("diversion").isBlank())
            throw new IllegalArgumentException("Provide diversion details when a road is closed.");
        save("Road Status", condition, data.get("location"), date, "Published", data,
                "Road status published for " + data.get("location") + ": " + condition
                        + (data.get("diversion").isBlank() ? "." : " · diversion: " + data.get("diversion") + "."));
    }

    private void impound(Map<String, String> data) throws IOException {
        String vehicle = normalizeVehicle(data.get("vehicle"));
        if (!knownVehicle(vehicle)) throw new IllegalArgumentException("Only a vehicle in the registry or saved ticket history can be impounded.");
        if (allRecords.stream().filter(row -> row.getOperation().equals("Vehicle Impound"))
                .anyMatch(row -> vehicle.equalsIgnoreCase(DashboardStore.decode(row.getDetails()).get("vehicle"))))
            throw new IllegalArgumentException("This vehicle is already impounded.");
        LocalDate date = LocalDate.parse(data.get("date"));
        if (date.isAfter(LocalDate.now())) throw new IllegalArgumentException("Impound date cannot be in the future.");
        LocalTime time;
        try { time = LocalTime.parse(data.get("time")); }
        catch (DateTimeParseException ex) { throw new IllegalArgumentException("Impound time must use 24-hour HH:mm format."); }
        data.put("vehicle", vehicle);
        data.put("time", time.toString());
        save("Vehicle Impound", vehicle, data.get("reason"), date, "Impounded · " + time, data,
                "Vehicle " + vehicle + " impounded at " + time + ".");
    }

    private void report(Map<String, String> data) throws IOException {
        LocalDate from = LocalDate.parse(data.get("from"));
        LocalDate to = LocalDate.parse(data.get("to"));
        if (to.isBefore(from)) throw new IllegalArgumentException("End date must be on or after start date.");
        String filter = data.get("violation");
        int total = 0, paid = 0, unpaid = 0;
        BigDecimal paidAmount = BigDecimal.ZERO, unpaidAmount = BigDecimal.ZERO;
        for (RecordRow row : allRecords) {
            if (!row.getOperation().equals("Traffic Ticket")) continue;
            LocalDate ticketDate = LocalDate.parse(row.getDate());
            Map<String, String> ticket = DashboardStore.decode(row.getDetails());
            if (ticketDate.isBefore(from) || ticketDate.isAfter(to)
                    || (!filter.equals("All") && !filter.equals(ticket.get("violation")))) continue;
            BigDecimal fine = amount(ticket.get("fine"), true);
            total++;
            if (isPaid(row.getId())) { paid++; paidAmount = paidAmount.add(fine); }
            else { unpaid++; unpaidAmount = unpaidAmount.add(fine); }
        }
        data.put("total", Integer.toString(total));
        data.put("paidCount", Integer.toString(paid));
        data.put("unpaidCount", Integer.toString(unpaid));
        data.put("paidAmount", paidAmount.toPlainString());
        data.put("unpaidAmount", unpaidAmount.toPlainString());
        save("Traffic Violation Report", filter, from + " to " + to, to, "Report generated", data,
                "Report " + from + " through " + to + " · " + total + " ticket(s), "
                        + paid + " paid (" + paidAmount.toPlainString() + "), "
                        + unpaid + " unpaid (" + unpaidAmount.toPlainString() + ").");
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

    private boolean isPaid(String ticketId) {
        return allRecords.stream().filter(row -> row.getOperation().equals("Fine Payment"))
                .map(row -> DashboardStore.decode(row.getDetails()).get("ticketId"))
                .anyMatch(ticketId::equalsIgnoreCase);
    }

    private String nextReceiptId() {
        int next = 1;
        for (RecordRow row : allRecords) {
            if (!row.getOperation().equals("Fine Payment")) continue;
            String receipt = DashboardStore.decode(row.getDetails()).get("receipt");
            if (receipt == null || !receipt.startsWith("R-")) continue;
            try { next = Math.max(next, Integer.parseInt(receipt.substring(2)) + 1); }
            catch (NumberFormatException ignored) { }
        }
        return "R-" + next;
    }

    private void refreshPaidTicketStatuses() {
        for (RecordRow row : allRecords) {
            if (row.getOperation().equals("Traffic Ticket") && isPaid(row.getId())) {
                String receipt = allRecords.stream().filter(payment -> payment.getOperation().equals("Fine Payment"))
                        .map(payment -> DashboardStore.decode(payment.getDetails()))
                        .filter(data -> row.getId().equalsIgnoreCase(data.get("ticketId")))
                        .map(data -> data.get("receipt")).filter(Objects::nonNull).findFirst().orElse("");
                row.setStatus("Paid" + (receipt.isBlank() ? "" : " · Receipt " + receipt));
            }
        }
    }

    private boolean knownVehicle(String vehicle) {
        return VEHICLES.containsKey(vehicle) || allRecords.stream().anyMatch(row ->
                row.getOperation().equals("Traffic Ticket") && vehicle.equalsIgnoreCase(row.getSubject()));
    }

    private String registeredOwner(String vehicle) {
        String owner = VEHICLES.get(vehicle);
        if (owner != null) return owner;
        return allRecords.stream().filter(row -> row.getOperation().equals("Traffic Ticket")
                        && vehicle.equalsIgnoreCase(row.getSubject()))
                .map(row -> DashboardStore.decode(row.getDetails()).get("owner"))
                .filter(Objects::nonNull).findFirst().orElse("");
    }

    private static String normalizeVehicle(String vehicle) {
        String normalized = vehicle.trim().toUpperCase(Locale.ROOT).replace(' ', '-');
        if (!normalized.matches("[A-Z0-9]{2,3}-[A-Z0-9]{3,4}"))
            throw new IllegalArgumentException("Vehicle registration must use format ABC-1234.");
        return normalized;
    }

    private static BigDecimal amount(String value, boolean positive) {
        try {
            BigDecimal amount = new BigDecimal(value);
            if (amount.scale() > 2 || (positive && amount.signum() <= 0))
                throw new IllegalArgumentException("Amount must be " + (positive ? "positive" : "valid") + " and have at most two decimal places.");
            return amount.stripTrailingZeros();
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("Enter a valid monetary amount.");
        }
    }

    private static int wholeNumber(String value, String label) {
        try { return Integer.parseInt(value); }
        catch (NumberFormatException ex) { throw new IllegalArgumentException(label + " must be a whole number."); }
    }

    private static String[] concat(String[] first, String[] second) {
        String[] result = Arrays.copyOf(first, first.length + second.length);
        System.arraycopy(second, 0, result, first.length, second.length);
        return result;
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
}
