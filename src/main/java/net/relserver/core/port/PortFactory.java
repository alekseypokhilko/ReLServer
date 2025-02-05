package net.relserver.core.port;

import net.relserver.core.Settings;
import net.relserver.core.app.App;

public class PortFactory {
    private final App app;
    private final Settings settings;

    public PortFactory(App app, Settings settings) {
        this.app = app;
        this.settings = settings;
    }

    public UdpPort randomPort() {
        return new UdpPort(null, settings);
    }

    public UdpPort appPort() {
        return new UdpPort(app.getPort(), settings);
    }
}
