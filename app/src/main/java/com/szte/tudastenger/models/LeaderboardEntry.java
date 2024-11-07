package com.szte.tudastenger.models;

public class LeaderboardEntry {
    private String username;
    private int points;
    private String rank;

    public LeaderboardEntry(String username, int points, String rank) {
        this.username = username;
        this.points = points;
        this.rank = rank;
    }

    public String getUsername() { return username; }
    public int getPoints() { return points; }
    public String getRank() { return rank; }
}
