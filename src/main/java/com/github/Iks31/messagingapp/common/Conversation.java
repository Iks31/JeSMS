package com.github.Iks31.messagingapp.common;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class Conversation {
//    @JsonProperty("conversationName")
//    public String conversationName;
    @JsonProperty("_id")
    public Object id;
    @JsonProperty ("name")
    public String name;
    @JsonProperty("messages")
    public List<ChatMessage> messages;
    @JsonProperty("users")
    public List<String> users;
    // Example template of what Json mapping class may look like
}
