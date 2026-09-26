package com.github.Iks31.messagingapp.client;

import com.github.Iks31.messagingapp.common.NetworkMessage;

// Receives server messages on the JavaFX application thread
public interface MessageHandler {
    void onMessage(NetworkMessage<?> message);
}
