package net.relserver.core.peer;

import net.relserver.core.app.App;
import net.relserver.core.util.Logger;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

public class PeerRegistry {
    private final App app;
    private final Queue<Peer> queue = new LinkedList<>();
    //peerId <=> peer
    private final Map<String, Peer> remotePeers = new ConcurrentHashMap<>();
    private final List<Consumer<Peer>> onNewRemotePeerActions = new CopyOnWriteArrayList<>();
    private final Thread worker;

    public PeerRegistry(App app) {
        this.app = app;
        worker = new Thread(() -> {
            while (!Thread.interrupted()) {
                Peer peer = queue.poll();
                if (peer != null) {
                    processPeer(peer);
                }
            }
        });
        worker.start();
    }

    public void onPeerChanged(Peer remotePeer) {
        if (!app.getId().equals(remotePeer.getAppId())) {
            return;
        }
        boolean offered = queue.offer(remotePeer);
        if (!offered) {
            Logger.log("Peer queue is full. Peer lost: %s", remotePeer); //todo
        }
    }

    private void processPeer(Peer remotePeer) {
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

    public void stop() {
        worker.interrupt();
    }
}
