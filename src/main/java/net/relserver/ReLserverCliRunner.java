package net.relserver;

import net.relserver.core.app.AppCatalog;
import net.relserver.core.Settings;
import net.relserver.core.app.DefaultAppCatalog;

public class ReLserverCliRunner {
    public static void main(String[] args) {
        Settings settings = new Settings(args);
        AppCatalog appCatalog = new DefaultAppCatalog();

        new ReLserver(settings, appCatalog);
    }
}
