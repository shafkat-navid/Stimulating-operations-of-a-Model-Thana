package com.example.modelthana.Navid.Citizen;

import java.io.Serializable;

public class PatrolConstable implements Serializable {

    private int badgeNo;
    private int officerId;
    private  int constableId;
    private boolean onDuty;

    public PatrolConstable(int badgeNo, int officerId, int constableId, boolean onDuty) {
        this.badgeNo = badgeNo;
        this.officerId = officerId;
        this.constableId = constableId;
        this.onDuty = onDuty;
    }

    public int getBadgeNo() {
        return badgeNo;
    }

    public void setBadgeNo(int badgeNo) {
        this.badgeNo = badgeNo;
    }

    public int getOfficerId() {
        return officerId;
    }

    public void setOfficerId(int officerId) {
        this.officerId = officerId;
    }

    public int getConstableId() {
        return constableId;
    }

    public void setConstableId(int constableId) {
        this.constableId = constableId;
    }

    public boolean isOnDuty() {
        return onDuty;
    }

    public void setOnDuty(boolean onDuty) {
        this.onDuty = onDuty;
    }

    @Override
    public String toString() {
        return "PatrolConstable{" +
                "badgeNo=" + badgeNo +
                ", officerId=" + officerId +
                ", constableId=" + constableId +
                ", onDuty=" + onDuty +
                '}';
    }
}
