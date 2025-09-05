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
    public MongoLong timestamp;
    @JsonProperty("readBy")
    public List<String> readBy;
    @JsonProperty("edited")
    public boolean edited;
    @JsonProperty("isDeleted")
    public boolean isDeleted;

    public Instant getTimestampInstant() {
        return Instant.ofEpochMilli(Long.parseLong(timestamp.value));
    }

    public static class MongoLong {
        @JsonProperty("$numberLong")
        public String value; // Mongo sends it as a string
    }
}

