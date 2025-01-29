package net.relserver.core.peer;

public class PeerPair {
    private final Peer peer;
    private Peer remotePeer;

    public PeerPair(Peer peer, Peer remotePeer) {
        this.peer = peer;
        this.remotePeer = remotePeer;
    }

    public Peer getPeer() {
        return peer;
    }

    public Peer getRemotePeer() {
        return remotePeer;
    }

    public void setRemotePeer(Peer remotePeer) {
        this.remotePeer = remotePeer;
    }
}
