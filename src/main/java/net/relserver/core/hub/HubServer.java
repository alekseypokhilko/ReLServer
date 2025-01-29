package net.relserver.core.hub;

import net.relserver.core.Constants;
import net.relserver.core.Settings;
import net.relserver.core.Utils;
import net.relserver.core.peer.*;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class HubServer implements AutoCloseable {
    private final String id;
    private final Settings settings;
    private final DatagramSocket udpSocket;
    private final ServerSocket serverSocket;

    //peerManagerId <=> instance
    private final Map<String, AppInstance> instances = new ConcurrentHashMap<>();

    public HubServer(Settings settings) {
        this.id = Utils.generateId(Constants.HUB_PREFIX);
        this.settings = settings;
        int servicePort = settings.getInt(Settings.hubServicePort);
        int registrationPort = settings.getInt(Settings.hubRegistrationPort);

        try {
            serverSocket = new ServerSocket(servicePort);
            udpSocket = new DatagramSocket(registrationPort);
            udpSocket.setSoTimeout(settings.getInt(Settings.socketTimeout));
            Utils.log("Hub " + id + " started with ports, TCP: " + servicePort + " UDP: " + registrationPort);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        acceptNewPeerManagerConnections();
        runUdpPeerRegistrationThread();
    }

    void acceptNewPeerManagerConnections() {
        new Thread(() -> {
            while (true) {
                try {
                    Socket socket = serverSocket.accept();
                    registerPeerManager(socket);
                } catch (Exception se) {
                    Utils.log("Exception on acceptNewPeerManagerConnections thread");
                    se.printStackTrace();//todo
                }
            }
        }, "acceptConnections-" + id).start();

    }

    private void registerPeerManager(Socket socket) {
        new Thread(() -> {
            try {
                AppInstance instance = getPeerManagerInfo(socket);
                sendPeers(instance);
                this.instances.put(instance.getPeerManagerId(), instance);
                Utils.log("Peer manager " + instance.getPeerManagerId() + " connected: appId=" + instance.getAppId() + " host=" + instance.getHostId());
            } catch (Exception e) {
                e.printStackTrace(); //todo
            }
        }, "registerPeerManager-" + System.currentTimeMillis()).start();
    }

    private void sendPeers(AppInstance instance) {
        for (AppInstance pmi : instances.values()) {
            try {
                writeToSocket(instance, pmi.getClientRouter());
                writeToSocket(instance, pmi.getServerRouter());
            } catch (Exception e) {
                e.printStackTrace(); //todo
            }
        }
    }

    private AppInstance getPeerManagerInfo(Socket socket) throws Exception {
        while (true) {
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            String message = in.readLine(); //todo disconnect on timeout
            if (message != null && message.startsWith(Constants.PEER_MANAGER_PREFIX + "-")) {
                return new AppInstance(message, socket);
            }
        }
    }

    private void onPeerManagerDisconnects(AppInstance instance) {
        try {
            AppInstance removed = instances.remove(instance.getPeerManagerId());
            if (removed != null) {
                notifyPeerStateChanged(removed.getServerRouter(), State.DISCONNECTED);
                notifyPeerStateChanged(removed.getClientRouter(), State.DISCONNECTED);
                Utils.log("Peer manager " + removed.getPeerManagerId() + " disconnected: " + removed.getHostId());
                removed.getSocket().close();
            }
        } catch (Exception e) {
            Utils.log("Error while closing socket: " + e.getMessage());
        }
    }

    void notifyPeerStateChanged(Peer peer, State state) {
        if (peer == null) {
            return;
        }
        Utils.log("Peer state changed: " + state + " " + peer);
        peer.setState(state);

        if (peer.getRemotePeerManagerId() != null) {
            //notify concrete peer manager
            AppInstance appInstance = instances.get(peer.getRemotePeerManagerId());
            writeToSocket(appInstance, peer);
        } else {
            //notify all
            new Thread(() -> {
                for (Map.Entry<String, AppInstance> pm : instances.entrySet()) {
                    AppInstance instance = pm.getValue();
                    if (peer.getAppId().equals(instance.getAppId())) {
                        writeToSocket(instance, peer);
                    }
                }
            }, "notifyPeerState-" + System.currentTimeMillis()).start();
        }
    }

    private void writeToSocket(AppInstance appInstance, Peer peer) {
        if (peer == null || appInstance == null) {
            return;
        }
        try {
            BufferedOutputStream out = new BufferedOutputStream(appInstance.getSocket().getOutputStream());
            out.write(peer.toString().getBytes(StandardCharsets.UTF_8));
            out.write(Constants.NEW_LINE);
            out.flush();
        } catch (SocketException se) {
            Utils.log("Exception while writing to socket for peer manager " + appInstance.getPeerManagerId() + " " + appInstance.getHostId() + " " + se.getMessage());
            onPeerManagerDisconnects(appInstance);
        } catch (IOException ioe) {
            Utils.log("Exception while writing to socket for peer manager " + appInstance.getPeerManagerId() + " " + appInstance.getHostId() + " " + ioe.getMessage());
            ioe.printStackTrace();//todo
        }
    }

    private void runUdpPeerRegistrationThread() {
        new Thread(() -> {
            String log = settings.getString(Settings.log);
            Integer bufSize = settings.getInt(Settings.packetBufferSize);
            while (true) {
                byte[] buf = new byte[bufSize];
                DatagramPacket packet = new DatagramPacket(buf, buf.length);
                try {
                    udpSocket.receive(packet);
                    if (log != null) {
                        Utils.logPacket(packet, false);
                    }
                    Peer peer = getPeer(packet);

                    AppInstance appInstance = instances.get(peer.getPeerManagerId());
                    if (appInstance != null) {
                        appInstance.onPeerStateChanged(peer);
                    }

                    notifyPeerStateChanged(peer, peer.getState());
                } catch (SocketTimeoutException e) {
                    //ignore
                } catch (SocketException e) {
                    Utils.log("Udp peer registration socket closed");
                    e.printStackTrace();
                    return;
                } catch (Exception e) {
                    throw new RuntimeException(e); //todo
                }
            }
        }).start();
    }

    private Peer getPeer(DatagramPacket packet) {
        String message = new String(packet.getData(), StandardCharsets.UTF_8).trim();
        Utils.log("Hub " + this.id + " received peer: " + message);
        Host host = new Host(packet.getAddress().getHostAddress(), packet.getPort(), Protocol.UDP);
        return Peer.of(message, host);
    }

    @Override
    public void close() throws IOException {
        serverSocket.close();
        udpSocket.close();
    }
}