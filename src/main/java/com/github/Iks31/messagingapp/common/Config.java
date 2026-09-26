package com.github.Iks31.messagingapp.common;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// Reads settings from environment variables, falling back to a .env file in the working directory.
// The .env file holds secrets such as the database password and must never be committed.
public final class Config {
    public static final Path ENV_FILE = Path.of(".env");
    private static final Map<String, String> DOT_ENV = load(ENV_FILE);

    private Config() {}

    // Returns the environment variable, else the .env value, else the default
    public static String get(String key, String defaultValue) {
        String value = System.getenv(key);
        if (value == null || value.isBlank()) value = DOT_ENV.get(key);
        return value == null || value.isBlank() ? defaultValue : value;
    }

    public static int getInt(String key, int defaultValue) {
        String value = get(key, null);
        if (value == null) return defaultValue;
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            System.err.println("[CONFIG] " + key + " is not a number, using " + defaultValue);
            return defaultValue;
        }
    }

    public static boolean hasEnvFile() {
        return !DOT_ENV.isEmpty();
    }

    private static Map<String, String> load(Path file) {
        if (!Files.isRegularFile(file)) return Map.of();
        try {
            return parse(Files.readAllLines(file));
        } catch (IOException e) {
            System.err.println("[CONFIG] Could not read " + file.toAbsolutePath() + ": " + e.getMessage());
            return Map.of();
        }
    }

    // Parses KEY=VALUE lines; blank lines, # comments and an optional "export " prefix are allowed.
    // Values may be wrapped in single or double quotes.
    static Map<String, String> parse(List<String> lines) {
        Map<String, String> values = new HashMap<>();
        for (String raw : lines) {
            String line = raw.strip();
            if (line.isEmpty() || line.startsWith("#")) continue;
            if (line.startsWith("export ")) line = line.substring("export ".length()).strip();
            int equals = line.indexOf('=');
            if (equals <= 0) continue;
            String key = line.substring(0, equals).strip();
            String value = line.substring(equals + 1).strip();
            if (value.length() >= 2 && (value.startsWith("\"") && value.endsWith("\"")
                    || value.startsWith("'") && value.endsWith("'"))) {
                value = value.substring(1, value.length() - 1);
            }
            values.put(key, value);
        }
        return values;
    }
}
