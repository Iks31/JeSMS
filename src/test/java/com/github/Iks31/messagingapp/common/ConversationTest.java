package com.github.Iks31.messagingapp.common;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ConversationTest {

    private static Conversation conversation(String name, String... users) {
        Conversation conversation = new Conversation();
        conversation.id = "c1";
        conversation.name = name;
        conversation.users = new ArrayList<>(List.of(users));
        return conversation;
    }

    @Test
    void directMessagesAreNamedAfterTheOtherUser() {
        Conversation dm = conversation("", "alice", "bobby");
        assertEquals("bobby", dm.displayName("alice"));
        assertEquals("alice", dm.displayName("bobby"));
        assertFalse(dm.isGroup());
    }

    @Test
    void groupsUseTheirName() {
        Conversation group = conversation("Friends", "alice", "bobby", "carol");
        assertEquals("Friends", group.displayName("alice"));
        assertTrue(group.isGroup());
    }

    @Test
    void unreadCountIgnoresOwnReadAndDeletedMessages() {
        Conversation dm = conversation("", "alice", "bobby");
        dm.messages.add(new ChatMessage("bobby", "hi", 1));
        dm.messages.add(new ChatMessage("bobby", "there", 2));
        dm.messages.add(new ChatMessage("alice", "hey", 3));
        ChatMessage deleted = new ChatMessage("bobby", "oops", 4);
        deleted.isDeleted = true;
        dm.messages.add(deleted);

        assertEquals(2, dm.unreadCount("alice"));
        assertEquals(1, dm.unreadCount("bobby"));

        dm.markReadBy("alice");
        assertEquals(0, dm.unreadCount("alice"));
        assertTrue(dm.messages.get(0).isReadByOthers());
        assertFalse(dm.messages.get(2).readBy.contains("alice"), "own messages are not marked read");
    }

    @Test
    void messagesAreIdentifiedBySenderAndTimestamp() {
        Conversation dm = conversation("", "alice", "bobby");
        dm.messages.add(new ChatMessage("bobby", "hi", 10));
        assertNotNull(dm.findMessage(new ChatMessage("bobby", "different content", 10)));
        assertNull(dm.findMessage(new ChatMessage("alice", "hi", 10)));
        assertNull(dm.findMessage(new ChatMessage("bobby", "hi", 11)));
    }

    @Test
    void copiesAreIndependent() {
        Conversation original = conversation("", "alice", "bobby");
        original.messages.add(new ChatMessage("bobby", "hi", 1));
        Conversation copy = new Conversation(original);
        copy.messages.getFirst().content = "changed";
        copy.users.add("carol");
        assertEquals("hi", original.messages.getFirst().content);
        assertEquals(2, original.users.size());
    }

    @Test
    void validationRules() {
        assertNull(Protocol.validateUsername("alice_1"));
        assertNotNull(Protocol.validateUsername("abc"));
        assertNotNull(Protocol.validateUsername("has space"));
        assertNotNull(Protocol.validateUsername(null));
        assertNull(Protocol.validatePassword("12345"));
        assertNotNull(Protocol.validatePassword("1234"));
    }
}
