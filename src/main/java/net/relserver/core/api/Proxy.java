package net.relserver.core.api;

import net.relserver.core.peer.State;
import net.relserver.core.peer.Peer;
import net.relserver.core.port.UdpPort;

import java.net.DatagramPacket;

public interface Proxy extends Id {

    void onPacketReceived(DatagramPacket packet);

    State getState();

    long getLastP2pPacketSentTime();

    Peer getPeer();

    UdpPort getPort();

    void setRemotePeer(Peer peer);

    Peer getRemotePeer();

    void sendHandshakePacket(Peer peer);

    @Override
    default String getId() {
        return getPeer().getId();
    }

    void stop();
}
