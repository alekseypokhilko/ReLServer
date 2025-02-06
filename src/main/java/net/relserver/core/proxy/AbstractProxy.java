package net.relserver.core.proxy;

import net.relserver.core.Constants;
import net.relserver.core.api.Proxy;
import net.relserver.core.peer.*;
import net.relserver.core.port.UdpPort;
import net.relserver.core.util.Logger;

import java.net.DatagramPacket;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;

public abstract class AbstractProxy implements Proxy {
    protected final UdpPort port;
    protected final PeerPair peerPair;
    protected final PeerRegistry peerRegistry;
    protected State state = State.DISCONNECTED;
    protected long lastP2pPacketSentTime = System.currentTimeMillis();

    public AbstractProxy(UdpPort port, PeerPair peerPair, PeerRegistry peerRegistry) {
        this.peerRegistry = peerRegistry;
        this.port = port;
        this.peerPair = peerPair;

        attachToPorts();
        Logger.log("Proxy %s started with port: p2p=%s", peerPair.getPeer(), port.getId());
    }

    protected void attachToPorts() {
        this.port.setOnPacketReceived(this::onPacketReceived);
    }

    public void sendWithRetry(DatagramPacket packet) {
        CompletableFuture.runAsync(() -> {
            //String message = new String(packet.getData()).trim(); //debug
            int count = 0;
            while (!Thread.interrupted()) {
                //packets can reach their destination after receiving information about the remote proxy
                Peer actual = peerRegistry.get(peerPair.getRemotePeer().getId());

                try {
                    port.send(packet, actual.getHost());
                    break;
                } catch (Exception e) {
                    try {
                        Thread.sleep(200L); //todo
                    } catch (Exception ignore) {
                        break;
                    }
                }
                count++;
                if (count > 10) { //todo
                    Logger.log("Not found a remote peer. Retry limit exceeds for sending to: %s", actual);
                    break;
                }
            }
        });
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
            getPort().send(packet, remotePeer.getHost());
        }
    }

    @Override
    public void setRemotePeer(Peer remotePeer) {
        Logger.log("Proxy %s changed remote peer to %s", getId(), remotePeer);
        this.peerPair.setRemotePeer(remotePeer);
        this.sendHandshakePacket(remotePeer);
    }

    @Override
    public Peer getRemotePeer() {
        return peerPair.getRemotePeer();
    }

    @Override
    public Peer getPeer() {
        return peerPair.getPeer();
    }

    @Override
    public UdpPort getPort() {
        return port;
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
        port.close();
    }

    protected boolean isHandshake(byte[] packetData) {
        return packetData.length > 3
                && packetData[0] == Constants.HANDSHAKE_PREFIX_BYTES[0]
                && packetData[1] == Constants.HANDSHAKE_PREFIX_BYTES[1]
                && packetData[2] == Constants.HANDSHAKE_PREFIX_BYTES[2];
    }
}
