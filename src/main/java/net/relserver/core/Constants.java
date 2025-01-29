package net.relserver.core;

public final class Constants {
    public static final String GITHUB_BASE_URL = "https://raw.githubusercontent.com";
    public static final String APPLICATIONS_CONFIG_URL = "/alekseypokhilko/ReLServer/refs/heads/main/src/main/resources/applications.json";
    public static final String HUBS_URL = "/alekseypokhilko/ReLServer/refs/heads/main/src/main/resources/hubs.json";
    public static final String SEPARATOR = ";";
    public static final char NEW_LINE = '\n';
    public static final String HANDSHAKE_MESSAGE_PREFIX = "HANDSHAKE";
    public static final String HUB_PREFIX = "H";
    public static final String PEER_MANAGER_PREFIX = "P";
    public static final String ROUTER_PREFIX = "R";
    public static final String CLIENT_PREFIX = "C";
    public static final String SERVER_PREFIX = "S";
    public static final String PORT_PREFIX = "PORT-";

    private Constants() {
        throw new IllegalStateException();
    }
}
