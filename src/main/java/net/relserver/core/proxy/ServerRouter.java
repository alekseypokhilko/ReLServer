package net.relserver.core.proxy;

import com.google.gson.JsonSyntaxException;
import net.relserver.core.port.PortPair;
import net.relserver.core.util.Logger;
import net.relserver.core.peer.*;
import net.relserver.core.util.Utils;

import java.net.DatagramPacket;
import java.nio.charset.StandardCharsets;

public class ServerRouter extends AbstractProxy {
    private final ProxyFactory proxyFactory;
    private final ProxyRegistry proxyRegistry;

    public ServerRouter(PortPair portPair, PeerPair peerPair, ProxyFactory proxyFactory, PeerRegistry peerRegistry, ProxyRegistry proxyRegistry) {
        super(portPair, peerPair, peerRegistry);
        this.proxyFactory = proxyFactory;
        this.proxyRegistry = proxyRegistry;
    }

    @Override
    protected void attachToPorts() {
        this.portPair.getP2pPort().setOnPacketReceived(this::processRequest);
    }

    public void processRequest(DatagramPacket packet) {
        if (Utils.isHandshake(packet.getData())) {
            return;
        }

        String peerInfo = new String(packet.getData(), StandardCharsets.UTF_8).trim();
        Logger.log("Router %s received create proxy request: '%s'", peerPair.getPeer().getId(), peerInfo);
        try {
            if (peerInfo.isEmpty()) {
                return;
            }
            Peer peerRequest = Utils.fromJson(peerInfo, Peer.class);
            if (Mode.SERVER != peerRequest.getMode() && this.proxyRegistry.get(peerRequest.getId()) != null) {
                return;
            }

            proxyFactory.createServerProxy(peerRequest);
        } catch (ArrayIndexOutOfBoundsException | IllegalArgumentException | JsonSyntaxException e) {
            Logger.log("Illegal peer request: '%s' %s", peerInfo, e.getMessage());
        } catch (Exception e) {
            Logger.log("Exception while processing peer request: '%s' %s", peerInfo, e.getMessage());
        }
    }

    protected void processResponse(DatagramPacket packet) {
        //noop
    }

    public void onPeerChanged(Peer peer) {
        if (State.CONNECTED == peer.getState()
                && peer.isRouter()
                && Mode.CLIENT == peer.getMode()) {
            sendHandshakePacket(peer);
        }

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