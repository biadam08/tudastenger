package com.szte.tudastenger.models;

import java.util.ArrayList;
import java.util.Date;

public class Challenge {
    private String id;
    private ArrayList<String> questionIds;
    private boolean isActive;
    private Date date;

    public Challenge() {}

    public Challenge(String id, ArrayList<String> questionIds, boolean isActive, Date date) {
        this.id = id;
        this.questionIds = questionIds;
        this.isActive = isActive;
        this.date = date;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    
    public ArrayList<String> getQuestionIds() { return questionIds; }
    public void setQuestionIds(ArrayList<String> questionIds) { this.questionIds = questionIds; }
    
    public Date getDate() { return date; }
    public void setDate(Date date) { this.date = date; }

    public boolean getIsActive() {
        return isActive;
    }

    public void setActive(boolean active) {
        isActive = active;
    }
}