package net.relserver.core.peer;

import net.relserver.core.app.App;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public class PeerRegistry {
    private final App app;
    private final List<Consumer<Peer>> onNewRemotePeerActions = new ArrayList<>();
    //peerId <=> peer
    private final Map<String, Peer> remotePeers = new ConcurrentHashMap<>();

    public PeerRegistry(App app) {
        this.app = app;
    }

    //todo add remotePeer to queue and run this method async
    public void onPeerChanged(Peer remotePeer) {
        if (!app.getId().equals(remotePeer.getAppId())) {
            return;
        }
        if (State.CONNECTED == remotePeer.getState()) {
            remotePeers.put(remotePeer.getId(), remotePeer);
        } else {
            remotePeers.remove(remotePeer.getId());
        }
        notifyRemotePeerState(remotePeer);
    }

    private void notifyRemotePeerState(Peer peer) {
        for (Consumer<Peer> action : onNewRemotePeerActions) {
            action.accept(peer);
        }
    }

    public void subscribeOnRemotePeerChanged(Consumer<Peer> action) {
        this.onNewRemotePeerActions.add(action);
    }

    public Peer get(String peerId) {
        return remotePeers.get(peerId);
    }

    public Map<String, Peer> getAll() {
        return remotePeers;
    }
}
