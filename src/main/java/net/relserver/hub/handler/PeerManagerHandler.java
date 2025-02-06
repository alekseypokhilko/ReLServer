package net.relserver.hub.handler;

import net.relserver.core.api.model.Operation;
import net.relserver.core.api.model.PeerManagerRegistrationRequest;
import net.relserver.core.util.Logger;
import net.relserver.core.util.Utils;
import net.relserver.hub.Client;
import net.relserver.hub.ClientRegistry;
import net.relserver.hub.PeerRegistry;

public class PeerManagerHandler implements RequestHandler {
    private final ClientRegistry clientRegistry;
    private final PeerRegistry peerRegistry;

    public PeerManagerHandler(ClientRegistry clientRegistry, PeerRegistry peerRegistry) {
        this.clientRegistry = clientRegistry;
        this.peerRegistry = peerRegistry;
    }

    @Override
    public void handle(Client client, String message) {
        PeerManagerRegistrationRequest request = Utils.fromJson(message, PeerManagerRegistrationRequest.class);
        client.setPeerManagerId(request.getPmId());
        client.setAppId(request.getAppId());
        client.setOnStop(peerRegistry::onPeerManagerDisconnects);
        clientRegistry.add(client);
        peerRegistry.sendAllPeers(client);
        Logger.log("Peer manager %s connected: appId=%s host=%s", client.getPeerManagerId(), client.getAppId(), client.getHostId());
    }

    @Override
    public Operation getOperation() {
        return Operation.REGISTER_PEER_MANAGER;
    }
}
