package com.github.Iks31.messagingapp.common;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

// Class used for mapping JSON conversations stored in MongoDB to Conversation objects
public class Conversation implements Serializable {
    @JsonProperty("_id")
    public Object id;
    @JsonProperty ("name")
    public String name;
    @JsonProperty("messages")
    public List<ChatMessage> messages;
    @JsonProperty("users")
    public ArrayList<String> users;

}
