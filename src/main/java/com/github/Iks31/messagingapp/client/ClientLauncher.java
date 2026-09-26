package com.github.Iks31.messagingapp.client;

// Entry point for running the client from an IDE.
// Launching a class that extends Application with JavaFX on the classpath fails with
// "JavaFX runtime components are missing", so this plain class starts ClientApp instead.
public class ClientLauncher {
    public static void main(String[] args) {
        ClientApp.main(args);
    }
}
