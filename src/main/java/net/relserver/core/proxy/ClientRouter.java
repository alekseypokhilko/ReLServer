package net.relserver.core.proxy;

import net.relserver.core.util.Logger;
import net.relserver.core.util.Utils;
import net.relserver.core.peer.*;
import net.relserver.core.port.PortPair;
import net.relserver.core.api.Proxy;

import java.net.DatagramPacket;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * Emulates a remote server port in a local network.
 * Accepts client connections and creates a required proxies for all remote servers.
 */
public class ClientRouter extends AbstractProxy {
    private final ProxyFactory proxyFactory;
    private final ProxyRegistry proxyRegistry;

    //host <-> proxyIds todo Map<Host, Set<String>>
    private final Map<String, Set<String>> clientProxyIds = new ConcurrentHashMap<>();

    public ClientRouter(ProxyFactory proxyFactory, PortPair portPair, PeerPair peerPair,
                        PeerRegistry peerRegistry, ProxyRegistry proxyRegistry) {
        super(portPair, peerPair, peerRegistry);
        this.proxyFactory = proxyFactory;
        this.proxyRegistry = proxyRegistry;
    }

    @Override
    protected void attachToPorts() {
        this.portPair.getP2pPort().setOnPacketReceived(this::processResponse);
        this.portPair.getTargetPort().setOnPacketReceived(this::processRequest);
    }

    public void onPeerChanged(Peer peer) {
        try {
            if (State.DISCONNECTED == peer.getState() || Mode.SERVER != peer.getMode()) {
                return;
            }
            sendHandshakePacket(peer);
            if (peer.isRouter() && Mode.SERVER == peer.getMode()) {
                createProxyForAllClients(peer);
            }
        } catch (Exception e) {
            e.printStackTrace();//todo
        }
    }

    public void processRequest(DatagramPacket packet) {
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
            Proxy proxy = this.proxyRegistry.get(proxyId);
            if (proxy != null) {
                proxy.processRequest(packet);
            }
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
        for (Peer peer : this.peerRegistry.getAll().values()) {
            if (peer.isRouter() && Mode.SERVER == peer.getMode()) {
                ClientProxy clientProxy = createProxy(key, peer);
                clientProxy.sendWithRetry(packet);
            }
        }
    }

    private void sendCreateProxyRequest(Peer remoteRouterPeer, Peer proxyInfo) {
        byte[] data = Utils.toJson(proxyInfo).getBytes();
        DatagramPacket createProxyRequest = new DatagramPacket(data, data.length);
        portPair.getP2pPort().send(createProxyRequest, remoteRouterPeer.getHost());
    }

    protected void processResponse(DatagramPacket packet) {
        Logger.logPacket(portPair.getP2pPort().getId(), packet, false);
    }

    public ClientProxy createProxy(String clientHostPort, Peer remoteServer) {
        Logger.log("Creating proxy for peer: %s", remoteServer);
        ClientProxy clientProxy = proxyFactory.createClientProxy(clientHostPort, remoteServer);

        this.clientProxyIds.get(clientHostPort).add(clientProxy.getPeerPair().getPeer().getId());

        sendCreateProxyRequest(remoteServer, clientProxy.getRemotePeer());
        return clientProxy;
    }
}
