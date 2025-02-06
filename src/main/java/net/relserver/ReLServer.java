package net.relserver;

import net.relserver.core.*;
import net.relserver.core.app.App;
import net.relserver.core.api.AppCatalog;
import net.relserver.core.app.DefaultAppCatalog;
import net.relserver.core.peer.*;
import net.relserver.core.port.UdpPort;
import net.relserver.core.proxy.*;
import net.relserver.hub.Hub;
import net.relserver.core.port.PortFactory;
import net.relserver.core.util.Logger;

import java.io.IOException;

//todo exception handling
//todo fix thread busy waiting
public class ReLServer {
    private AppCatalog appCatalog;
    private PeerRegistry peerRegistry;
    private ProxyRegistry proxyRegistry;
    private PeerManager peerManager;
    private PeerFactory peerFactory;
    private PortFactory portFactory;
    private ProxyFactory proxyFactory;


    private Hub hub;
    private ClientRouter client;
    private ServerRouter server;

    public ReLServer(Settings settings) {
        Logger.init(settings);
        Logger.log("Starting ReLServer with settings: %s", settings);
        Mode mode = settings.getMode();
        if (Mode.HUB == mode) {
            hub = new Hub(settings);
            return;
        }

        appCatalog = new DefaultAppCatalog(settings);
        App app = appCatalog.getApp(settings.getString(Settings.appId), settings.getInt(Settings.appPort));
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
        UdpPort port = portFactory.appPort();
        PeerPair peerPair = peerFactory.clientRouterPeer();
        client = new ClientRouter(proxyFactory, port, peerPair, peerRegistry, proxyRegistry);
        peerRegistry.subscribeOnRemotePeerChanged(client::onPeerChanged);
        peerManager.notifyPeerState(client.getPeer(), State.CONNECTED, client.getPort());
    }

    private void createServer() {
        UdpPort port = portFactory.randomPort();
        PeerPair peerPair = peerFactory.serverRouterPeer();
        server = new ServerRouter(port, peerPair, proxyFactory, peerRegistry, proxyRegistry);
        peerRegistry.subscribeOnRemotePeerChanged(server::onPeerChanged);
        peerManager.notifyPeerState(server.getPeer(), State.CONNECTED, server.getPort());
    }

    //todo proper order
    public void stop() {
        if (hub != null) {
            hub.stop();
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

    public Hub getHub() {
        return hub;
    }

    public ClientRouter getClient() {
        return client;
    }

    public ServerRouter getServer() {
        return server;
    }

    public AppCatalog getAppCatalog() {
        return appCatalog;
    }
}
