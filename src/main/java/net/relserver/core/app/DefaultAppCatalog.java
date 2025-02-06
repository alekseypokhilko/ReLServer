package net.relserver.core.app;

import com.google.gson.reflect.TypeToken;
import net.relserver.core.Settings;
import net.relserver.core.api.AppCatalog;
import net.relserver.core.api.model.AppStat;
import net.relserver.core.api.model.Operation;
import net.relserver.core.api.model.Request;
import net.relserver.core.http.SocketRequest;
import net.relserver.core.util.Utils;

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
            return new App(appId == null ? "unknown" : appId, port, "Unknown App");
        } else {
            for (App app : apps) {
                if (app.getId().equals(appId)) {
                    return app;
                }
            }
            throw new IllegalArgumentException("Cannot create app. Provide correct parameters '-appId' and '-appPort'");
        }
    }

    public Map<String, AppStat> getAppStats() {
        String response = SocketRequest.execute(
                settings.getString(Settings.hubIp),
                settings.getInt(Settings.hubServicePort),
                new Request(Operation.GET_APP_STATS, null)
        );
        return Utils.fromJson(response, APP_STATS_TYPE);
    }

    public List<App> getApps() {
        loadApps();
        return apps;
    }

    private void loadApps() {
        if (this.apps != null) return;
        this.apps = AppLoader.loadFromRemoteRepository();
        if (!this.apps.isEmpty()) return;
        this.apps = AppLoader.loadFromResourcesFolder();
    }
}
