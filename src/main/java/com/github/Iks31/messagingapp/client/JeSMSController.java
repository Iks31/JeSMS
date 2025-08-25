package com.github.Iks31.messagingapp.client;

public class JeSMSController {
    private final JeSMSView view;
    private final Conversations conversations;
    private final Messages displayedMessages;

    public JeSMSController(JeSMSView view) {
        this.view = view;
        this.conversations = new Conversations();
        this.displayedMessages = new Messages();

        //Update view based on model data gathered from server here

         attachEvents();
    }

    private void attachEvents() {
    }
}
