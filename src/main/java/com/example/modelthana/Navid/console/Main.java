package com.example.modelthana.Navid.console;
import java.io.*;
import java.time.*;
import java.util.*;

/*
 * Model Thana - User 3 and User 4
 * Simple Java console project. No external libraries are used.
 */
public class Main {
    static Scanner sc = new Scanner(System.in);
    static Store store = load();

    public static void main(String[] args) {
        int choice;
        do {
            System.out.println("\n===== MODEL THANA =====");
            System.out.println("1. Patrol Constable (User 3)");
            System.out.println("2. Traffic Sergeant (User 4)");
            System.out.println("0. Exit");
            choice = number("Choice: ");
            if (choice == 1) patrolMenu();
            else if (choice == 2) trafficMenu();
        } while (choice != 0);
        save();
        System.out.println("Goodbye.");
    }

    static void patrolMenu() {
        int c;
        do {
            System.out.println("\n--- USER 3: PATROL CONSTABLE ---");
            System.out.println("1 View duty/route  2 Submit patrol log");
            System.out.println("3 Respond dispatch  4 Report incident");
            System.out.println("5 Record arrest  6 Request backup");
            System.out.println("7 Shift check-in/out  8 Stop and search");
            System.out.println("0 Back");
            c = number("Choice: ");
            switch (c) {
                case 1: viewDuty(); break;
                case 2: patrolLog(); break;
                case 3: dispatch(); break;
                case 4: incident(); break;
                case 5: arrest(); break;
                case 6: backup(); break;
                case 7: attendance(); break;
                case 8: stopSearch(); break;
            }
        } while (c != 0);
    }

    static void trafficMenu() {
        int c;
        do {
            System.out.println("\n--- USER 4: TRAFFIC SERGEANT ---");
            System.out.println("1 Issue ticket  2 Accident report");
            System.out.println("3 Traffic deployment  4 Vehicle lookup");
            System.out.println("5 Fine payment  6 Road status/diversion");
            System.out.println("7 Impound vehicle  8 Traffic report");
            System.out.println("0 Back");
            c = number("Choice: ");
            switch (c) {
                case 1: ticket(); break;
                case 2: accident(); break;
                case 3: deployment(); break;
                case 4: vehicleLookup(); break;
                case 5: payment(); break;
                case 6: roadStatus(); break;
                case 7: impound(); break;
                case 8: report(); break;
            }
        } while (c != 0);
    }

    // USER 3 - GOAL 1
    static void viewDuty() {
        for (Duty d : store.duties) System.out.println(d);
        String date = ask("Date to filter (YYYY-MM-DD, Enter for all): ");
        for (Duty d : store.duties) {
            if (date.isEmpty() || d.date.equals(date)) {
                System.out.println("Checkpoints: " + d.checkpoints);
                System.out.println("Instructions: " + d.instructions);
                if (ask("Acknowledge duty? (yes/no): ").equalsIgnoreCase("yes")) {
                    d.acknowledgedAt = LocalDateTime.now().toString();
                    save();
                    System.out.println("Duty acknowledged.");
                }
            }
        }
    }

    // USER 3 - GOAL 2
    static void patrolLog() {
        String zone = ask("Patrol zone: ");
        String start = ask("Start time (HH:mm): ");
        String end = ask("End time (HH:mm): ");
        String observations = ask("Observations: ");
        String incidents = ask("Incidents noted: ");
        if (empty(zone, start, end, observations, incidents)) { msg("All fields required."); return; }
        try {
            if (!LocalTime.parse(end).isAfter(LocalTime.parse(start))) {
                msg("End time must be after start time."); return;
            }
        } catch (Exception e) { msg("Use time format HH:mm."); return; }
        boolean assigned = false;
        for (Duty d : store.duties) if (d.zone.equalsIgnoreCase(zone)) assigned = true;
        if (!assigned) { msg("Zone does not match assigned duty."); return; }
        store.patrolLogs.add(new Entry(id("PL"), zone, start, end, observations, incidents, today()));
        save(); msg("Patrol log submitted and saved.");
    }

    // USER 3 - GOAL 3
    static void dispatch() {
        for (Dispatch d : store.dispatches) if (d.status.equals("Pending")) System.out.println(d);
        String id = ask("Dispatch ID to accept: ");
        for (Dispatch d : store.dispatches) {
            if (d.id.equalsIgnoreCase(id) && d.status.equals("Pending")) {
                d.status = "En Route"; d.acceptedAt = LocalDateTime.now().toString();
                save(); msg("Dispatch accepted. Duty Officer notified."); return;
            }
        }
        msg("Pending dispatch not found.");
    }

