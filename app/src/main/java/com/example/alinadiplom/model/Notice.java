package com.example.alinadiplom.model;

public class Notice {
    public String id, title, body, date, userId, userName, room, tags;
    public boolean isAdmin;
    public int avatarResId;
    public String telegramLink;

    public Notice() {}

    public Notice(String id, String title, String body, String date, boolean isAdmin,
                  String userId, String userName, String room, String tags, int avatarResId) {
        this.id = id;
        this.title = title;
        this.body = body;
        this.date = date;
        this.isAdmin = isAdmin;
        this.userId = userId;
        this.userName = userName;
        this.room = room;
        this.tags = tags;
        this.avatarResId = avatarResId;
    }
}