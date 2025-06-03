package com.example.alinadiplom.model;

public class ChatMessage {
    public String text;
    public String senderId;
    public String receiverId;
    public long timestamp;
    public boolean read;

    public ChatMessage() {}

    public ChatMessage(String text, String senderId, String receiverId, long timestamp, boolean read) {
        this.text = text;
        this.senderId = senderId;
        this.receiverId = receiverId;
        this.timestamp = timestamp;
        this.read = read;
    }
}
