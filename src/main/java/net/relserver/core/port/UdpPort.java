package net.relserver.core.port;

import net.relserver.core.Constants;
import net.relserver.core.Utils;
import net.relserver.core.peer.Host;
import net.relserver.core.Settings;

import java.io.IOException;
import java.net.*;
import java.util.function.Consumer;

public class UdpPort implements Port<DatagramPacket> {
    private final String id;
    private final Settings settings;
    private final DatagramSocket udpSocket;
    private Consumer<DatagramPacket> onPacketReceived;

    public UdpPort(Integer port, Settings settings) {
        this.settings = settings;
        try {
            udpSocket = port == null ? new DatagramSocket() : new DatagramSocket(port, InetAddress.getByName("0.0.0.0"));
            udpSocket.setSoTimeout(settings.getInt(Settings.socketTimeout));
        } catch (IOException ex) {
            throw new RuntimeException("Error creating socket: " + ex.getMessage(), ex);
        }
        this.id = Utils.generateId(Constants.PORT_PREFIX + udpSocket.getLocalPort());
        runReceiverThread();
    }

    public void setOnPacketReceived(Consumer<DatagramPacket> onPacketReceived) {
        this.onPacketReceived = onPacketReceived;
    }

    protected void runReceiverThread() {
        new Thread(() -> {
            Integer bufSize = settings.getInt(Settings.packetBufferSize);
            String log = settings.getString(Settings.log);
            while (true) {
                try {
                    byte[] buf2 = new byte[bufSize];  //todo check GC stats
                    DatagramPacket packet = new DatagramPacket(buf2, buf2.length);
                    if (udpSocket.isClosed()) {
                        Utils.log("Socket " + this.id + " closed");
                        return;
                    }
                    udpSocket.receive(packet);
                    if (log != null) {
                        Utils.logPacket(packet, false);
                    }
                    if (this.onPacketReceived != null) {
                        this.onPacketReceived.accept(packet);
                    }
                } catch (SocketTimeoutException ignore) {
                } catch (SocketException ignore) {
                    Utils.log("Socket " + this.id + " closed");
                    return;
                } catch (Exception e) {
                    e.printStackTrace();//todo
                }
            }
        }, this.id + "-thread").start();
    }

    public void send(DatagramPacket packet, Host host) {
        if (host == null) {
            throw new IllegalArgumentException("Host is null for data: '" + new String(packet.getData()).trim() + "'");
        }
        try {
            DatagramPacket p = new DatagramPacket(packet.getData(), packet.getLength(), InetAddress.getByName(host.getIp()), host.getPort());
            if (settings.getString(Settings.log) != null) {
                Utils.logPacket(p, true);
            }
            udpSocket.send(p);
        } catch (SocketTimeoutException | SocketException ignore) {
            //ignore
        } catch (Exception e) {
            //ignore //todo trace mode on cli args
            throw new RuntimeException(e);
        }
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public void close() {
        try {
            udpSocket.close();
        } catch (Exception e) {
            Utils.log("Exception while closing port: " + e.getMessage());
        }
    }
}
