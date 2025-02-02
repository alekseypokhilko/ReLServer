package net.relserver.core.peer;

import net.relserver.core.Constants;

public class Handshake {
    private final String from;
    private final String to;
    private final boolean received;

    public Handshake(String from, String to, boolean received) {
        this.from = from;
        this.to = to;
        this.received = received;
    }

    public static Handshake of(String handshakeMessage) {
        String[] tokens = handshakeMessage.split(Constants.SEPARATOR);
        return new Handshake(tokens[1], tokens[2], Boolean.parseBoolean(tokens[3]));
    }

    @Override
    public String toString() {
        return Constants.HANDSHAKE_MESSAGE_PREFIX + Constants.SEPARATOR
                + from + Constants.SEPARATOR
                + to + Constants.SEPARATOR
                + received;
    }

    public String getFrom() {
        return from;
    }

    public String getTo() {
        return to;
    }

    public boolean isReceived() {
        return received;
    }
}
