package net.relserver.core.proxy;

import net.relserver.core.peer.*;
import net.relserver.core.port.UdpPort;

import java.net.DatagramPacket;

public class ServerRouter extends AbstractProxy {
    private final ProxyFactory proxyFactory;
    private final ProxyRegistry proxyRegistry;

    public ServerRouter(UdpPort port, PeerPair peerPair, ProxyFactory proxyFactory, PeerRegistry peerRegistry, ProxyRegistry proxyRegistry) {
        super(port, peerPair, peerRegistry);
        this.proxyFactory = proxyFactory;
        this.proxyRegistry = proxyRegistry;
    }

    public void onPacketReceived(DatagramPacket packet) {
        if (isHandshake(packet.getData())) {
            return; //todo change
        }
    }

    public void onPeerChanged(Peer peer) {
        if (State.CONNECTED == peer.getState()
                && Mode.CLIENT == peer.getMode()
                && peerPair.getPeer().getAppId().equals(peer.getAppId())
                && !peer.isRouter()
                && peerPair.getPeer().getPeerManagerId().equals(peer.getRemotePeerManagerId())
                && proxyRegistry.get(peer.getRemotePeerId()) == null) {
            proxyFactory.createServerProxy(peer);
        }
    }
}