package net.relserver;

import net.relserver.core.*;
import net.relserver.core.app.App;
import net.relserver.core.api.AppCatalog;
import net.relserver.core.client.ClientRouter;
import net.relserver.core.hub.HubServer;
import net.relserver.core.peer.Mode;
import net.relserver.core.peer.PeerFactory;
import net.relserver.core.peer.PeerManager;
import net.relserver.core.port.PortFactory;
import net.relserver.core.server.ServerRouter;
import net.relserver.core.util.Logger;

import java.io.IOException;

//todo remove disconnected peers
//todo synchronize collection access
//todo exception handling
//todo fix thread busy waiting
public class ReLServer {

    private HubServer hub;
    private ClientRouter client;
    private ServerRouter server;
    private PeerManager peerManager;

    public ReLServer(Settings settings, AppCatalog appCatalog) {
        Logger.init(settings);
        Logger.log("Starting ReLServer with settings: %s", settings);
        Mode mode = settings.getMode();
        if (Mode.HUB == mode) {
            hub = new HubServer(settings);
            return;
        }

        App app = appCatalog.getApp(settings.getString(Settings.appId), settings.getInt(Settings.appPort));
        Logger.log("Selected app: %s", app);
        peerManager = new PeerManager(settings, app);
        PeerFactory peerFactory = new PeerFactory(app, peerManager);
        PortFactory portFactory = new PortFactory(app, settings);

        if (Mode.CLIENT_SERVER == mode) {
            client = new ClientRouter(portFactory, peerFactory, peerManager);
            server = new ServerRouter(portFactory, peerFactory, settings.getLocalServerIp(), peerManager);
        } else if (Mode.CLIENT == mode) {
            client = new ClientRouter(portFactory, peerFactory, peerManager);
        } else if (Mode.SERVER == mode) {
            server = new ServerRouter(portFactory, peerFactory, settings.getLocalServerIp(), peerManager);
        }
    }

    public void stop() throws IOException {
        if (hub != null) {
            hub.close();
        }
        if (client != null) {
            client.close();
        }
        if (server != null) {
            server.close();
        }
        if (peerManager != null) {
            peerManager.close();
        }
    }
}
