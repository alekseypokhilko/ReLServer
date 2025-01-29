package net.relserver.core.app;

import java.util.List;

public class DefaultAppCatalog implements AppCatalog {
    private List<App> apps;

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

    public List<App> getApps() {
        loadApps();
        return apps;
    }

    private void loadApps() {
        if (this.apps != null) return;
        this.apps = AppLoader.loadFromRemoteRepository();
        if (this.apps != null) return;
        this.apps = AppLoader.loadFromResourcesFolder();
    }
}
