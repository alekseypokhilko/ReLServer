package net.relserver.hub.handler;

import net.relserver.core.api.model.AppStat;
import net.relserver.core.api.model.Operation;
import net.relserver.core.peer.Mode;
import net.relserver.core.peer.Peer;
import net.relserver.core.util.Utils;
import net.relserver.hub.Client;
import net.relserver.hub.PeerRegistry;

import java.util.HashMap;
import java.util.Map;

public class AppStatsHandler implements RequestHandler {
    private final PeerRegistry peerRegistry;

    public AppStatsHandler(PeerRegistry peerRegistry) {
        this.peerRegistry = peerRegistry;
    }

    @Override
    public void handle(Client client, String message) {
        Map<String, AppStat> stats = getAppStats();
        client.sendMessage(Utils.toJson(stats));
        client.stop();
    }

    public Map<String, AppStat> getAppStats() {
        Map<String, AppStat> appStats = new HashMap<>();
        for (Peer peer : peerRegistry.getAll()) {
            String appId = peer.getAppId();
            AppStat stats = appStats.get(appId);
            if (stats == null) {
                stats = new AppStat(appId, 0, 0);
                appStats.put(appId, stats);
            }
            if (peer.isRouter() && Mode.SERVER == peer.getMode()) {
                stats.incrementServers();
            }
            if (peer.isRouter() && Mode.CLIENT == peer.getMode()) {
                stats.incrementClients();
            }
        }
        return appStats;
    }

    @Override
    public Operation getOperation() {
        return Operation.GET_APP_STATS;
    }
}
