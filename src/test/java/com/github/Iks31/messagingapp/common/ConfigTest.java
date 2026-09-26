package com.github.Iks31.messagingapp.common;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ConfigTest {

    @Test
    void parsesKeyValueLines() {
        Map<String, String> values = Config.parse(List.of(
                "# a comment",
                "",
                "JESMS_PORT=9999",
                "export JESMS_DB_NAME = JeSMS ",
                "TEST_VALUE=\"a=b@c?d&e\"",
                "SINGLE='quoted value'",
                "not a setting",
                "=novalue"));

        assertEquals("9999", values.get("JESMS_PORT"));
        assertEquals("JeSMS", values.get("JESMS_DB_NAME"));
        // Only the first = separates the key; later =, @, ? and & are part of the value
        assertEquals("a=b@c?d&e", values.get("TEST_VALUE"));
        assertEquals("quoted value", values.get("SINGLE"));
        assertEquals(4, values.size());
    }

    @Test
    void fallsBackToDefault() {
        assertEquals("fallback", Config.get("JESMS_TEST_SETTING_THAT_IS_NEVER_SET", "fallback"));
        assertEquals(42, Config.getInt("JESMS_TEST_SETTING_THAT_IS_NEVER_SET", 42));
    }
}