    // USER 3 - GOAL 4
    static void incident() {
        String type = ask("Incident type: "), severity = ask("Severity: ");
        String location = ask("Location: "), description = ask("Description: ");
        if (empty(type, severity, location, description)) { msg("All fields required."); return; }
        store.incidents.add(new Entry(id("IR"), type, severity, location, description, LocalDateTime.now().toString()));
        save(); msg("Incident saved and forwarded to Duty Officer.");
    }

    // USER 3 - GOAL 5
    static void arrest() {
        String name = ask("Suspect name: ");
        int age = number("Age: ");
        String address = ask("Address: "), location = ask("Arrest location: ");
        String reason = ask("Reason: "), fir = ask("FIR number (Enter if none): ");
        if (empty(name, address, location, reason) || age <= 0) { msg("Invalid mandatory information."); return; }
        if (!fir.isEmpty() && !store.firs.contains(fir)) { msg("FIR number not found."); return; }
        store.arrests.add(new Entry(id("AR"), name, "" + age, address, location, reason, fir, LocalDateTime.now().toString()));
        save(); msg("Arrest record saved. Duty Officer notified.");
    }

    // USER 3 - GOAL 6
    static void backup() {
        String location = ask("Location: "), urgency = ask("Urgency (Low/Medium/High): ");
        int officers = number("Number of officers needed: ");
        if (empty(location, urgency) || officers <= 0) { msg("Enter valid details and a positive officer count."); return; }
        store.backups.add(new Entry(id("BR"), location, urgency, "" + officers, LocalDateTime.now().toString()));
        save(); msg("Backup request sent. Duty Officer alerted.");
    }

    // USER 3 - GOAL 7
    static void attendance() {
        String action = ask("Enter IN or OUT: ");
        if (action.equalsIgnoreCase("IN")) {
            for (Attendance a : store.attendance) if (a.out.isEmpty()) { msg("There is already an active check-in."); return; }
            store.attendance.add(new Attendance(id("AT"), LocalDateTime.now().toString()));
            save(); msg("Check-in recorded.");
        } else if (action.equalsIgnoreCase("OUT")) {
            for (int i = store.attendance.size()-1; i >= 0; i--) {
                Attendance a = store.attendance.get(i);
                if (a.out.isEmpty()) {
                    a.out = LocalDateTime.now().toString();
                    long minutes = Duration.between(LocalDateTime.parse(a.in), LocalDateTime.parse(a.out)).toMinutes();
                    a.hours = minutes / 60.0;
                    save(); System.out.println("Check-out recorded. Hours on duty: " + a.hours); return;
                }
            }
            msg("No active check-in found.");
        } else msg("Enter IN or OUT.");
    }

    // USER 3 - GOAL 8
    static void stopSearch() {
        String details = ask("Person/vehicle details: "), location = ask("Location: ");
        String reason = ask("Reason: "), outcome = ask("Outcome (Clear/Item Seized/Detained): ");
        if (empty(details, location, reason, outcome)) { msg("All fields required."); return; }
        if (!outcome.equalsIgnoreCase("Clear") && !outcome.equalsIgnoreCase("Item Seized") && !outcome.equalsIgnoreCase("Detained")) {
            msg("Invalid outcome."); return;
        }
        store.searches.add(new Entry(id("SS"), details, location, reason, outcome, LocalDateTime.now().toString()));
        save(); msg("Stop-and-search record saved.");
    }

    // USER 4 - GOAL 1
    static void ticket() {
        String reg = ask("Vehicle registration: ").toUpperCase();
        String driver = ask("Driver name: "), license = ask("License number: ");
        System.out.println("1 Signal violation (1000 Tk, 2 points)");
        System.out.println("2 Speeding (2000 Tk, 3 points)");
        System.out.println("3 Wrong parking (500 Tk, 1 point)");
        System.out.println("4 No driving license (2500 Tk, 4 points)");
        int v = number("Violation: ");
        String location = ask("Location: "), date = ask("Date YYYY-MM-DD: "), time = ask("Time HH:mm: ");
        Vehicle vehicle = findVehicle(reg);
        if (vehicle == null) { msg("Vehicle registration not found."); return; }
        if (empty(driver, license, location) || !validDate(date) || !validTime(time)) { msg("Invalid or missing information."); return; }
        String type; double fine; int points;
        if (v == 1) { type="Signal violation"; fine=1000; points=2; }
        else if (v == 2) { type="Speeding"; fine=2000; points=3; }
        else if (v == 3) { type="Wrong parking"; fine=500; points=1; }
        else if (v == 4) { type="No driving license"; fine=2500; points=4; }
        else { msg("Invalid violation."); return; }
        store.tickets.add(new Ticket(id("TT"), reg, driver, license, type, location, date, time, fine, points));
        save(); System.out.println("Ticket issued: " + store.tickets.get(store.tickets.size()-1));
        System.out.println("Status: Unpaid. Fine: Tk. " + fine);
    }

