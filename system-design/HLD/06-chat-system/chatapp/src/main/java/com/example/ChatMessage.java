package com.example;

public class ChatMessage {

    private String type;    // "join", "leave", "msg", "sys", "error"
    private String group;   // group name
    private String from;    // sender name
    private String content; // message body

    public ChatMessage() {
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getGroup() {
        return group;
    }

    public void setGroup(String group) {
        this.group = group;
    }

    public String getFrom() {
        return from;
    }

    public void setFrom(String from) {
        this.from = from;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

}
