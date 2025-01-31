package net.relserver.core.port;

import net.relserver.core.Settings;
import net.relserver.core.app.App;

import java.net.DatagramPacket;
import java.util.function.Consumer;

public class PortFactory {
    private final App app;
    private final Settings settings;
    public PortFactory(App app, Settings settings) {
        this.app = app;
        this.settings = settings;
    }

    public PortPair pair() {
        return new PortPair(new UdpPort(null, settings), new UdpPort(null, settings));
    }

    public PortPair clientRouterPair() {
        UdpPort p2pPort = udpPort(null, null);
        UdpPort targetPort = udpPort(app.getPort(), null);
        return new PortPair(p2pPort, targetPort);
    }

    public PortPair serverRouterPair() {
        UdpPort p2pPort = udpPort(null, null);
        return new PortPair(p2pPort, null);
    }

    public UdpPort udpPort(Integer port, Consumer<DatagramPacket> onPacketReceived) {
        UdpPort udpPort = new UdpPort(port, settings);
        udpPort.setOnPacketReceived(onPacketReceived);
        return udpPort;
    }
}
