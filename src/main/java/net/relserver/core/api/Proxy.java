package net.relserver.core.api;

import net.relserver.core.peer.PeerPair;
import net.relserver.core.peer.State;
import net.relserver.core.port.PortPair;
import net.relserver.core.peer.Peer;

import java.net.DatagramPacket;

public interface Proxy extends Id {

    void processRequest(DatagramPacket packet);

    State getState();
    PeerPair getPeerPair();

    PortPair getPortPair();

    void setRemotePeer(Peer peer);

    Peer getRemotePeer();

    void sendHandshakePacket(Peer peer);

    @Override
    default String getId() {
        return getPeerPair().getPeer().getId();
    }

    void stop();
}
