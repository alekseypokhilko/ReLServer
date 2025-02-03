package net.relserver;

import net.relserver.core.*;
import net.relserver.core.app.App;
import net.relserver.core.api.AppCatalog;
import net.relserver.core.peer.*;
import net.relserver.core.port.PortPair;
import net.relserver.core.proxy.*;
import net.relserver.hub.HubServer;
import net.relserver.core.port.PortFactory;
import net.relserver.core.util.Logger;

import java.io.IOException;

//todo exception handling
//todo fix thread busy waiting
public class ReLServer {
    private App app;
    private PeerRegistry peerRegistry;
    private ProxyRegistry proxyRegistry;
    private PeerManager peerManager;
    private PeerFactory peerFactory;
    private PortFactory portFactory;
    private ProxyFactory proxyFactory;


    private HubServer hub;
    private ClientRouter client;
    private ServerRouter server;

    public ReLServer(Settings settings, AppCatalog appCatalog) {
        Logger.init(settings);
        Logger.log("Starting ReLServer with settings: %s", settings);
        Mode mode = settings.getMode();
        if (Mode.HUB == mode) {
            hub = new HubServer(settings);
            return;
        }

        this.app = appCatalog.getApp(settings.getString(Settings.appId), settings.getInt(Settings.appPort));
        Logger.log("Selected app: %s", app);

        peerRegistry = new PeerRegistry(app);
        peerManager = new PeerManager(settings, app, peerRegistry);
        proxyRegistry = new ProxyRegistry(peerManager);
        peerFactory = new PeerFactory(app, peerManager);
        portFactory = new PortFactory(app, settings);
        proxyFactory = new ProxyFactory(portFactory, peerFactory, settings.getLocalServerIp(), peerRegistry, proxyRegistry);

        peerRegistry.subscribeOnRemotePeerChanged(proxyRegistry::updateProxyRemotePeer);

        if (Mode.CLIENT_SERVER == mode) {
            createClient();
            createServer();
        } else if (Mode.CLIENT == mode) {
            createClient();
        } else if (Mode.SERVER == mode) {
            createServer();
        }

        peerManager.start();
    }

    private void createClient() {
        PortPair portPair = portFactory.clientRouterPair();
        PeerPair peerPair = peerFactory.clientRouterPeer();
        client = new ClientRouter(proxyFactory, portPair, peerPair, peerRegistry, proxyRegistry);
        peerRegistry.subscribeOnRemotePeerChanged(client::onPeerChanged);
        peerManager.notifyPeerState(client, State.CONNECTED);
    }

    private void createServer() {
        PortPair portPair = portFactory.serverRouterPair();
        PeerPair peerPair = peerFactory.serverRouterPeer();
        server = new ServerRouter(portPair, peerPair, proxyFactory, peerRegistry, proxyRegistry);
        peerRegistry.subscribeOnRemotePeerChanged(server::onPeerChanged);
        peerManager.notifyPeerState(server, State.CONNECTED);
    }

    //todo proper order
    public void stop() {
        if (hub != null) {
            try {
                hub.stop();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        if (client != null) {
            client.stop();
        }
        if (server != null) {
            server.stop();
        }
        if (peerManager != null) {
            peerManager.stop();
        }
        peerRegistry.stop();
        proxyRegistry.stop();
    }

    public HubServer getHub() {
        return hub;
    }

    public ClientRouter getClient() {
        return client;
    }

    public ServerRouter getServer() {
        return server;
    }
}
