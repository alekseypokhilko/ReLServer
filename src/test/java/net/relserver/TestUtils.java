package net.relserver;

import net.relserver.core.Constants;
import net.relserver.core.port.UdpPort;
import net.relserver.core.peer.Host;
import net.relserver.core.peer.Protocol;
import net.relserver.core.Settings;

import java.net.DatagramPacket;
import java.nio.charset.StandardCharsets;

public class TestUtils {

    public static void createServer(int realServerPort, String tag) {
        UdpPort realServer = new UdpPort(realServerPort, new Settings(null));
        realServer.setOnPacketReceived(packet -> {
            String receivedData = new String(packet.getData(), StandardCharsets.UTF_8).trim();
            if (receivedData.startsWith(Constants.HANDSHAKE_MESSAGE_PREFIX)) {
                return;
            }
            System.out.println(tag + " received: " + packet.getAddress().getHostAddress() + ":" + packet.getPort() + " " + receivedData);
            realServer.send(packet, new Host(packet.getAddress(), packet.getPort(), Protocol.UDP));
        });
        System.out.println("Test server '" + tag + "' started: " + realServer.getId());
    }

    public static void createClient(String tag, int port) {
        Host fakeServerHost = new Host("127.0.0.1", port, Protocol.UDP);
        UdpPort realClient = new UdpPort(null, new Settings(null));
        realClient.setOnPacketReceived(packet -> {
            String receivedData = new String(packet.getData(), StandardCharsets.UTF_8).trim();
            System.out.println(tag + " received: " + packet.getAddress().getHostAddress() + ":" + packet.getPort() + " " + receivedData);
            System.out.println(tag + "===== TEST ==== " + tag.equals(receivedData));
        });
        System.out.println("Test client '" + tag + "' started: " + realClient.getId());
        new Thread(() -> {
            while (true) {
                try {
                    Thread.sleep(5000L);
                    byte[] buf = tag.getBytes(StandardCharsets.UTF_8);
                    DatagramPacket packet = new DatagramPacket(buf, buf.length);
                    System.out.println(tag + " sent: " + new String(buf, StandardCharsets.UTF_8));
                    realClient.send(packet, fakeServerHost);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }
}
