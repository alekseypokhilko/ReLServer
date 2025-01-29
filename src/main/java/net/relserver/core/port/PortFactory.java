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

    public PortPair clientRouterPair(Consumer<DatagramPacket> onP2pPacketReceived, Consumer<DatagramPacket> onTargetPacketReceived) {
        UdpPort p2pPort = new UdpPort(null, settings);
        p2pPort.setOnPacketReceived(onP2pPacketReceived);
        UdpPort targetPort = new UdpPort(app.getPort(), settings);
        targetPort.setOnPacketReceived(onTargetPacketReceived);
        return new PortPair(p2pPort, targetPort);
    }

    public UdpPort udpPort(Consumer<DatagramPacket> onPacketReceived) {
        UdpPort port = new UdpPort(null, settings);
        port.setOnPacketReceived(onPacketReceived);
        return port;
    }
}
