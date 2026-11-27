package com.example.modelthana.Navid.Citizen;

import javafx.scene.control.DatePicker;

import java.io.Serializable;

public class TrafficSergeant implements Serializable {

    private int dutyID;
    private DatePicker date;
    private String shift;
    private String patrolZone;
    private String vehicle;
    private boolean acknowledged;
    private boolean acknowledgedAt;

    public TrafficSergeant(int dutyID, DatePicker date, String shift, String patrolZone, String vehicle, boolean acknowledged, boolean acknowledgedAt) {
        this.dutyID = dutyID;
        this.date = date;
        this.shift = shift;
        this.patrolZone = patrolZone;
        this.vehicle = vehicle;
        this.acknowledged = acknowledged;
        this.acknowledgedAt = acknowledgedAt;
    }

    public int getDutyID() {
        return dutyID;
    }

    public void setDutyID(int dutyID) {
        this.dutyID = dutyID;
    }

    public DatePicker getDate() {
        return date;
    }

    public void setDate(DatePicker date) {
        this.date = date;
    }

    public String getShift() {
        return shift;
    }

    public void setShift(String shift) {
        this.shift = shift;
    }

    public String getPatrolZone() {
        return patrolZone;
    }

    public void setPatrolZone(String patrolZone) {
        this.patrolZone = patrolZone;
    }

    public String getVehicle() {
        return vehicle;
    }

    public void setVehicle(String vehicle) {
        this.vehicle = vehicle;
    }

    public boolean isAcknowledged() {
        return acknowledged;
    }

    public void setAcknowledged(boolean acknowledged) {
        this.acknowledged = acknowledged;
    }

    public boolean isAcknowledgedAt() {
        return acknowledgedAt;
    }

    public void setAcknowledgedAt(boolean acknowledgedAt) {
        this.acknowledgedAt = acknowledgedAt;
    }

    @Override
    public String toString() {
        return "DutyRoster{" +
                "dutyID=" + dutyID +
                ", date=" + date +
                ", shift='" + shift + '\'' +
                ", patrolZone='" + patrolZone + '\'' +
                ", vehicle='" + vehicle + '\'' +
                ", acknowledged=" + acknowledged +
                ", acknowledgedAt=" + acknowledgedAt +
                '}';
    }
}
