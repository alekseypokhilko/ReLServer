package net.relserver.core.proxy;

import net.relserver.core.peer.PeerRegistry;
import net.relserver.core.peer.Peer;
import net.relserver.core.peer.PeerPair;
import net.relserver.core.port.UdpPort;

import java.net.DatagramPacket;

/**
 * A proxy for a specific client and a specific remote server
 */
public class ServerProxy extends AbstractProxy {
    public ServerProxy(UdpPort port, PeerPair peerPair, PeerRegistry peerRegistry) {
        super(port, peerPair, peerRegistry);
    }

    @Override
    public void onPacketReceived(DatagramPacket packet) {
        if (peerPair.isRequestFromPeer(packet.getAddress().getHostAddress(), packet.getPort())) {
            //from local server -> remote client
            Peer remotePeer = this.peerPair.getRemotePeer();
            if (remotePeer.getHost() == null) {
                updateRemotePeer();
                sendWithRetry(packet);
            } else {
                port.send(packet, remotePeer.getHost());
            }
            lastP2pPacketSentTime = System.currentTimeMillis();
        } else {
            if (isHandshake(packet.getData())) {
                receiveHandshakePacket(packet);
                return;
            }
            //from remote client -> local server
            port.send(packet, this.peerPair.getPeer().getHost());
        }
    }
}