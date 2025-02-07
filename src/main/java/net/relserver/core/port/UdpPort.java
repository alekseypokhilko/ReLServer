package net.relserver.core.port;

import net.relserver.core.Constants;
import net.relserver.core.api.Id;
import net.relserver.core.api.Port;
import net.relserver.core.util.Logger;
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
    private final Thread receiverThread;

    public UdpPort(Integer port, Settings settings) {
        this.settings = settings;
        try {
            udpSocket = port == null ? new DatagramSocket() : new DatagramSocket(port, InetAddress.getByName("0.0.0.0"));
            udpSocket.setSoTimeout(settings.getInt(Settings.socketTimeout));
        } catch (IOException ex) {
            throw new RuntimeException("Error creating socket: " + ex.getMessage(), ex);
        }
        String prefix = Constants.PORT_PREFIX + udpSocket.getLocalPort();
        this.id = Id.generateId(prefix);
        receiverThread = new Thread(this::receiverLoop, this.id + "-thread");
        receiverThread.start();
    }

    public void setOnPacketReceived(Consumer<DatagramPacket> onPacketReceived) {
        this.onPacketReceived = onPacketReceived;
    }

    private void receiverLoop() {
        Integer bufSize = settings.getInt(Settings.packetBufferSize);
        while (!Thread.interrupted()) {
            try {
                byte[] buf2 = new byte[bufSize];  //todo check GC stats
                DatagramPacket packet = new DatagramPacket(buf2, buf2.length);
                if (udpSocket.isClosed()) {
                    Logger.log("Socket %s closed", this.id);
                    return;
                }
                udpSocket.receive(packet);
                Logger.logPacket(id, packet, false);
                if (this.onPacketReceived != null) {
                    this.onPacketReceived.accept(packet);
                }
            } catch (SocketTimeoutException ignore) {
            } catch (SocketException ignore) {
                Logger.log("Socket %s closed", this.id);
                return;
            } catch (Exception e) {
                e.printStackTrace();//todo
            }
        }
    }

    public void send(DatagramPacket packet, Host host) {
        if (host == null) {
            throw new IllegalArgumentException("Host is null for data: '" + new String(packet.getData()).trim() + "'");
        }
        try {
            DatagramPacket p = new DatagramPacket(packet.getData(), packet.getLength(), InetAddress.getByName(host.getIp()), host.getPort());
            Logger.logPacket(id, p, true);
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
            receiverThread.interrupt();
            udpSocket.close();
        } catch (Exception e) {
            Logger.log("Exception while closing port: %s", e.getMessage());
        }
    }
}
