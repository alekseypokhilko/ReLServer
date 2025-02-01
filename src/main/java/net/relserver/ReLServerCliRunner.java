package net.relserver;

import net.relserver.core.api.AppCatalog;
import net.relserver.core.Settings;
import net.relserver.core.app.DefaultAppCatalog;

public class ReLServerCliRunner {
    public static void main(String[] args) {
        of(args);
    }

    public static ReLServer of(String[] args) {
        Settings settings = new Settings(args);
        AppCatalog appCatalog = new DefaultAppCatalog();
        return new ReLServer(settings, appCatalog);
    }
}