    // USER 4 - GOAL 2
    static void accident() {
        String location = ask("Accident location: "), date = ask("Date YYYY-MM-DD: "), time = ask("Time HH:mm: ");
        String vehicles = ask("Vehicle registration(s), comma separated: ");
        String severity = ask("Severity (Minor/Major/Fatal): "), injuries = ask("Injuries: ");
        String description = ask("Description: "), gdFir = ask("Related GD/FIR number: ");
        if (empty(location, vehicles, severity, injuries, description, gdFir) || !validDate(date) || !validTime(time)) {
            msg("Complete all fields with valid date/time."); return;
        }
        if (!severity.equalsIgnoreCase("Minor") && !severity.equalsIgnoreCase("Major") && !severity.equalsIgnoreCase("Fatal")) {
            msg("Invalid severity."); return;
        }
        store.accidents.add(new Entry(id("AC"), location, date, time, vehicles, severity, injuries, description, gdFir));
        save(); msg("Accident report saved.");
        if (severity.equalsIgnoreCase("Fatal")) msg("Fatal accident forwarded to Duty Officer for FIR registration.");
    }

    // USER 4 - GOAL 3
    static void deployment() {
        System.out.println("Traffic points: " + store.points);
        System.out.println("Traffic constables: " + store.constables);
        String point = ask("Traffic point: "), constable = ask("Constable ID: ");
        String shift = ask("Shift: "), date = ask("Date YYYY-MM-DD: ");
        if (!store.points.contains(point) || !store.constables.contains(constable) || !validDate(date)) {
            msg("Unknown point/constable or invalid date."); return;
        }
        if (LocalDate.parse(date).isBefore(LocalDate.now())) { msg("Date cannot be in the past."); return; }
        for (Deployment d : store.deployments) {
            if (d.constable.equals(constable) && d.shift.equalsIgnoreCase(shift) && d.date.equals(date)) {
                msg("Constable already assigned to another point in this shift."); return;
            }
        }
        store.deployments.add(new Deployment(id("TD"), point, constable, shift, date));
        save(); msg("Deployment saved.");
    }

    // USER 4 - GOAL 4
    static void vehicleLookup() {
        String reg = ask("Vehicle registration: ").toUpperCase();
        Vehicle v = findVehicle(reg);
        if (v == null) { msg("Vehicle not found."); return; }
        System.out.println("Owner: " + v.owner + " | Type: " + v.type + " | Status: " + v.status);
        boolean found = false;
        for (Ticket t : store.tickets) if (t.reg.equalsIgnoreCase(reg)) { System.out.println(t); found=true; }
        if (!found) msg("No previous violations.");
    }

    // USER 4 - GOAL 5
    static void payment() {
        String ticketId = ask("Ticket ID: ");
        for (Ticket t : store.tickets) {
            if (t.id.equalsIgnoreCase(ticketId)) {
                if (!t.status.equals("Unpaid")) { msg("Ticket is already paid."); return; }
                System.out.println("Fine amount: Tk. " + t.fine);
                double amount = decimal("Amount received: ");
                String method = ask("Payment method: ");
                if (amount != t.fine || method.isEmpty()) { msg("Amount must exactly match the fine and method is required."); return; }
                t.status = "Paid";
                store.payments.add(new Entry(id("FP"), ticketId, "" + amount, method, LocalDateTime.now().toString()));
                save(); msg("Payment recorded. Ticket marked Paid. Receipt generated."); return;
            }
        }
        msg("Ticket not found.");
    }

