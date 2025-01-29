package net.relserver.core.client;

import net.relserver.core.Utils;
import net.relserver.core.peer.*;
import net.relserver.core.port.PortFactory;
import net.relserver.core.port.PortPair;
import net.relserver.core.port.UdpPort;
import net.relserver.core.proxy.Proxy;

import java.net.DatagramPacket;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.function.Function;

/**
 * Emulates a remote server port in a local network.
 * Accepts client connections and creates a required proxies for all remote servers.
 */
public class ClientRouter implements Proxy {
    private final PortPair portPair;
    private final PortFactory portFactory;
    private final PeerFactory peerFactory;
    private final PeerManager peerManager;
    private final Peer peer;

    //peerId <=> proxy
    private final Map<String, Peer> servers = new ConcurrentHashMap<>();
    //proxyId <=> proxy
    private final Map<String, ClientProxy> proxies = new ConcurrentHashMap<>();
    //host <-> proxyIds todo Map<Host, Set<String>>
    private final Map<String, Set<String>> clientProxyIds = new ConcurrentHashMap<>();

    public ClientRouter(PortFactory portFactory, PeerFactory peerFactory, PeerManager peerManager) {
        this.peerManager = peerManager;
        this.portFactory = portFactory;
        this.peerFactory = peerFactory;

        this.portPair = portFactory.clientRouterPair(this::onP2pPacketReceived, this::onRouterPacketReceived);
        this.peer = peerFactory.clientRouterPeer();

        peerManager.notifyPeerState(this, State.CONNECTED);
        peerManager.subscribeOnRemotePeerChanged(this::onPeerChanged);

        Utils.log("Router " + this.peer.getId() + " started with ports: listen=" + portPair.getTargetPort().getId() + ", p2p=" + portPair.getP2pPort().getId());
    }

    private void onPeerChanged(Peer peer) {
        try {
            if (Mode.SERVER != peer.getMode()) {
                return;
            }

            Utils.log("Client " + getId() + " received peer: " + peer);
            if (State.DISCONNECTED == peer.getState()) {
                disconnectFromPeer(peer);
                return;
            }

            servers.put(peer.getId(), peer);
            sendHandshakePacket(peer);

            if (peer.isRouter()) {
                createProxyForAllClients(peer);
            } else {
                updateProxyRemotePeer(peer);
            }
        } catch (Exception e) {
            e.printStackTrace();//todo
        }
    }

    private void updateProxyRemotePeer(Peer peer) {
        ClientProxy proxy = proxies.get(peer.getRemotePeerId());
        if (proxy != null) {
            proxy.setRemotePeer(peer);
        }
    }

    private void disconnectFromPeer(Peer peer) {
        servers.remove(peer.getId());
        Iterator<Map.Entry<String, ClientProxy>> proxyIterator = proxies.entrySet().iterator();
        while (proxyIterator.hasNext()) {
            ClientProxy proxy = proxyIterator.next().getValue();
            if (proxy.getRemotePeer().getPeerManagerId().equals(peer.getPeerManagerId())) {
                proxies.remove(proxy.getId());
                proxy.close();

                for (Map.Entry<String, Set<String>> clientProxies : clientProxyIds.entrySet()) {
                    clientProxies.getValue().remove(proxy.getId());
                }
            }
        }
    }

    private void onRouterPacketReceived(DatagramPacket packet) {
        String key = Host.toId(packet.getAddress().getHostAddress(), packet.getPort(), Protocol.UDP); //todo find more fast solution
        Set<String> proxyIds = this.clientProxyIds.get(key);
        if (proxyIds != null) {
            sendToAllRemoteServers(packet, proxyIds);
        } else {
            createProxiesForAllRemoteServers(packet, key);
        }
    }

    private void sendToAllRemoteServers(DatagramPacket packet, Set<String> proxyIds) {
        for (String proxyId : proxyIds) {
            ClientProxy proxy = this.proxies.get(proxyId);
            proxy.sendWithRetry(packet);
        }
    }

    private void createProxyForAllClients(Peer peer) {
        for (Map.Entry<String, Set<String>> entry : clientProxyIds.entrySet()) {
            ClientProxy clientProxy = createProxy(entry.getKey(), peer);
            clientProxy.sendHandshakePacket(peer);
        }
    }

    private void createProxiesForAllRemoteServers(DatagramPacket packet, String key) {
        this.clientProxyIds.put(key, new CopyOnWriteArraySet<>());
        for (Peer remoteServer : this.servers.values()) {
            if (!remoteServer.isRouter()) {
                continue;
            }
            ClientProxy clientProxy = createProxy(key, remoteServer);
            clientProxy.sendWithRetry(packet);
        }
    }

    private void sendCreateProxyRequest(Host host, Peer proxyInfo) {
        byte[] data = proxyInfo.toString().getBytes();
        DatagramPacket createProxyRequest = new DatagramPacket(data, data.length);
        portPair.getP2pPort().send(createProxyRequest, host);
        Utils.sendWithRetry(portPair.getP2pPort(), createProxyRequest, proxyInfo, 30, 100L, getPeerSupplier());
    }

    private void onP2pPacketReceived(DatagramPacket packet) {
        Utils.logPacket(packet, false);
    }

    private ClientProxy createProxy(String clientHostPort, Peer remoteServer) {
        Utils.log("Creating proxy for peer: " + remoteServer);
        ClientProxy clientProxy = createClientProxy(clientHostPort, remoteServer);

        this.proxies.put(clientProxy.getInfo().getId(), clientProxy);
        this.peerManager.notifyPeerState(clientProxy, State.CONNECTED);
        this.clientProxyIds.get(clientHostPort).add(clientProxy.getInfo().getId());

        sendCreateProxyRequest(remoteServer.getHost(), clientProxy.getRemotePeer());
        return clientProxy;
    }

    private ClientProxy createClientProxy(String clientHostPort, Peer remoteServer) {
        PeerPair peers = peerFactory.createClientPeerPair(remoteServer);
        PortPair portPair = portFactory.pair();
        ClientProxy clientProxy = new ClientProxy(portPair, peers, servers::get);
        clientProxy.getInfo().setHost(Host.ofId(clientHostPort));
        return clientProxy;
    }

    @Override
    public Peer getInfo() {
        return peer;
    }

    @Override
    public UdpPort getPort() {
        return portPair.getP2pPort();
    }

    @Override
    public void setRemotePeer(Peer peer) {
        //ignore
    }

    @Override
    public Function<String, Peer> getPeerSupplier() {
        return servers::get;
    }

    @Override
    public void close() {
        for (Map.Entry<String, ClientProxy> proxy : proxies.entrySet()) {
            ClientProxy clientProxy = proxy.getValue();
            peerManager.notifyPeerState(clientProxy, State.DISCONNECTED);
            clientProxy.close();
        }
        peerManager.notifyPeerState(this, State.DISCONNECTED);
        portPair.getP2pPort().close();
        portPair.getTargetPort().close();
    }
}
