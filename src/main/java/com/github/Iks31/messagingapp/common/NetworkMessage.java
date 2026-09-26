package com.github.Iks31.messagingapp.common;

import java.io.Serial;
import java.io.Serializable;

// Model for network messages that are sent over the network.
// The flag is one of the constants in Protocol and determines the type of the content.
public class NetworkMessage<T> implements Serializable {
    @Serial
    private static final long serialVersionUID = 2L;

    private final String flag;
    private final T content;

    public NetworkMessage(String flag, T content) {
        this.flag = flag;
        this.content = content;
    }

    public String getFlag() {
        return flag;
    }

    public T getContent() {
        return content;
    }

    @Override
    public String toString() {
        return "NetworkMessage[" + flag + "]";
    }
}
