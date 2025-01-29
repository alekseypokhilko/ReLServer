package net.relserver;

import net.relserver.core.app.AppCatalog;
import net.relserver.core.Settings;
import net.relserver.core.app.DefaultAppCatalog;

public class ReLServerCliRunner {
    public static void main(String[] args) {
        Settings settings = new Settings(args);
        AppCatalog appCatalog = new DefaultAppCatalog();

        new ReLServer(settings, appCatalog);
    }
}
