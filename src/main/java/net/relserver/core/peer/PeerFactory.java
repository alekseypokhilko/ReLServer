package net.relserver.core.peer;

import net.relserver.core.Constants;
import net.relserver.core.api.Id;
import net.relserver.core.app.App;

public class PeerFactory {
    private final App app;
    private final PeerManager peerManager;

    public PeerFactory(App app, PeerManager peerManager) {
        this.app = app;
        this.peerManager = peerManager;
    }

    public PeerPair clientRouterPeer() {
        Peer peer = Peer.of(peerManager.getId(), Id.generateId(Constants.ROUTER_PREFIX), State.CONNECTED, Mode.CLIENT, null, null, app.getId(), null);
        return new PeerPair(peer, null);
    }

    public PeerPair serverRouterPeer() {
        Peer peer = Peer.of(peerManager.getId(), Id.generateId(Constants.ROUTER_PREFIX), State.CONNECTED, Mode.SERVER, null, null, app.getId(), null);
        return new PeerPair(peer, null);
    }

    public PeerPair createClientPeerPair(Peer remoteServer) {
        String peerId = Id.generateId(Constants.CLIENT_PREFIX);
        String remotePeerId = Id.generateId(Constants.SERVER_PREFIX);
        Peer peer = Peer.of(
                peerManager.getId(), peerId,
                State.CONNECTED, Mode.CLIENT,
                remoteServer.getPeerManagerId(), remotePeerId,
                app.getId(), null
        );
        Peer remotePeer = Peer.of(
                remoteServer.getPeerManagerId(), remotePeerId,
                State.CONNECTED, Mode.SERVER,
                peerManager.getId(), peerId,
                app.getId(), null
        );
        return new PeerPair(peer, remotePeer);
    }

    public PeerPair serverPeerPair(String localServerIp, Peer peerRequest) {
        if (!app.getId().equals(peerRequest.getAppId())) {
            throw new IllegalArgumentException("Peer request have different appId");
        }

        Host appHost = new Host(localServerIp, app.getPort(), Protocol.UDP);
        Peer peer = Peer.of(
                peerManager.getId(), peerRequest.getId(),
                State.CONNECTED, Mode.SERVER,
                peerRequest.getRemotePeerManagerId(), peerRequest.getRemotePeerId(),
                app.getId(), appHost
        );
        Peer remotePeer = Peer.of(
                peerRequest.getRemotePeerManagerId(), peerRequest.getRemotePeerId(),
                State.CONNECTED, Mode.CLIENT,
                peerManager.getId(), peerRequest.getId(),
                app.getId(), null
        );
        return new PeerPair(peer, remotePeer);
    }
}
