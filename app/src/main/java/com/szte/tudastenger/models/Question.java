package com.szte.tudastenger.models;

import java.util.ArrayList;

public class Question {
    private String id;
    private String questionText;
    private String category;
    private ArrayList<String> answers;
    private int correctAnswerIndex;
    private String image;
    private String explanationText;


    public Question() {
    }

    public Question(String id, String questionText, String category, ArrayList<String> answers, int correctAnswerIndex, String image, String explanationText) {
        this.id = id;
        this.questionText = questionText;
        this.category = category;
        this.answers = answers;
        this.correctAnswerIndex = correctAnswerIndex;
        this.image = image;
        this.explanationText = explanationText;
    }

    public String getExplanationText() {
        return explanationText;
    }

    public void setExplanationText(String explanationText) {
        this.explanationText = explanationText;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getQuestionText() {
        return questionText;
    }

    public void setQuestionText(String questionText) {
        this.questionText = questionText;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public ArrayList<String> getAnswers() {
        return answers;
    }

    public void setAnswers(ArrayList<String> answers) {
        this.answers = answers;
    }

    public int getCorrectAnswerIndex() {
        return correctAnswerIndex;
    }

    public void setCorrectAnswerIndex(int correctAnswerIndex) {
        this.correctAnswerIndex = correctAnswerIndex;
    }

    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
    }
}
