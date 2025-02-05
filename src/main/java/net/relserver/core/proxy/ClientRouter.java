package net.relserver.core.proxy;

import net.relserver.core.port.UdpPort;
import net.relserver.core.util.Logger;
import net.relserver.core.peer.*;
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

    public ClientRouter(ProxyFactory proxyFactory, UdpPort port, PeerPair peerPair,
                        PeerRegistry peerRegistry, ProxyRegistry proxyRegistry) {
        super(port, peerPair, peerRegistry);
        this.proxyFactory = proxyFactory;
        this.proxyRegistry = proxyRegistry;
    }

    public void onPeerChanged(Peer peer) {
        if (State.DISCONNECTED != peer.getState() && peer.isRouter() && Mode.SERVER == peer.getMode()) {
            sendHandshakePacket(peer);
            createProxyForAllClients(peer);
        }
    }

    public void onPacketReceived(DatagramPacket packet) {
        if (isHandshake(packet.getData())) {
            return; //todo
        }
        //todo remove clientProxyIds. iterate proxyRegistry
        String clientHostPort = Host.toId(packet.getAddress().getHostAddress(), packet.getPort(), Protocol.UDP); //todo find more fast solution
        Set<String> proxyIds = this.clientProxyIds.get(clientHostPort);
        if (proxyIds == null || proxyIds.isEmpty()) {
            createProxiesForAllRemoteServers(packet, clientHostPort);
        } else {
            sendToAllRemoteServers(packet, proxyIds); //mutates this.clientProxyIds.value() todo remove on proxy disconnect
            if (proxyIds.isEmpty()) {
                createProxiesForAllRemoteServers(packet, clientHostPort);
            }
        }
    }

    private void sendToAllRemoteServers(DatagramPacket packet, Set<String> proxyIds) {
        for (String proxyId : proxyIds) {
            Proxy proxy = this.proxyRegistry.get(proxyId);
            if (proxy != null) {
                proxy.onPacketReceived(packet);
            } else {
                proxyIds.remove(proxyId); //mutates this.clientProxyIds.value() todo remove on proxy disconnect
            }
        }
    }

    private void createProxyForAllClients(Peer peer) {
        for (Map.Entry<String, Set<String>> entry : clientProxyIds.entrySet()) {
            ClientProxy clientProxy = createProxy(entry.getKey(), peer);
            clientProxy.sendHandshakePacket(peer);
        }
    }

    private void createProxiesForAllRemoteServers(DatagramPacket packet, String clientHostPort) {
        this.clientProxyIds.put(clientHostPort, new CopyOnWriteArraySet<>());
        for (Peer peer : this.peerRegistry.getAll().values()) {
            if (peer.isRouter() && Mode.SERVER == peer.getMode()) {
                ClientProxy clientProxy = createProxy(clientHostPort, peer);
                clientProxy.sendWithRetry(packet);
            }
        }
    }

    public ClientProxy createProxy(String clientHostPort, Peer remoteServer) {
        Logger.log("Creating proxy for peer: %s", remoteServer);
        ClientProxy clientProxy = proxyFactory.createClientProxy(clientHostPort, remoteServer);

        this.clientProxyIds.get(clientHostPort).add(clientProxy.getId());
        return clientProxy;
    }
}
