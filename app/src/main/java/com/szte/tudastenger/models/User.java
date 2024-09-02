package com.szte.tudastenger.models;

public class User {

    private String id;
    private String username;
    private String email;

    private String fcmToken;
    private String profilePicture;

    private String role;

    private Integer gold;

    public User() {
    }

    public User(String id, String username, String email, String fcmToken) {
        this.id = id;
        this.username = username;
        this.email = email;
        this.fcmToken = fcmToken;
        this.profilePicture = "";
        this.gold = 100;
        this.role = "user";
    }

    public String getFcmToken() {
        return fcmToken;
    }

    public void setFcmToken(String fcmToken) {
        this.fcmToken = fcmToken;
    }

    public String getId() {
        return id;
    }

    public String getProfilePicture() {
        return profilePicture;
    }

    public void setProfilePicture(String profilePicture) {
        this.profilePicture = profilePicture;
    }

    public Integer getGold() {
        return gold;
    }

    public void setGold(Integer gold) {
        this.gold = gold;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }
}
