package net.relserver.hub;

import net.relserver.core.port.UdpPort;
import net.relserver.core.util.Logger;
import net.relserver.core.Settings;
import net.relserver.core.peer.*;
import net.relserver.hub.handler.AppStatsHandler;
import net.relserver.hub.handler.DisconnectPeerManagerHandler;
import net.relserver.hub.handler.RegisterPeerManagerHandler;

import java.util.HashMap;
import java.util.Map;

public class Hub {
    private final SocketServer socketServer;
    private final ClientRegistry clientRegistry;
    private final PeerRegistry peerRegistry;

    public Hub(Settings settings) {
        int servicePort = settings.getInt(Settings.hubServicePort);
        int registrationPort = settings.getInt(Settings.hubRegistrationPort);

        UdpPort port = new UdpPort(registrationPort, settings);
        this.clientRegistry = new ClientRegistry();
        this.peerRegistry = new PeerRegistry(port, clientRegistry);

        try {
            socketServer = new SocketServer(servicePort);
            socketServer.registerHandler(new RegisterPeerManagerHandler(clientRegistry, peerRegistry));
            socketServer.registerHandler(new DisconnectPeerManagerHandler(peerRegistry));
            socketServer.registerHandler(new AppStatsHandler(peerRegistry));
            socketServer.start();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        Logger.log("Hub started with ports, TCP: %d UDP: %d", servicePort, registrationPort);
    }

    public void stop() {
        peerRegistry.stop();
        clientRegistry.stop();
        socketServer.stop();
    }

    public Map<String, Object> getStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("size", peerRegistry.getAll().size());

        int clientRouters = 0;
        int serverRouters = 0;
        int clientProxies = 0;
        int serverProxies = 0;
        for (Peer peer : peerRegistry.getAll()) {
            if (peer.isRouter() && peer.getMode() == Mode.CLIENT) {
                clientRouters++;
            } else if (peer.isRouter() && peer.getMode() == Mode.SERVER) {
                serverRouters++;
            } else if (!peer.isRouter() && peer.getMode() == Mode.SERVER) {
                serverProxies++;
            } else if (!peer.isRouter() && peer.getMode() == Mode.CLIENT) {
                clientProxies++;
            }
        }
        stats.put("clientRouters", clientRouters);
        stats.put("clientProxies", clientProxies);
        stats.put("serverRouters", serverRouters);
        stats.put("serverProxies", serverProxies);
        return stats;
    }
}