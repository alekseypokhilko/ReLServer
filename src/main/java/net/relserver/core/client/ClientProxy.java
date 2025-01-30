package net.relserver.core.client;

import net.relserver.core.util.Logger;
import net.relserver.core.util.Utils;
import net.relserver.core.peer.Host;
import net.relserver.core.peer.Peer;
import net.relserver.core.peer.PeerPair;
import net.relserver.core.peer.Protocol;
import net.relserver.core.port.PortPair;
import net.relserver.core.port.UdpPort;
import net.relserver.core.api.Proxy;

import java.net.DatagramPacket;
import java.util.function.Function;

/**
 * A proxy for a specific client and a specific remote server
 */
public class ClientProxy implements Proxy {
    private final PortPair portPair;
    private final PeerPair peerPair;
    private final Function<String, Peer> peerSupplier;

    public ClientProxy(PortPair portPair, PeerPair peerPair, Function<String, Peer> peerSupplier) {
        this.peerSupplier = peerSupplier;
        this.portPair = portPair;

        this.portPair.getTargetPort().setOnPacketReceived(this::processRequest);
        this.portPair.getP2pPort().setOnPacketReceived(this::processResponse);

        this.peerPair = peerPair;
        Logger.log("Proxy %s started with ports: p2p=%s, server=%s", peerPair.getPeer().getId(), portPair.getP2pPort().getId(), portPair.getTargetPort().getId());
    }

    private void processResponse(DatagramPacket packet) {
        if (Utils.isHandshake(packet.getData())) {
            return;
        }
        //from real remote server -> local client
        portPair.getTargetPort().send(packet, this.peerPair.getPeer().getHost());
    }

    private void processRequest(DatagramPacket packet) {
        if (peerPair.getPeer().getHost() == null) {
            peerPair.getPeer().setHost(new Host(packet.getAddress(), packet.getPort(), Protocol.UDP));
        }

        Peer remotePeer = peerPair.getRemotePeer();
        if (remotePeer.getHost() == null) {
            Peer clientPeer = peerSupplier.apply(remotePeer.getId());
            if (clientPeer != null && clientPeer.getHost() != null) {
                remotePeer.setHost(clientPeer.getHost());
                Logger.log("Cannot get host for peer: %s", remotePeer);
            }
        }
        //from local client -> real remote server
        portPair.getP2pPort().send(packet, remotePeer.getHost());
    }

    public void sendWithRetry(DatagramPacket packet) {
        Utils.sendWithRetry(portPair.getP2pPort(), packet, peerPair.getRemotePeer(), 10, 100L, peerSupplier);
    }

    @Override
    public void setRemotePeer(Peer remotePeer) {
        Logger.log("Proxy %s changed remote peer from %s to %s", getId(), peerPair.getRemotePeer(), remotePeer);
        this.peerPair.setRemotePeer(remotePeer);
        this.sendHandshakePacket(remotePeer);
    }

    public Peer getRemotePeer() {
        return peerPair.getRemotePeer();
    }

    @Override
    public Peer getInfo() {
        return peerPair.getPeer();
    }

    @Override
    public UdpPort getPort() {
        return portPair.getP2pPort();
    }

    @Override
    public Function<String, Peer> getPeerSupplier() {
        return peerSupplier;
    }

    @Override
    public void stop() {
        portPair.getTargetPort().close();
        portPair.getP2pPort().close();
    }
}
