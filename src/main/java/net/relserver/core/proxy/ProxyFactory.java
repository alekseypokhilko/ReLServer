package net.relserver.core.proxy;

import net.relserver.core.peer.*;
import net.relserver.core.port.PortFactory;
import net.relserver.core.port.PortPair;

public class ProxyFactory {
    private final PortFactory portFactory;
    private final PeerFactory peerFactory;
    private final PeerRegistry peerRegistry;
    private final ProxyRegistry proxyRegistry;
    private final String localServerIp;

    public ProxyFactory(PortFactory portFactory, PeerFactory peerFactory, String localServerIp,
                        PeerRegistry peerRegistry, ProxyRegistry proxyRegistry) {
        this.localServerIp = localServerIp;
        this.portFactory = portFactory;
        this.peerFactory = peerFactory;
        this.peerRegistry = peerRegistry;
        this.proxyRegistry = proxyRegistry;
    }

    public ClientProxy createClientProxy(String clientHostPort, Peer remoteServer) {
        PeerPair peers = peerFactory.createClientPeerPair(remoteServer);
        PortPair portPair = portFactory.pair();
        ClientProxy clientProxy = new ClientProxy(portPair, peers, peerRegistry);
        clientProxy.getPeerPair().getPeer().setHost(Host.ofId(clientHostPort)); //todo move to peer factory
        proxyRegistry.add(clientProxy);
        return clientProxy;
    }

    public ServerProxy createServerProxy(Peer peerRequest) {
        PeerPair peers = peerFactory.serverPeerPair(localServerIp, peerRequest);
        PortPair portPair = portFactory.pair();
        ServerProxy serverProxy = new ServerProxy(portPair, peers, peerRegistry);
        proxyRegistry.add(serverProxy);
        return serverProxy;
    }
}