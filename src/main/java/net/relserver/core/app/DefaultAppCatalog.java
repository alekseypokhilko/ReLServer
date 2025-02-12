package net.relserver.core.app;

import com.google.gson.reflect.TypeToken;
import net.relserver.core.Settings;
import net.relserver.core.api.AppCatalog;
import net.relserver.core.api.model.AppStat;
import net.relserver.core.api.model.Operation;
import net.relserver.core.api.model.Request;
import net.relserver.core.http.SocketRequest;
import net.relserver.core.peer.HubLoader;
import net.relserver.core.util.Utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DefaultAppCatalog implements AppCatalog {
    private static final TypeToken<Map<String, AppStat>> APP_STATS_TYPE = new TypeToken<>() {
    };
    private final Settings settings;
    private List<App> apps;

    public DefaultAppCatalog(Settings settings) {
        this.settings = settings;
    }

    @Override
    public App getApp(String appId, Integer port) {
        loadApps();
        if (port != null) {
            String customAppId = appId == null ? "custom" : String.format("%s:%s", appId, port);
            String customAppTitle = String.format("APP %s", customAppId);
            App app = new App(customAppId, port, customAppTitle);
            apps.add(app);
            return app;
        } else {
            for (App app : apps) {
                if (app.getId().equals(appId)) {
                    return app;
                }
            }
            throw new IllegalArgumentException("Illegal 'appId' or 'appPort'");
        }
    }

    @Override
    public Map<String, AppStat> getAppStats() {
        String response = SocketRequest.execute(
                HubLoader.getHubIps(settings).get(0), //todo remove static
                settings.getInt(Settings.hubServicePort),
                new Request(Operation.GET_APP_STATS, null)
        );
        Map<String, AppStat> stats = Utils.fromJson(response, APP_STATS_TYPE);
        return stats == null ? new HashMap<>() : stats;
    }

    @Override
    public List<String> getAppNamesWithStats() {
        loadApps();
        Map<String, AppStat> appStats = getAppStats();
        List<String> appNames = new ArrayList<>();
        for (App app : apps) {
            AppStat stat = appStats.get(app.getId());
            appNames.add(stat == null
                    ? formatName(0, 0, app.getTitle())
                    : formatName(stat.getServers(), stat.getClients(), app.getTitle()));
        }
        return appNames;
    }

    public List<App> getApps() {
        loadApps();
        return apps;
    }

    private static String formatName(int serverCount, int clientCount, String title) {
        return String.format("S:%s C:%s | %s", serverCount, clientCount, title);
    }

    private void loadApps() {
        if (this.apps != null) return;
        this.apps = AppLoader.loadFromRemoteRepository();
        if (!this.apps.isEmpty()) return;
        this.apps = AppLoader.loadFromResourcesFolder();
    }
}
