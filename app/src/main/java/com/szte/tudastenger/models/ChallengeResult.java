package com.szte.tudastenger.models;

import java.util.ArrayList;
import java.util.Date;

public class ChallengeResult {
    private String id;
    private String userId;
    private String challengeId;
    private int correctAnswers;
    private ArrayList<Boolean> results;
    private Date completedAt;

    public ChallengeResult() {}

    public ChallengeResult(String userId, String challengeId, int correctAnswers, ArrayList<Boolean> results, Date completedAt) {
        this.userId = userId;
        this.challengeId = challengeId;
        this.correctAnswers = correctAnswers;
        this.results = results;
        this.completedAt = completedAt;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getChallengeId() { return challengeId; }
    public void setChallengeId(String challengeId) { this.challengeId = challengeId; }

    public int getCorrectAnswers() { return correctAnswers; }
    public void setCorrectAnswers(int correctAnswers) { this.correctAnswers = correctAnswers; }

    public ArrayList<Boolean> getResults() { return results; }
    public void setResults(ArrayList<Boolean> results) { this.results = results; }

    public Date getCompletedAt() { return completedAt; }
    public void setCompletedAt(Date completedAt) { this.completedAt = completedAt; }
}

