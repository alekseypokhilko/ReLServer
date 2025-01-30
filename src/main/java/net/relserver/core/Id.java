package net.relserver.core;

import java.util.UUID;

public interface Id {
    String getId();

    static String generateId(String prefix) {
        String id = UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        return "%s-%s".formatted(prefix == null ? "" : prefix, id);
    }
}
