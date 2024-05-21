package com.szte.tudastenger;

public class Category {
    private String name;

    public Category() { // szükséges a Firebase Firestore-ból való objektum-deszerializáláshoz
    }

    public Category(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
