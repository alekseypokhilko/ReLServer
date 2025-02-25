package net.relserver.core.proxy;

import net.relserver.core.api.Proxy;
import net.relserver.core.peer.Peer;
import net.relserver.core.peer.PeerManager;
import net.relserver.core.peer.State;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

public class ProxyRegistry {
    private final PeerManager peerManager;
    //proxyId <=> proxy
    private final Map<String, Proxy> registry = new ConcurrentHashMap<>();
    private final Thread worker;

    public ProxyRegistry(PeerManager peerManager) {
        this.peerManager = peerManager;
        worker = new Thread(this::managementLoop, "managementLoop");
        worker.start();
    }

    public void add(Proxy proxy) {
        registry.put(proxy.getId(), proxy);
        peerManager.notifyPeerState(proxy.getPeer(), State.CONNECTED, proxy.getPort());
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
            peerManager.notifyPeerState(proxy.getPeer(), State.DISCONNECTED, proxy.getPort());
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

    public void onPeerDisconnected(Peer peer) {
        Iterator<Map.Entry<String, Proxy>> proxyIterator = registry.entrySet().iterator();
        while (proxyIterator.hasNext()) {
            Proxy proxy = proxyIterator.next().getValue();
            if (proxy.getRemotePeer().getPeerManagerId().equals(peer.getPeerManagerId())) {
                remove(proxy.getId());
            }
        }
    }

    private void managementLoop() {
        while (!Thread.interrupted()) {
            for (Proxy proxy : registry.values()) {
                sendHandshakeIfNeeded(proxy);
                removeIfNotUsed(proxy);
            }
            try {
                Thread.sleep(1000L);//todo
            } catch (InterruptedException e) {
                break;
            }
        }
    }

    private void removeIfNotUsed(Proxy proxy) {
        if (!proxy.getPeer().isRouter()
                && State.CONNECTED == proxy.getState()
                && System.currentTimeMillis() - proxy.getLastP2pPacketSentTime() > TimeUnit.MINUTES.toMillis(5)) {
            //todo move duration to settings
            //todo find a low cost solution to remove unused proxies immediately after connecting to a game room
            remove(proxy.getId());
        }
    }

    private void sendHandshakeIfNeeded(Proxy proxy) {
        if (!proxy.getPeer().isRouter() && State.DISCONNECTED == proxy.getState()) {
            proxy.sendHandshakePacket(null);
        }
    }

    public void stop() {
        for (Proxy proxy : registry.values()) {
            proxy.stop();
        }
        worker.interrupt();
    }
}
