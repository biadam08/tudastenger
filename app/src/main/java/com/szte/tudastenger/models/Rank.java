package com.szte.tudastenger.models;

public class Rank {
    private String id;
    private String rankName;
    private Long threshold;

    public Rank(){

    }

    public Rank(String id, String rankName, Long threshold) {
        this.id = id;
        this.rankName = rankName;
        this.threshold = threshold;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getRankName() {
        return rankName;
    }

    public void setRankName(String rankName) {
        this.rankName = rankName;
    }

    public Long getThreshold() {
        return threshold;
    }

    public void setThreshold(Long threshold) {
        this.threshold = threshold;
    }
}
