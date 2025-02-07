package net.relserver.core.api;

import net.relserver.core.api.model.AppStat;
import net.relserver.core.app.App;

import java.util.List;
import java.util.Map;

public interface AppCatalog {

    App getApp(String appId, Integer port);
    List<App> getApps();

    List<String> getAppNamesWithStats();

    Map<String, AppStat> getAppStats();
}
