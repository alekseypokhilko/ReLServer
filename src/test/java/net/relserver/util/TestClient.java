package net.relserver.util;

import net.relserver.core.Settings;
import net.relserver.core.peer.Host;
import net.relserver.core.peer.Protocol;
import net.relserver.core.port.UdpPort;

import java.net.DatagramPacket;
import java.nio.charset.StandardCharsets;

public class TestClient {

    String tag;
    Host fakeServerHost;
    public int success;
    public int failed;
    Thread sender;
    UdpPort udpPort;

    public TestClient(String tag, int port, int count) {
        this.tag = tag;
        fakeServerHost = new Host("127.0.0.1", port, Protocol.UDP);
        this.udpPort = new UdpPort(null, new Settings(null));
        udpPort.setOnPacketReceived(packet -> {
            String receivedData = new String(packet.getData(), StandardCharsets.UTF_8).trim();
//            System.out.println(tag + " received: " + packet.getAddress().getHostAddress() + ":" + packet.getPort() + " " + receivedData);
            if (tag.equals(receivedData)) {
                success++;
            } else {
                failed++;
            }

        });
        System.out.println("Test client '" + tag + "' started: " + udpPort.getId());
        sender = new Thread(() -> {
            for (int i = 0; i < count; i++) {
                try {
                    Thread.sleep(5000L);
                    send();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
        sender.start();
    }

    public void send() {
        byte[] buf = this.tag.getBytes(StandardCharsets.UTF_8);
        DatagramPacket packet = new DatagramPacket(buf, buf.length);
        System.out.println(this.tag + " sent: " + new String(buf, StandardCharsets.UTF_8));
        udpPort.send(packet, this.fakeServerHost);
    }

    public void stop() {
        udpPort.close();
        sender.interrupt();
    }
}
