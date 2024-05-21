package com.szte.tudastenger;

import com.google.firebase.Timestamp;

public class AnsweredQuestion {
    private String questionId;
    private String category;
    private String userId;
    private Timestamp date;
    private String answer;
    private boolean isCorrect;

    public AnsweredQuestion() {
    }

    public AnsweredQuestion(String questionId, String category, String userId, String answer, boolean isCorrect) {
        this.questionId = questionId;
        this.category = category;
        this.userId = userId;
        this.date = Timestamp.now();
        this.answer = answer;
        this.isCorrect = isCorrect;
    }

    public AnsweredQuestion(String questionId, String category, String userId, Timestamp date, String answer, boolean isCorrect) {
        this.questionId = questionId;
        this.category = category;
        this.userId = userId;
        this.date = date;
        this.answer = answer;
        this.isCorrect = isCorrect;
    }

    public String getQuestionId() {
        return questionId;
    }

    public void setQuestionId(String questionId) {
        this.questionId = questionId;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public Timestamp getDate() {
        return date;
    }

    public void setDate(Timestamp date) {
        this.date = date;
    }

    public String getAnswer() {
        return answer;
    }

    public void setAnswer(String answer) {
        this.answer = answer;
    }

    public boolean isCorrect() {
        return isCorrect;
    }

    public void setCorrect(boolean correct) {
        isCorrect = correct;
    }
}
