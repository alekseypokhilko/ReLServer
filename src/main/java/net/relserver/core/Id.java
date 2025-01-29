package net.relserver.core;

import java.util.UUID;

public interface Id {
    String getId();

    static String generateId(String prefix) {
        return (prefix == null ? "" : prefix) + "-" + UUID.randomUUID().toString().replace("-", "");
    }
}
