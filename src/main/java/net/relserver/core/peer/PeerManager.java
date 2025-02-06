package net.relserver.core.peer;

import net.relserver.core.*;
import net.relserver.core.api.Id;
import net.relserver.core.api.model.Operation;
import net.relserver.core.api.model.PeerManagerRegistrationRequest;
import net.relserver.core.api.model.Request;
import net.relserver.core.app.App;
import net.relserver.core.http.SocketRequest;
import net.relserver.core.port.UdpPort;
import net.relserver.core.util.Logger;
import net.relserver.core.util.Utils;

import java.net.DatagramPacket;
import java.nio.charset.StandardCharsets;

public class PeerManager implements Id {
    private final String id;
    private final PeerManagerClient client;
    private final PeerRegistry peerRegistry;
    private final Host service;
    private final Host registrationServiceHost;
    private final App app;
    Settings settings;

    public PeerManager(Settings settings, App app, PeerRegistry peerRegistry) {
        this.id = Id.generateId(Constants.PEER_MANAGER_PREFIX);
        this.peerRegistry = peerRegistry;
        this.app = app;
        this.settings = settings;

        String hubIp = HubLoader.getHubIps(settings).get(0);
        int hubServicePort = settings.getInt(Settings.hubServicePort);
        int hubRegistrationPort = settings.getInt(Settings.hubRegistrationPort);
        this.service = new Host(hubIp, hubServicePort, Protocol.TCP);
        this.registrationServiceHost = new Host(hubIp, hubRegistrationPort, Protocol.UDP);

        client = new PeerManagerClient();
    }

    public void start() {
        try {
            client.startConnection(service);
            String message = Utils.createRequest(Operation.REGISTER_PEER_MANAGER, new PeerManagerRegistrationRequest(getId(), app.getId()));
            client.sendMessage(message);
            client.subscribeOnPeers(this::receiveRemotePeer);
        } catch (Exception e) {
            Logger.log("Disconnected from Hub: %s", e.getMessage()); //todo throw from main thread
            client.stop();
            throw new IllegalStateException(e.getMessage(), e);
        }
        Logger.log("Peer manager %s started with app=%s", id, app);
    }

    private void receiveRemotePeer(String peerInfo) {
        if (peerInfo == null || peerInfo.isEmpty()) {
            return;
        }
        Logger.log("Peer manager %s received peer: %s", id, peerInfo);
        try {
            Peer peer = Utils.fromJson(peerInfo, Peer.class);
            if (!app.getId().equals(peer.getAppId()) && !(!peer.isRouter() && id.equals(peer.getPeerManagerId()))) {
                return;
            }
            peerRegistry.onPeerChanged(peer);
        } catch (Exception e) {
            e.printStackTrace();//todo
        }
    }

    public void notifyPeerState(Peer peer, State state, UdpPort port) {
        peer.setState(state);
        String message = Utils.toJson(peer);
        byte[] sendData = message.getBytes(StandardCharsets.UTF_8);
        DatagramPacket packet = new DatagramPacket(sendData, sendData.length);
        port.send(packet, this.registrationServiceHost);
        Logger.log("Proxy %s state changed: %s", peer.getId(), message);
    }

    @Override
    public String getId() {
        return id;
    }

    public void stop() {
        SocketRequest.execute(
                HubLoader.getHubIps(settings).get(0), //todo remove static
                settings.getInt(Settings.hubServicePort),
                new Request(Operation.DISCONNECT_PEER_MANAGER, Utils.toJson(new PeerManagerRegistrationRequest(getId(), app.getId())))
        );
        client.stop();
    }
}
