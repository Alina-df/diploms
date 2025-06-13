package com.example.alinadiplom.model;

public class ServiceRequest {
    private String room;
    private String problem;
    private String type;
    private String status;
    private long timestamp;

    private String requestId;
    private String userId;

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public ServiceRequest() {} // Обязательно для Firebase

    public ServiceRequest(String room, String problem, String type, String status, long timestamp) {
        this.room = room;
        this.problem = problem;
        this.type = type;
        this.status = status;
        this.timestamp = timestamp;
    }

    public String getRoom() { return room; }
    public String getProblem() { return problem; }
    public String getType() { return type; }
    public String getStatus() { return status; }
    public long getTimestamp() { return timestamp; }

    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }
}
