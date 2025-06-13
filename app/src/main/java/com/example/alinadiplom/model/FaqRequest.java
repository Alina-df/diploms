package com.example.alinadiplom.model;

public class FaqRequest {
    private String id;
    private String userId;    // Кто задал вопрос
    private String question;
    private String answer;
    private boolean answered;

    public FaqRequest() {}

    public FaqRequest(String userId, String question, String answer, boolean answered) {
        this.userId = userId;
        this.question = question;
        this.answer = answer;
        this.answered = answered;
    }

    // Геттеры и сеттеры...

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getQuestion() { return question; }
    public void setQuestion(String question) { this.question = question; }

    public String getAnswer() { return answer; }
    public void setAnswer(String answer) { this.answer = answer; }

    public boolean isAnswered() { return answered; }
    public void setAnswered(boolean answered) { this.answered = answered; }
}
