package net.relserver.core.api;

import net.relserver.core.app.App;

import java.util.List;

public interface AppCatalog {

    App getApp(String appId, Integer port);
    List<App> getApps();
}
