package net.relserver.core.hub;

import net.relserver.core.Constants;

import java.net.Socket;

public class AppInstance {
    private final String hostId;
    private final Socket socket;
    private final String peerManagerId;
    private final String appId;

    public AppInstance(String info, Socket socket) {
        String[] tokens = info.split(Constants.SEPARATOR);
        this.peerManagerId = tokens[0];
        this.appId = tokens[1];
        this.hostId = socket.getInetAddress().getHostAddress() + ":" + socket.getPort();
        this.socket = socket;
    }

    public String getHostId() {
        return hostId;
    }

    public String getPeerManagerId() {
        return peerManagerId;
    }

    public String getAppId() {
        return appId;
    }

    public Socket getSocket() {
        return socket;
    }
}