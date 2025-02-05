package net.relserver.core.proxy;

import net.relserver.core.peer.*;
import net.relserver.core.port.UdpPort;

import java.net.DatagramPacket;

/**
 * A proxy for a specific client and a specific remote server
 */
public class ClientProxy extends AbstractProxy {

    public ClientProxy(UdpPort port, PeerPair peerPair, PeerRegistry peerRegistry) {
        super(port, peerPair, peerRegistry);
    }

    public void onPacketReceived(DatagramPacket packet) {
        if (peerPair.isRequestFromPeer(packet.getAddress().getHostAddress(), packet.getPort())) {
            if (peerPair.getPeer().getHost() == null) {
                peerPair.getPeer().setHost(new Host(packet.getAddress(), packet.getPort(), Protocol.UDP));
            }
            Peer remotePeer = peerPair.getRemotePeer();
            if (remotePeer.getHost() == null) {
                updateRemotePeer();
                sendWithRetry(packet);
            } else {
                //from local client -> real remote server
                port.send(packet, remotePeer.getHost());
            }
            lastP2pPacketSentTime = System.currentTimeMillis();
        } else {
            if (isHandshake(packet.getData())) {
                receiveHandshakePacket(packet);
                return;
            }
            //from real remote server -> local client
            port.send(packet, this.peerPair.getPeer().getHost());
        }
    }
}
