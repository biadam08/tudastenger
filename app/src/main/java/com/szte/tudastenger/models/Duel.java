package com.szte.tudastenger.models;

import com.google.firebase.Timestamp;

import java.util.ArrayList;

public class Duel {
    private String id;
    private String challengerUid;
    private String challengedUid;
    private String category;
    private ArrayList<String> questionIds;
    private ArrayList<Boolean> challengerUserResults;
    private ArrayList<Boolean> challengedUserResults;
    private Timestamp date;
    private boolean isFinished;

    public Duel() {
    }

    public Duel(String id, String challengerUid, String challengedUid, String category, ArrayList<String> questionIds, ArrayList<Boolean> challengerUserResults, ArrayList<Boolean> challengedUserResults) {
        this.id = id;
        this.challengerUid = challengerUid;
        this.challengedUid = challengedUid;
        this.category = category;
        this.questionIds = questionIds;
        this.challengerUserResults = challengerUserResults;
        this.challengedUserResults = challengedUserResults;
        this.date = Timestamp.now();
        this.isFinished = false;
    }

    public boolean isFinished() {
        return isFinished;
    }

    public void setFinished(boolean finished) {
        isFinished = finished;
    }

    public Timestamp getDate() {
        return date;
    }

    public void setDate(Timestamp date) {
        this.date = date;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getChallengerUid() {
        return challengerUid;
    }

    public void setChallengerUid(String challengerUid) {
        this.challengerUid = challengerUid;
    }

    public String getChallengedUid() {
        return challengedUid;
    }

    public void setChallengedUid(String challengedUid) {
        this.challengedUid = challengedUid;
    }

    public ArrayList<String> getQuestionIds() {
        return questionIds;
    }

    public void setQuestionIds(ArrayList<String> questionIds) {
        this.questionIds = questionIds;
    }

    public ArrayList<Boolean> getChallengerUserResults() {
        return challengerUserResults;
    }

    public void setChallengerUserResults(ArrayList<Boolean> challengerUserResults) {
        this.challengerUserResults = challengerUserResults;
    }

    public ArrayList<Boolean> getChallengedUserResults() {
        return challengedUserResults;
    }

    public void setChallengedUserResults(ArrayList<Boolean> challengedUserResults) {
        this.challengedUserResults = challengedUserResults;
    }
}
