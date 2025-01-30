package net.relserver.core.peer;

import java.net.InetAddress;

public final class Host {
    private final String ip;
    private final int port;
    private final Protocol protocol;

    public Host(String ip, int port, Protocol protocol) {
        this.ip = ip;
        this.port = port;
        this.protocol = protocol;
    }

    public Host(InetAddress ip, int port, Protocol protocol) {
        this.ip = ip.getHostAddress();
        this.port = port;
        this.protocol = protocol;
    }

    public static Host ofId(String id) {
        String[] tokens = id.split(":");
        return new Host(tokens[1], Integer.parseInt(tokens[2]), Protocol.valueOf(tokens[0]));
    }

    public static String toId(String ip, int port, Protocol protocol) {
        return protocol + ":" + ip + ":" + port;
    }

    public String getIp() {
        return ip;
    }

    public int getPort() {
        return port;
    }

    public Protocol getProtocol() {
        return protocol;
    }

    public String getId() {
        return protocol + ":" + ip + ":" + port;
    }
}
