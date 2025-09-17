package com.github.Iks31.messagingapp.common;

import java.io.Serializable;

// Model for network messages that are sent over the network
// TODO use of generics within all use cases of NetworkMessage
public class NetworkMessage<T> implements Serializable {
    private String flag;
    private T content;
    public NetworkMessage(String flag, T content) {
        this.flag = flag;
        this.content = content;
    }
    public String getFlag() {
        return flag;
    }
    public Object getContent() {
        return content;
    }
}
