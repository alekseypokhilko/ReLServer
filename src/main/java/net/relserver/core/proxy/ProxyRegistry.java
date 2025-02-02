package net.relserver.core.proxy;

import net.relserver.core.api.Proxy;
import net.relserver.core.peer.Peer;
import net.relserver.core.peer.PeerManager;
import net.relserver.core.peer.State;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class ProxyRegistry {
    private final PeerManager peerManager;
    //proxyId <=> proxy
    private final Map<String, Proxy> registry = new ConcurrentHashMap<>();
    private final Thread worker;

    public ProxyRegistry(PeerManager peerManager) {
        this.peerManager = peerManager;
        worker = new Thread(this::handshakeLoop, "handshakeLoop");
        worker.start();
    }

    public void add(Proxy proxy) {
        registry.put(proxy.getId(), proxy);
        peerManager.notifyPeerState(proxy, State.CONNECTED);
    }

    public Proxy get(String proxyId) {
        if (proxyId == null) {
            return null;
        }
        return registry.get(proxyId);
    }

    public String remove(String id) {
        Proxy proxy = registry.remove(id);
        if (proxy != null) {
            peerManager.notifyPeerState(proxy, State.DISCONNECTED);
            proxy.stop();
            return proxy.getId();
        }
        return null;
    }

    public void updateProxyRemotePeer(Peer peer) {
        if (State.CONNECTED == peer.getState()) {
            String remotePeerId = peer.getRemotePeerId();
            if (remotePeerId == null) {
                return;
            }
            Proxy proxy = registry.get(remotePeerId);
            if (proxy != null) {
                proxy.setRemotePeer(peer);
            }
        }
        if (State.DISCONNECTED == peer.getState()) {
            onPeerDisconnected(peer);
        }
    }

    public Set<String> onPeerDisconnected(Peer peer) {
        Iterator<Map.Entry<String, Proxy>> proxyIterator = registry.entrySet().iterator();
        Set<String> ids = new HashSet<>();
        while (proxyIterator.hasNext()) {
            Proxy proxy = proxyIterator.next().getValue();
            if (proxy.getRemotePeer().getPeerManagerId().equals(peer.getPeerManagerId())) {
                remove(proxy.getId());
                ids.add(proxy.getId());
            }
        }
        return ids;
    }

    private void handshakeLoop() {
        while (!Thread.interrupted()) {
            for (Proxy proxy : registry.values()) {
                if (!proxy.getPeerPair().getPeer().isRouter() && State.DISCONNECTED == proxy.getState()) {
                    proxy.sendHandshakePacket(null);
                }
            }
            try {
                Thread.sleep(1000L);//todo
            } catch (InterruptedException e) {
                break;
            }
        }
    }

    public void stop() {
        for (Proxy proxy : registry.values()) {
            proxy.stop();
        }
        worker.interrupt();
    }
}
