package net.relserver.core.proxy;

import net.relserver.core.peer.PeerRegistry;
import net.relserver.core.peer.Peer;
import net.relserver.core.peer.PeerPair;
import net.relserver.core.port.PortPair;
import net.relserver.core.util.Utils;

import java.net.DatagramPacket;

/**
 * A proxy for a specific client and a specific remote server
 */
public class ServerProxy extends AbstractProxy {
    public ServerProxy(PortPair portPair, PeerPair peerPair, PeerRegistry peerRegistry) {
        super(portPair, peerPair, peerRegistry);
    }

    @Override
    protected void attachToPorts() {
        this.portPair.getTargetPort().setOnPacketReceived(this::processResponse);
        this.portPair.getP2pPort().setOnPacketReceived(this::processRequest);
    }

    protected void processResponse(DatagramPacket packet) {
        //from local server -> remote client
        Peer remotePeer = this.peerPair.getRemotePeer();
        if (remotePeer.getHost() == null) {
            Utils.sendWithRetry(portPair.getP2pPort(), packet, peerPair.getRemotePeer(), 10, 100L, peerRegistry::get);
        } else {
            portPair.getP2pPort().send(packet, remotePeer.getHost());
        }
    }

    public void processRequest(DatagramPacket packet) {
        if (Utils.isHandshake(packet.getData())) {
            return;
        }
        //from remote client -> local server
        portPair.getTargetPort().send(packet, this.peerPair.getPeer().getHost());
    }
}