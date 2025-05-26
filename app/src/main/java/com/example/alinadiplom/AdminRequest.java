package com.example.alinadiplom;

public class AdminRequest {
    public String uid;
    public String fio;
    public String number;
    public long timestamp;

    public AdminRequest(String uid, String fio, String number, long timestamp) {
        this.uid = uid;
        this.fio = fio;
        this.number = number;
        this.timestamp = timestamp;
    }
}