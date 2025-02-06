package net.relserver.hub;

import net.relserver.core.peer.Peer;
import net.relserver.core.peer.State;
import net.relserver.core.util.Utils;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public class ClientRegistry {
    //peerManagerId <=> instance
    private final Map<String, Client> clients = new ConcurrentHashMap<>();

    public void add(Client client) {
        this.clients.put(client.getPeerManagerId(), client);
    }

    public void remove(String peerManagerId) {
        clients.remove(peerManagerId);
    }

    public Client get(String remotePeerManagerId) {
        return clients.get(remotePeerManagerId);
    }

    public void stop() {
        for (Client client : clients.values()) {
            client.stop();
        }
    }

    public void notifyPeerStateChanged(Peer peer, State state) {
        if (peer == null) {
            return;
        }

        peer.setState(state);
        if (peer.getRemotePeerManagerId() != null) {
            notify(peer);
        } else {
            CompletableFuture.runAsync(() -> notifyAll(peer));
        }
    }

    private void notify(Peer peer) {
        Client client = get(peer.getRemotePeerManagerId());
        if (client != null) {
            client.sendMessage(Utils.toJson(peer));
        }
    }

    private void notifyAll(Peer peer) {
        for (Client client : clients.values()) {
            if (peer.getAppId().equals(client.getAppId())) {
                client.sendMessage(Utils.toJson(peer));
            }
        }
    }
}
