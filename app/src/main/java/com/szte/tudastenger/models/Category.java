package com.szte.tudastenger.models;

public class Category {
    private String id;
    private String name;

    private String image;

    public Category() { // szükséges a Firebase Firestore-ból való objektum-deszerializáláshoz
    }

    public Category(String id, String name, String image) {
        this.id = id;
        this.name = name;
        this.image = image;
    }

    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
