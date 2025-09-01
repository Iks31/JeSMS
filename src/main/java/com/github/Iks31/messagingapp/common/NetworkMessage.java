package com.github.Iks31.messagingapp.common;

import java.io.Serializable;

public class NetworkMessage implements Serializable {
    private String flag;
    private Object content;
    public NetworkMessage(String flag, Object content) {
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
