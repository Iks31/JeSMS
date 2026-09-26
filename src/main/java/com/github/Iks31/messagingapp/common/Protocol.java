package com.github.Iks31.messagingapp.common;

import java.io.Serial;
import java.io.Serializable;

// Flags and payload types exchanged between the client and server.
// Each flag documents the content type carried by its NetworkMessage.
public final class Protocol {
    private Protocol() {}

    // ---- Client -> Server ----
    public static final String LOGIN = "LOGIN";                            // Credentials
    public static final String REGISTER = "REGISTER";                      // Credentials
    public static final String LOGOUT = "LOGOUT";                          // null
    public static final String DISCONNECT = "DISCONNECT";                  // String
    public static final String GET_CONVERSATIONS = "GET_CONVERSATIONS";    // null
    public static final String CREATE_CONVERSATION = "CREATE_CONVERSATION"; // Conversation
    public static final String SEND_CHAT = "SEND_CHAT";                    // ChatRequest
    public static final String EDIT_CHAT = "EDIT_CHAT";                    // ChatRequest (message holds new content)
    public static final String DELETE_CHAT = "DELETE_CHAT";                // ChatRequest
    public static final String READ_CONVERSATION = "READ_CONVERSATION";    // String conversation id
    public static final String ADD_MEMBER = "ADD_MEMBER";                  // MemberRequest
    public static final String LEAVE_CONVERSATION = "LEAVE_CONVERSATION";  // String conversation id
    public static final String CHANGE_PASSWORD = "CHANGE_PASSWORD";        // PasswordChange

    // ---- Server -> Client ----
    public static final String INIT_SUCCESS = "INIT_SUCCESS";              // String
    public static final String LOGIN_SUCCESS = "LOGIN_SUCCESS";            // String username
    public static final String LOGIN_FAIL = "LOGIN_FAIL";                  // String reason
    public static final String REGISTER_SUCCESS = "REGISTER_SUCCESS";      // String
    public static final String REGISTER_FAIL = "REGISTER_FAIL";            // String reason
    public static final String LOGOUT_SUCCESS = "LOGOUT_SUCCESS";          // null
    public static final String CONVERSATIONS_RECEIVED = "CONVERSATIONS_RECEIVED";         // ArrayList<Conversation>
    public static final String CONVERSATIONS_NOT_RECEIVED = "CONVERSATIONS_NOT_RECEIVED"; // String reason
    public static final String CREATE_CONVERSATION_SUCCESS = "CREATE_CONVERSATION_SUCCESS"; // Conversation
    public static final String CREATE_CONVERSATION_FAIL = "CREATE_CONVERSATION_FAIL";       // String reason
    // A conversation was created or its membership changed; clients insert or replace it
    public static final String REALTIME_CONVERSATION = "REALTIME_CONVERSATION";           // Conversation
    public static final String REMOVED_FROM_CONVERSATION = "REMOVED_FROM_CONVERSATION";   // String conversation id
    public static final String REALTIME_CHAT = "REALTIME_CHAT";            // ChatRequest
    public static final String REALTIME_EDIT = "REALTIME_EDIT";            // ChatRequest
    public static final String REALTIME_DELETE = "REALTIME_DELETE";        // ChatRequest
    public static final String REALTIME_READ = "REALTIME_READ";            // MemberRequest (conversation id, reader)
    public static final String CHANGE_PASSWORD_SUCCESS = "CHANGE_PASSWORD_SUCCESS";       // String
    public static final String CHANGE_PASSWORD_FAIL = "CHANGE_PASSWORD_FAIL";             // String reason
    // Generic failure for a request, content is a human-readable reason
    public static final String REQUEST_FAIL = "REQUEST_FAIL";              // String reason

    // Validation rules shared by the client and server
    public static final int MIN_USERNAME_LENGTH = 5;
    public static final int MAX_USERNAME_LENGTH = 15;
    public static final int MIN_PASSWORD_LENGTH = 5;
    public static final int MAX_PASSWORD_LENGTH = 64;
    public static final int MAX_MESSAGE_LENGTH = 4000;
    public static final int MAX_CONVERSATION_NAME_LENGTH = 40;

    // Returns an error message, or null if the username is valid
    public static String validateUsername(String username) {
        if (username == null || username.length() < MIN_USERNAME_LENGTH || username.length() > MAX_USERNAME_LENGTH) {
            return "Username must be " + MIN_USERNAME_LENGTH + "-" + MAX_USERNAME_LENGTH + " characters.";
        }
        if (!username.matches("[A-Za-z0-9_]+")) {
            return "Username may only contain letters, numbers and underscores.";
        }
        return null;
    }

    // Returns an error message, or null if the password is valid
    public static String validatePassword(String password) {
        if (password == null || password.length() < MIN_PASSWORD_LENGTH || password.length() > MAX_PASSWORD_LENGTH) {
            return "Password must be " + MIN_PASSWORD_LENGTH + "-" + MAX_PASSWORD_LENGTH + " characters.";
        }
        return null;
    }

    public record Credentials(String username, String password) implements Serializable {
        @Serial private static final long serialVersionUID = 1L;
    }

    // Identifies a message within a conversation
    public record ChatRequest(String conversationId, ChatMessage message) implements Serializable {
        @Serial private static final long serialVersionUID = 1L;
    }

    public record MemberRequest(String conversationId, String username) implements Serializable {
        @Serial private static final long serialVersionUID = 1L;
    }

    public record PasswordChange(String oldPassword, String newPassword) implements Serializable {
        @Serial private static final long serialVersionUID = 1L;
    }
}
