package com.github.Iks31.messagingapp.common;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;

public class ChatMessage {
    @JsonProperty("content")
    public String content;
    @JsonProperty("sender")
    public String sender;
    @JsonProperty("timestamp")
    public Instant timestamp;
    @JsonProperty("readBy")
    public List<String> readBy;
    @JsonProperty("edited")
    public boolean edited;
    @JsonProperty("isDeleted")
    public boolean isDeleted;
}
