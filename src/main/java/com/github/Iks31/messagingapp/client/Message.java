package com.github.Iks31.messagingapp.client;

import java.util.ArrayList;
import java.util.Date;

public class Message {
    private String id;
    private String content;
    private String sender;
    private Date timestamp;
    private ArrayList<String> readBy;
    private boolean edited;
    private boolean isDeleted;

    public Message(String id,String content, String sender, Date timestamp, ArrayList<String> readBy, boolean edited, boolean isDeleted) {
        this.id = id;
        this.content = content;
        this.sender = sender;
        this.timestamp = timestamp;
        this.readBy = readBy;
        this.edited = edited;
    }
    public String getContent() {
        return content;
    }
    public String getSender() {
        return sender;
    }
    public Date getTimestamp() {
        return timestamp;
    }
    public ArrayList<String> getReadBy() {
        return readBy;
    }
    public boolean isEdited() {
        return edited;
    }
    public boolean isDeleted() {
        return isDeleted;
    }
}
