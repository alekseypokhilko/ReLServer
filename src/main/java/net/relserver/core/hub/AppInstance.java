package net.relserver.core.hub;

import net.relserver.core.Constants;
import net.relserver.core.peer.Mode;
import net.relserver.core.peer.Peer;
import net.relserver.core.peer.State;

import java.net.Socket;

public class AppInstance {
    private final String hostId;
    private final Socket socket;
    private final String peerManagerId;
    private final String appId;
    private Peer clientRouter;
    private Peer serverRouter;

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

    public Peer getClientRouter() {
        return clientRouter;
    }

    public Peer getServerRouter() {
        return serverRouter;
    }

    public void setRouter(Peer router) {
        if (Mode.CLIENT == router.getMode()) {
            this.clientRouter = router;
        }
        if (Mode.SERVER == router.getMode()) {
            this.serverRouter = router;
        }
    }

    public void removeRouter(Peer router) {
        if (Mode.CLIENT == router.getMode()) {
            this.clientRouter = null;
        }
        if (Mode.SERVER == router.getMode()) {
            this.serverRouter = null;
        }
    }

    public void onPeerStateChanged(Peer peer) {
        if (!peer.isRouter()) {
            return;
        }
        if (State.CONNECTED == peer.getState()) {
            setRouter(peer);
        }
        if (State.DISCONNECTED == peer.getState()) {
            removeRouter(peer);
        }
    }
}