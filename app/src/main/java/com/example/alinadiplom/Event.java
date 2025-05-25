package com.example.alinadiplom;

public class Event {
    private String title;
    private int imageResId;

    public Event(String title, int imageResId) {
        this.title = title;
        this.imageResId = imageResId;
    }

    public String getTitle() {
        return title;
    }

    public int getImageResId() {
        return imageResId;
    }
}
