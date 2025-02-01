package net.relserver.util;

import net.relserver.core.Constants;
import net.relserver.core.Settings;
import net.relserver.core.peer.Host;
import net.relserver.core.peer.Protocol;
import net.relserver.core.port.UdpPort;

import java.nio.charset.StandardCharsets;

public class TestServer {
    UdpPort udpPort;

    public TestServer(int port, String tag) {
        udpPort = new UdpPort(port, new Settings(null));
        udpPort.setOnPacketReceived(packet -> {
            String receivedData = new String(packet.getData(), StandardCharsets.UTF_8).trim();
            if (receivedData.startsWith(Constants.HANDSHAKE_MESSAGE_PREFIX)) {
                return;
            }
//            System.out.println(tag + " received: " + packet.getAddress().getHostAddress() + ":" + packet.getPort() + " " + receivedData);
            udpPort.send(packet, new Host(packet.getAddress(), packet.getPort(), Protocol.UDP));
        });
        System.out.println("Test server '" + tag + "' started: " + udpPort.getId());
    }

    public void stop() {
        udpPort.close();
    }
}
