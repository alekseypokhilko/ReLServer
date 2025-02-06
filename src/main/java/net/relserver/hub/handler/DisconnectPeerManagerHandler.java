package net.relserver.hub.handler;

import net.relserver.core.api.model.Operation;
import net.relserver.core.api.model.PeerManagerRegistrationRequest;
import net.relserver.core.util.Logger;
import net.relserver.core.util.Utils;
import net.relserver.hub.Client;
import net.relserver.hub.PeerRegistry;

public class DisconnectPeerManagerHandler implements RequestHandler {
    private final PeerRegistry peerRegistry;

    public DisconnectPeerManagerHandler(PeerRegistry peerRegistry) {
        this.peerRegistry = peerRegistry;
    }

    @Override
    public void handle(Client client, String message) {
        PeerManagerRegistrationRequest request = Utils.fromJson(message, PeerManagerRegistrationRequest.class);
        client.setPeerManagerId(request.getPmId());
        client.setAppId(request.getAppId());
        client.sendMessage(Operation.DISCONNECT_PEER_MANAGER.name());
        peerRegistry.onPeerManagerDisconnects(client);
        Logger.log("Peer manager %s disconnected: appId=%s host=%s", client.getPeerManagerId(), client.getAppId(), client.getHostId());
    }

    @Override
    public Operation getOperation() {
        return Operation.DISCONNECT_PEER_MANAGER;
    }
}
