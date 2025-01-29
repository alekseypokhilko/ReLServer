package net.relserver.core.peer;

import java.net.InetAddress;
import java.net.UnknownHostException;

public final class Host {
    private final String ip;
    private final int port;
    private final Protocol protocol;
    private final InetAddress _inetAddress;

    public Host(String ip, int port, Protocol protocol) {
        try {
            this._inetAddress = InetAddress.getByName(ip); //todo lazy
            this.ip = ip;
            this.port = port;
            this.protocol = protocol;
        } catch (UnknownHostException e) {
            throw new RuntimeException(e);
        }
    }

    public Host(InetAddress ip, int port, Protocol protocol) {
        this._inetAddress = ip;
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
