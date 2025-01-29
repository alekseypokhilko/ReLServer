package net.relserver.core.server;

import net.relserver.core.Utils;
import net.relserver.core.peer.Peer;
import net.relserver.core.peer.PeerPair;
import net.relserver.core.port.PortPair;
import net.relserver.core.port.UdpPort;
import net.relserver.core.proxy.Proxy;

import java.net.DatagramPacket;
import java.util.function.Function;

/**
 * A proxy for a specific client and a specific remote server
 */
public class ServerProxy implements Proxy {
    private final PortPair portPair;
    private final PeerPair peerPair;
    private final Function<String, Peer> peerSupplier;

    public ServerProxy(PortPair portPair, PeerPair peerPair, Function<String, Peer> peerSupplier) {
        this.portPair = portPair;
        this.portPair.getP2pPort().setOnPacketReceived(this::processRequest);
        this.portPair.getTargetPort().setOnPacketReceived(this::processResponse);

        this.peerPair = peerPair;
        this.peerSupplier = peerSupplier;
        Utils.log("Proxy " + peerPair.getPeer() + " started with ports: p2p=" + portPair.getP2pPort().getId() + ", server=" + portPair.getTargetPort().getId());
    }

    private void processResponse(DatagramPacket packet) {
        //from local server -> remote client
        Peer remotePeer = this.peerPair.getRemotePeer();
        if (remotePeer.getHost() == null){
            Peer clientPeer = peerSupplier.apply(remotePeer.getId());
            if (clientPeer != null && clientPeer.getHost() != null) {
                remotePeer.setHost(clientPeer.getHost());
                Utils.log("Cannot get host for peer: " + remotePeer);
            }
        }
        portPair.getP2pPort().send(packet, remotePeer.getHost());
    }

    private void processRequest(DatagramPacket packet) {
        //from remote client -> local server
        portPair.getTargetPort().send(packet, this.peerPair.getPeer().getHost());
    }

    @Override
    public void setRemotePeer(Peer remotePeer) {
        Utils.log("Proxy " + getId() + " changed remote peer from " + this.peerPair.getRemotePeer() + " to " + remotePeer);
        this.peerPair.setRemotePeer(remotePeer);
        this.sendHandshakePacket(remotePeer);
    }

    @Override
    public Peer getInfo() {
        return peerPair.getPeer();
    }

    public Peer getRemotePeer() {
        return peerPair.getRemotePeer();
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
    public void close() {
        portPair.getTargetPort().close();
        portPair.getP2pPort().close();
    }
}