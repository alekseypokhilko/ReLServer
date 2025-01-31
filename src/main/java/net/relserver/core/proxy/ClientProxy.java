package net.relserver.core.proxy;

import net.relserver.core.peer.*;
import net.relserver.core.util.Utils;
import net.relserver.core.port.PortPair;

import java.net.DatagramPacket;

/**
 * A proxy for a specific client and a specific remote server
 */
public class ClientProxy extends AbstractProxy {

    public ClientProxy(PortPair portPair, PeerPair peerPair, PeerRegistry peerRegistry) {
        super(portPair, peerPair, peerRegistry);
    }

    @Override
    protected void attachToPorts() {
        this.portPair.getTargetPort().setOnPacketReceived(this::processRequest);
        this.portPair.getP2pPort().setOnPacketReceived(this::processResponse);
    }

    protected void processResponse(DatagramPacket packet) {
        if (Utils.isHandshake(packet.getData())) {
            return;
        }
        //from real remote server -> local client
        portPair.getTargetPort().send(packet, this.peerPair.getPeer().getHost());
    }

    public void processRequest(DatagramPacket packet) {
        if (peerPair.getPeer().getHost() == null) {
            peerPair.getPeer().setHost(new Host(packet.getAddress(), packet.getPort(), Protocol.UDP));
        }

        Peer remotePeer = peerPair.getRemotePeer();
        if (remotePeer.getHost() == null) {
            sendWithRetry(packet);
        } else {
            //from local client -> real remote server
            portPair.getP2pPort().send(packet, remotePeer.getHost());
        }
    }
}
