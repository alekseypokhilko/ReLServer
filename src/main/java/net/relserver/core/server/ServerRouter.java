package net.relserver.core.server;

import net.relserver.core.Constants;
import net.relserver.core.util.Logger;
import net.relserver.core.peer.*;
import net.relserver.core.port.PortFactory;
import net.relserver.core.port.PortPair;
import net.relserver.core.port.UdpPort;
import net.relserver.core.proxy.Proxy;

import java.net.DatagramPacket;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

public class ServerRouter implements Proxy {
    private final PortFactory portFactory;
    private final PeerFactory peerFactory;
    private final UdpPort p2pPort;
    private final Peer peer;
    private final String localServerIp;
    private final PeerManager peerManager;
    private final Map<String, ServerProxy> proxies = new ConcurrentHashMap<>();
    private final Map<String, Peer> clients = new ConcurrentHashMap<>();

    public ServerRouter(PortFactory portFactory, PeerFactory peerFactory, String localServerIp, PeerManager peerManager) {
        this.portFactory = portFactory;
        this.peerManager = peerManager;
        this.localServerIp = localServerIp;
        this.peerFactory = peerFactory;

        this.p2pPort = portFactory.udpPort(this::processRequest);
        this.peer = peerFactory.serverRouterPeer();

        peerManager.subscribeOnRemotePeerChanged(this::onPeerChanged);
        peerManager.notifyPeerState(this, State.CONNECTED);
        Logger.log("Router %s started with ports: p2p=%s", peer.getId(), p2pPort.getId());
    }

    private void processRequest(DatagramPacket packet) {
        String peerInfo = new String(packet.getData(), StandardCharsets.UTF_8).trim();
        Logger.log("Router %s received create proxy request: '%s'", peer.getId(), peerInfo);
        try {
            if (peerInfo.isEmpty() || peerInfo.startsWith(Constants.HANDSHAKE_MESSAGE_PREFIX)) {
                return;
            }
            ServerProxy proxy = this.proxies.get(peer.getId());
            if (proxy == null) {
                PeerPair peers = peerFactory.serverPeerPair(peerInfo, localServerIp);
                PortPair portPair = portFactory.pair();
                ServerProxy serverProxy = new ServerProxy(portPair, peers, clients::get);
                this.proxies.put(serverProxy.getId(), serverProxy);

                peerManager.notifyPeerState(serverProxy, State.CONNECTED);
            }
        } catch (ArrayIndexOutOfBoundsException e) {
            Logger.log("Illegal peer request: '%s'", peerInfo);
        } catch (Exception e) {
            e.printStackTrace(); //todo
        }
    }

    private void onPeerChanged(Peer peer) {
        try {
            if (Mode.CLIENT != peer.getMode()) {
                return;
            }

            Logger.log("Server %s received peer: %s", getId(), peer);
            if (State.DISCONNECTED == peer.getState()) {
                onPeerDisconnected(peer);
                return;
            }

            clients.put(peer.getId(), peer);
            if (!peer.isRouter()) {
                updateProxyRemotePeer(peer);
            } else {
                sendHandshakePacket(peer);
            }
        } catch (Exception e) {
            e.printStackTrace(); //todo
        }
    }

    private void updateProxyRemotePeer(Peer peer) {
        ServerProxy proxy = proxies.get(peer.getRemotePeerId());
        if (proxy != null) {
            proxy.setRemotePeer(peer);
        }
    }

    private void onPeerDisconnected(Peer peer) {
        clients.remove(peer.getId());
        Iterator<Map.Entry<String, ServerProxy>> proxyIterator = proxies.entrySet().iterator();
        while (proxyIterator.hasNext()) {
            ServerProxy proxy = proxyIterator.next().getValue();
            if (proxy.getRemotePeer().getPeerManagerId().equals(peer.getPeerManagerId())) {
                proxies.remove(proxy.getId());
                proxy.close();
            }
        }
    }

    @Override
    public Peer getInfo() {
        return peer;
    }

    @Override
    public UdpPort getPort() {
        return p2pPort;
    }

    @Override
    public void setRemotePeer(Peer peer) {
        //ignore
    }

    @Override
    public Function<String, Peer> getPeerSupplier() {
        return clients::get;
    }

    @Override
    public void close() {
        for (Map.Entry<String, ServerProxy> proxy : proxies.entrySet()) {
            ServerProxy serverProxy = proxy.getValue();
            peerManager.notifyPeerState(serverProxy, State.DISCONNECTED);
            serverProxy.close();
        }
        peerManager.notifyPeerState(this, State.DISCONNECTED);
        p2pPort.close();
    }
}