    // USER 4 - GOAL 6
    static void roadStatus() {
        String road = ask("Road name: "), cause = ask("Cause: ");
        String status = ask("Status (Closed/Congested/Diverted): ");
        String clearance = ask("Expected clearance time: "), diversion = ask("Diversion route (None if not applicable): ");
        if (empty(road, cause, status, clearance, diversion)) { msg("All fields required."); return; }
        if (!status.equalsIgnoreCase("Closed") && !status.equalsIgnoreCase("Congested") && !status.equalsIgnoreCase("Diverted")) {
            msg("Invalid status."); return;
        }
        store.roads.add(new Entry(id("RS"), road, cause, status, clearance, diversion, LocalDateTime.now().toString()));
        save(); msg("Road status saved. Current road status:");
        for (Entry e : store.roads) System.out.println(e);
    }

    // USER 4 - GOAL 7
    static void impound() {
        String reg = ask("Vehicle registration: ").toUpperCase();
        String reason = ask("Reason: "), yard = ask("Yard location: ");
        Vehicle v = findVehicle(reg);
        if (v == null) { msg("Vehicle does not exist."); return; }
        if (v.status.equals("Impounded")) { msg("Vehicle is already impounded."); return; }
        if (empty(reason, yard)) { msg("Reason and yard are required."); return; }
        v.status = "Impounded";
        store.impounds.add(new Entry(id("IM"), reg, reason, yard, LocalDateTime.now().toString()));
        save(); msg("Impound record saved. Vehicle marked Impounded.");
    }

    // USER 4 - GOAL 8
    static void report() {
        String start = ask("Start date YYYY-MM-DD: "), end = ask("End date YYYY-MM-DD: ");
        String type = ask("Violation type (All for all): ");
        if (!validDate(start) || !validDate(end) || LocalDate.parse(start).isAfter(LocalDate.parse(end))) {
            msg("Invalid date range."); return;
        }
        int total=0, paid=0, unpaid=0; double fine=0;
        for (Ticket t : store.tickets) {
            boolean inRange = !LocalDate.parse(t.date).isBefore(LocalDate.parse(start)) &&
                    !LocalDate.parse(t.date).isAfter(LocalDate.parse(end));
            if (inRange && (type.equalsIgnoreCase("All") || t.violation.equalsIgnoreCase(type))) {
                System.out.println(t); total++; fine += t.fine;
                if (t.status.equals("Paid")) paid++; else unpaid++;
            }
        }
        System.out.println("Total tickets: " + total);
        System.out.println("Total fine: Tk. " + fine);
        System.out.println("Paid: " + paid + " | Unpaid: " + unpaid);
    }

    // Common helper methods
    static String ask(String s) { System.out.print(s); return sc.nextLine().trim(); }
    static int number(String s) {
        while (true) try { return Integer.parseInt(ask(s)); }
        catch (NumberFormatException e) { System.out.println("Enter a whole number."); }
    }
    static double decimal(String s) {
        while (true) try { return Double.parseDouble(ask(s)); }
        catch (NumberFormatException e) { System.out.println("Enter a valid amount."); }
    }
    static boolean empty(String... values) {
        for (String s : values) if (s == null || s.trim().isEmpty()) return true;
        return false;
    }
    static boolean validDate(String s) { try { LocalDate.parse(s); return true; } catch (Exception e) { return false; } }
    static boolean validTime(String s) { try { LocalTime.parse(s); return true; } catch (Exception e) { return false; } }
    static String today() { return LocalDate.now().toString(); }
    static String id(String prefix) { return prefix + System.currentTimeMillis(); }
    static void msg(String s) { System.out.println(s); }
    static Vehicle findVehicle(String reg) {
        for (Vehicle v : store.vehicles) if (v.reg.equalsIgnoreCase(reg)) return v;
        return null;
    }

