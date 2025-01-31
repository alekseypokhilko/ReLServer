package net.relserver.core.api;

import java.util.UUID;

public interface Id {
    String getId();

    static String generateId(String prefix) {
        String id = UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        return String.format("%s-%s", prefix == null ? "" : prefix, id);
    }
}
