package net.relserver.hub;

import net.relserver.core.peer.Host;
import net.relserver.core.peer.Peer;
import net.relserver.core.peer.Protocol;
import net.relserver.core.peer.State;
import net.relserver.core.port.UdpPort;
import net.relserver.core.util.Logger;
import net.relserver.core.util.Utils;

import java.net.DatagramPacket;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

public class PeerRegistry {
    private final ClientRegistry clientRegistry;
    private final UdpPort registrationPort;
    private final Map<String, Peer> peers = new ConcurrentHashMap<>();
    private final Queue<DatagramPacket> peerRegistrationQueue = new ConcurrentLinkedQueue<>();
    private final Thread peerRegistrationWorker;

    public PeerRegistry(UdpPort registrationPort, ClientRegistry clientRegistry) {
        this.registrationPort = registrationPort;
        this.registrationPort.setOnPacketReceived(this::onUdpPeerRegistrationRequest);
        this.clientRegistry = clientRegistry;
        peerRegistrationWorker = new Thread(this::peerRegistrationWorkerLoop, "peerRegistrationWorker");
        peerRegistrationWorker.start();
    }

    private void onUdpPeerRegistrationRequest(DatagramPacket packet) {
        boolean offered = peerRegistrationQueue.offer(packet);
        if (!offered) {
            Logger.log("Peer registration queue is full. Peer lost: %s:%s", packet.getAddress().getHostAddress(), packet.getPort()); //todo
        }
    }

    private void peerRegistrationWorkerLoop() {
        while (!Thread.interrupted()) {
            DatagramPacket packet = peerRegistrationQueue.poll();
            if (packet != null) {
                processPeerRequest(packet);
            }
        }
    }

    private void processPeerRequest(DatagramPacket packet) {
        try {
            String message = new String(packet.getData(), StandardCharsets.UTF_8).trim();
            Logger.log("Hub received peer: %s", message);
            Host host = new Host(packet.getAddress().getHostAddress(), packet.getPort(), Protocol.UDP);
            Peer peer = Utils.fromJson(message, Peer.class);
            peer.setHost(host);

            if (State.CONNECTED == peer.getState()) {
                peers.put(peer.getId(), peer);
            } else {
                peers.remove(peer.getId());
            }

            clientRegistry.notifyPeerStateChanged(peer, peer.getState());
        } catch (Exception e) {
            e.printStackTrace();//todo
        }
    }

    public Collection<Peer> getAll() {
        return peers.values();
    }

    public void sendAllPeers(Client client) {
        for (Peer peer : peers.values()) {
            if (client.getAppId().equals(peer.getAppId())
                    && (peer.isRouter() || client.getPeerManagerId().equals(peer.getRemotePeerManagerId()))) {
                sendPeer(client, peer);
            }
        }
    }

    private void sendPeer(Client client, Peer peer) {
        try {
            client.sendMessage(Utils.toJson(peer));
        } catch (Exception e) {
            Logger.log("Error while closing sending peer: %s", e.getMessage());
            onPeerManagerDisconnects(client);
        }
    }

    public void onPeerManagerDisconnects(Client client) {
        clientRegistry.remove(client.getPeerManagerId());
        for (Peer peer : peers.values()) {
            if (peer.getPeerManagerId().equals(client.getPeerManagerId())) {
                peers.remove(peer.getId());
                if (peer.isRouter()) { //other peers stay connected
                    clientRegistry.notifyPeerStateChanged(peer, State.DISCONNECTED);
                }
            }
        }
        Logger.log("Peer manager %s disconnected: %s", client.getPeerManagerId(), client.getHostId());
        client.stop();
    }

    public void stop() {
        peerRegistrationWorker.interrupt();
        registrationPort.close();
    }
}
