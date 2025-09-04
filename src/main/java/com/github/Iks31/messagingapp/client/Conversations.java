package com.github.Iks31.messagingapp.client;

import java.util.ArrayList;

public class Conversations {
    // directly copies the structure of the conversation collection
    private String conversationId;
    private ArrayList<String> users;
    private ArrayList<Message> messages;

    public Conversations(String conversationId, ArrayList<String> users, ArrayList<Message> messages) {
        this.conversationId = conversationId;
        this.users = users;
        this.messages = messages;
    }
    public String getConversationId() {
        return conversationId;
    }
    public ArrayList<String> getUsers() {
        return users;
    }
    public ArrayList<Message> getMessages() {
        return messages;
    }
}