    // Binary file handling with object streams, as covered in the class notes.
    static Store load() {
        try {
            ObjectInputStream in = new ObjectInputStream(new FileInputStream("model_thana_data.dat"));
            Store s = (Store) in.readObject(); in.close(); return s;
        } catch (Exception e) {
            Store s = new Store();
            s.duties.add(new Duty("DUTY-01", today(), "Morning", "Zone A", "PC-02",
                    "Police Vehicle-01", "Checkpoint 1, Checkpoint 2, Checkpoint 3",
                    "Check the market area and main road."));
            s.dispatches.add(new Dispatch("DSP-01", "Road 10, Zone A", "Theft", "Theft report needs response."));
            s.firs.add("FIR-1001");
            s.vehicles.add(new Vehicle("DHAKA-METRO-GA-11-1234", "Rahim Uddin", "Private Car"));
            s.vehicles.add(new Vehicle("DHAKA-METRO-CHA-22-5678", "Karim Ahmed", "Motorcycle"));
            s.points.add("Shahbagh"); s.points.add("Farmgate"); s.points.add("Mohakhali");
            s.constables.add("TC-101"); s.constables.add("TC-102"); s.constables.add("TC-103");
            return s;
        }
    }
    static void save() {
        try {
            ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream("model_thana_data.dat"));
            out.writeObject(store); out.close();
        } catch (IOException e) { System.out.println("File save error: " + e.getMessage()); }
    }

    // Base user class and subclasses demonstrate inheritance.
    static class StaffUser implements Serializable {
        private String staffId, name;
        StaffUser(String id, String name) { staffId=id; this.name=name; }
        public String getStaffId() { return staffId; }
        public String getName() { return name; }
    }
    static class PatrolConstable extends StaffUser { PatrolConstable(String id, String name) { super(id,name); } }
    static class TrafficSergeant extends StaffUser { TrafficSergeant(String id, String name) { super(id,name); } }

    // Model classes. Fields are private and initialized through constructors.
    static class Store implements Serializable {
        ArrayList<Duty> duties = new ArrayList<Duty>();
        ArrayList<Dispatch> dispatches = new ArrayList<Dispatch>();
        ArrayList<Entry> patrolLogs = new ArrayList<Entry>(), incidents = new ArrayList<Entry>(),
                arrests = new ArrayList<Entry>(), backups = new ArrayList<Entry>(), searches = new ArrayList<Entry>(),
                accidents = new ArrayList<Entry>(), payments = new ArrayList<Entry>(), roads = new ArrayList<Entry>(),
                impounds = new ArrayList<Entry>();
        ArrayList<Attendance> attendance = new ArrayList<Attendance>();
        ArrayList<Vehicle> vehicles = new ArrayList<Vehicle>();
        ArrayList<Ticket> tickets = new ArrayList<Ticket>();
        ArrayList<Deployment> deployments = new ArrayList<Deployment>();
        ArrayList<String> points = new ArrayList<String>(), constables = new ArrayList<String>(), firs = new ArrayList<String>();
    }
    static class Entry implements Serializable {
        private String[] values;
        Entry(String... v) { values=v; }
        public String toString() { return Arrays.toString(values); }
    }
    static class Duty implements Serializable {
        private String id, date, shift, zone, partner, vehicle, checkpoints, instructions, acknowledgedAt="";
        Duty(String id,String date,String shift,String zone,String partner,String vehicle,String checkpoints,String instructions) {
            this.id=id; this.date=date; this.shift=shift; this.zone=zone; this.partner=partner;
            this.vehicle=vehicle; this.checkpoints=checkpoints; this.instructions=instructions;
        }
        public String toString() { return id+" | "+date+" | "+shift+" | "+zone+" | Partner: "+partner+" | Vehicle: "+vehicle; }
    }
    static class Dispatch implements Serializable {
        private String id, location, type, details, status="Pending", acceptedAt="";
        Dispatch(String id,String location,String type,String details) {
            this.id=id; this.location=location; this.type=type; this.details=details;
        }
        public String toString() { return id+" | "+location+" | "+type+" | "+details+" | "+status; }
    }
    static class Attendance implements Serializable {
        private String id, in, out=""; private double hours;
        Attendance(String id,String in) { this.id=id; this.in=in; }
    }
    static class Vehicle implements Serializable {
        private String reg, owner, type, status="Active";
        Vehicle(String reg,String owner,String type) { this.reg=reg; this.owner=owner; this.type=type; }
    }
    static class Ticket implements Serializable {
        private String id, reg, driver, license, violation, location, date, time, status="Unpaid";
        private double fine; private int points;
        Ticket(String id,String reg,String driver,String license,String violation,String location,
               String date,String time,double fine,int points) {
            this.id=id; this.reg=reg; this.driver=driver; this.license=license; this.violation=violation;
            this.location=location; this.date=date; this.time=time; this.fine=fine; this.points=points;
        }
        public String toString() { return id+" | "+reg+" | "+driver+" | "+violation+" | "+date+" | Tk."+fine+" | "+status; }
    }
    static class Deployment implements Serializable {
        private String id, point, constable, shift, date;
        Deployment(String id,String point,String constable,String shift,String date) {
            this.id=id; this.point=point; this.constable=constable; this.shift=shift; this.date=date;
        }
    }
}
