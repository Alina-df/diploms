package com.example.alinadiplom.model;

import java.io.Serializable;

public class Event implements Serializable {
    private String title;
    private String description;
    private String date;

    // Пустой конструктор (обязателен для Firebase)
    public Event() {
    }

    // Конструктор с параметрами
    public Event(String title, String description, String date) {
        this.title = title;
        this.description = description;
        this.date = date;
    }

    // Геттеры и сеттеры
    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }
}
