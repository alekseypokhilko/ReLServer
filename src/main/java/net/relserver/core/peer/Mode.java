package net.relserver.core.peer;

import java.util.Locale;

public enum Mode {
    CLIENT,
    SERVER,
    CLIENT_SERVER,
    HUB;

    public String id() {
        return this.name().toLowerCase(Locale.ENGLISH);
    }
}
