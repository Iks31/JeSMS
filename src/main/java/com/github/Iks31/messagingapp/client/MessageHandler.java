package com.github.Iks31.messagingapp.client;

import com.github.Iks31.messagingapp.common.NetworkMessage;

public interface MessageHandler {
    public void onMessage(NetworkMessage message);
}
