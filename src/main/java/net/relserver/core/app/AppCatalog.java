package net.relserver.core.app;

import java.util.List;

public interface AppCatalog {

    App getApp(String appId, Integer port);
    List<App> getApps();
}
