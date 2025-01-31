package net.relserver.core.proxy;

import net.relserver.core.Constants;
import net.relserver.core.api.Proxy;
import net.relserver.core.peer.*;
import net.relserver.core.port.PortPair;
import net.relserver.core.port.UdpPort;
import net.relserver.core.util.Logger;
import net.relserver.core.util.Utils;

import java.net.DatagramPacket;
import java.nio.charset.StandardCharsets;

public abstract class AbstractProxy implements Proxy {
    protected final PortPair portPair;
    protected final PeerPair peerPair;
    protected final PeerRegistry peerRegistry;

    public AbstractProxy(PortPair portPair, PeerPair peerPair, PeerRegistry peerRegistry) {
        this.peerRegistry = peerRegistry;
        this.portPair = portPair;
        this.peerPair = peerPair;

        attachToPorts();
        Logger.log(
                "Proxy %s started with ports: p2p=%s, target=%s",
                peerPair.getPeer(),
                portPair.getP2pPort().getId(),
                this.portPair.getTargetPort() == null ? null : this.portPair.getTargetPort().getId()
        );
    }

    protected abstract void attachToPorts();

    protected abstract void processResponse(DatagramPacket packet);

    public abstract void processRequest(DatagramPacket packet);

    public void sendWithRetry(DatagramPacket packet) {
        Utils.sendWithRetry(portPair.getP2pPort(), packet, peerPair.getRemotePeer(), 10, 100L, peerRegistry::get);
    }

    @Override
    public void sendHandshakePacket(Peer peer) {
        String msg = Constants.HANDSHAKE_MESSAGE_PREFIX + ":" + this.getId() + "<=>" + peer.getId();
        Logger.log("Sending handshake: %s", msg);
        byte[] senData = msg.getBytes(StandardCharsets.UTF_8);
        DatagramPacket packet = new DatagramPacket(senData, senData.length);
        Utils.sendWithRetry(getPortPair().getP2pPort(), packet, peer, 10, 100L, peerRegistry::get);
    }

    @Override
    public void setRemotePeer(Peer remotePeer) {
        Logger.log("Proxy %s changed remote peer from %s to %s", getId(), peerPair.getRemotePeer(), remotePeer);
        this.peerPair.setRemotePeer(remotePeer);
        this.sendHandshakePacket(remotePeer);
    }

    @Override
    public Peer getRemotePeer() {
        return peerPair.getRemotePeer();
    }

    @Override
    public PeerPair getPeerPair() {
        return peerPair;
    }

    @Override
    public PortPair getPortPair() {
        return portPair;
    }

    @Override
    public void stop() {
        portPair.getP2pPort().close();
        UdpPort targetPort = portPair.getTargetPort();
        if (targetPort != null) {
            targetPort.close();
        }
    }
}
