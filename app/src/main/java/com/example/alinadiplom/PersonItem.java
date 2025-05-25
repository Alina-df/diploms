package com.example.alinadiplom;


public class PersonItem {
    public int photoResId;
    private String room;
    private String name;
    private String message;
    private String tags;
    private int avatarResId; // R.drawable.аватар

    public PersonItem(String room, String name, String message, String tags, int avatarResId) {
        this.room = room;
        this.name = name;
        this.message = message;
        this.tags = tags;
        this.avatarResId = avatarResId;
    }

    public String getRoom() { return room; }
    public String getName() { return name; }
    public String getMessage() { return message; }
    public CharSequence getTags() { return tags; }
    public int getAvatarResId() { return avatarResId; }
}
