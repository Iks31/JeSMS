package com.github.Iks31.messagingapp.common;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class Conversation {
    @JsonProperty("name")
    public String conversationName;
    @JsonProperty("messages")
    public List<String> messages;
    // Example template of what Json mapping class may look like
}
