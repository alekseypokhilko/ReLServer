package net.relserver.core.proxy;

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
    protected State state = State.DISCONNECTED;
    protected long lastP2pPacketSentTime = System.currentTimeMillis();

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

    protected void updateRemotePeer() {
        Peer remotePeer = peerPair.getRemotePeer();
        if (remotePeer == null) {
            return;
        }
        Peer actualRemotePeer = peerRegistry.get(remotePeer.getRemotePeerId());
        if (actualRemotePeer != null && actualRemotePeer.getHost() != null) {
            peerPair.setRemotePeer(actualRemotePeer);
        }
    }

    @Override
    public void sendHandshakePacket(Peer peer) {
        Peer remotePeer = peer == null ? peerPair.getRemotePeer() : peer;
        Handshake handshake = new Handshake(this.getId(), remotePeer.getId(), State.CONNECTED == state);
        sendHandshake(remotePeer, handshake);
    }

    protected void receiveHandshakePacket(DatagramPacket packet) {
        if (State.CONNECTED == state) {
            return;
        }
        String handshakeMessage = new String(packet.getData()).trim();
        Logger.log("Received handshake: %s", handshakeMessage);
        Handshake handshake = Handshake.of(handshakeMessage);
        Peer remotePeer = peerPair.getRemotePeer();
        if (!remotePeer.getId().equals(handshake.getFrom()) || !this.getId().equals(handshake.getTo())) {
            return;
        }
        if (handshake.isReceived() && State.DISCONNECTED == state) {
            state = State.CONNECTED;
            Logger.log("Proxy %s changed state to %s", getId(), state);
        }
        Handshake handshakeResponse = new Handshake(this.getId(), remotePeer.getId(), true);
        sendHandshake(remotePeer, handshakeResponse);
    }

    private void sendHandshake(Peer remotePeer, Handshake handshake) {
        String msg = handshake.toString();
        Logger.log("Sending handshake: %s", msg);
        byte[] senData = msg.getBytes(StandardCharsets.UTF_8);
        DatagramPacket packet = new DatagramPacket(senData, senData.length);
        if (remotePeer.getHost() != null) {
            getPortPair().getP2pPort().send(packet, remotePeer.getHost());
        }
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
    public State getState() {
        return state;
    }

    @Override
    public long getLastP2pPacketSentTime() {
        return lastP2pPacketSentTime;
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
