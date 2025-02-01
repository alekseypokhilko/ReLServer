package net.relserver.core.hub;

import net.relserver.core.Constants;
import net.relserver.core.api.Id;
import net.relserver.core.util.Logger;
import net.relserver.core.Settings;
import net.relserver.core.peer.*;
import net.relserver.core.util.Utils;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

public class HubServer {
    private final String id;
    private final Settings settings;
    private final DatagramSocket udpSocket;
    private final ServerSocket serverSocket;

    //peerManagerId <=> instance
    private final Map<String, AppInstance> instances = new ConcurrentHashMap<>();
    //peerId <=> peer
    private final Map<String, Peer> peers = new ConcurrentHashMap<>();
    private final Queue<DatagramPacket> peerRegistrationQueue = new ConcurrentLinkedQueue<>();
    private final Thread peerRegistrationWorker;
    private Thread acceptNewPeerManagerConnectionsWorker;

    public HubServer(Settings settings) {
        this.id = Id.generateId(Constants.HUB_PREFIX);
        this.settings = settings;
        int servicePort = settings.getInt(Settings.hubServicePort);
        int registrationPort = settings.getInt(Settings.hubRegistrationPort);

        try {
            serverSocket = new ServerSocket(servicePort);
            udpSocket = new DatagramSocket(registrationPort);
            udpSocket.setSoTimeout(settings.getInt(Settings.socketTimeout));
            Logger.log("Hub %s started with ports, TCP: %d UDP: %d", id, servicePort, registrationPort);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        acceptNewPeerManagerConnections();
        runUdpPeerRegistrationThread();
        peerRegistrationWorker = new Thread(() -> {
            while (!Thread.interrupted()) {
                DatagramPacket packet = peerRegistrationQueue.poll();
                if (packet != null) {
                    processPeerRequest(packet);
                }
            }
        });
        peerRegistrationWorker.start();
    }

    void acceptNewPeerManagerConnections() {
        acceptNewPeerManagerConnectionsWorker = new Thread(() -> {
            while (!Thread.interrupted()) {
                try {
                    Socket socket = serverSocket.accept();
                    registerPeerManager(socket);
                } catch (Exception se) {
                    Logger.log("Exception on acceptNewPeerManagerConnections thread: %s", se.getMessage());
                }
            }
        }, "acceptConnections-" + id);
        acceptNewPeerManagerConnectionsWorker.start();

    }

    private void registerPeerManager(Socket socket) {
        new Thread(() -> {
            try {
                AppInstance instance = getPeerManagerInfo(socket);
                this.instances.put(instance.getPeerManagerId(), instance);
                sendPeers(instance);
                Logger.log("Peer manager %s connected: appId=%s host=%s", instance.getPeerManagerId(), instance.getAppId(), instance.getHostId());
            } catch (Exception e) {
                e.printStackTrace(); //todo
            }
        }, "registerPeerManager-" + System.currentTimeMillis()).start();
    }

    private void sendPeers(AppInstance instance) {
        for (Peer peer : peers.values()) {
            if (instance.getAppId().equals(peer.getAppId())
                    && (peer.isRouter() || instance.getPeerManagerId().equals(peer.getRemotePeerManagerId()))) {
                writeToSocket(instance, peer);
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
                //todo remove all for PM from peers
                notifyPeerStateChanged(removed.getServerRouter(), State.DISCONNECTED);
                notifyPeerStateChanged(removed.getClientRouter(), State.DISCONNECTED);
                Logger.log("Peer manager %s disconnected: %s", removed.getPeerManagerId(), removed.getHostId());
                removed.getSocket().close();
            }
        } catch (Exception e) {
            Logger.log("Error while closing socket: %s", e.getMessage());
        }
    }

    void notifyPeerStateChanged(Peer peer, State state) {
        if (peer == null) {
            return;
        }
        Logger.log("Peer state changed: %s %s", state, peer);
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
            Logger.log("Writing for %s to socket: %s", appInstance.getPeerManagerId(), peer);
            BufferedOutputStream out = new BufferedOutputStream(appInstance.getSocket().getOutputStream());
            out.write(Utils.toJson(peer).getBytes(StandardCharsets.UTF_8));
            out.write(Constants.NEW_LINE);
            out.flush();
        } catch (SocketException se) {
            Logger.log("Exception while writing to socket for peer manager %s %s %s", appInstance.getPeerManagerId(), appInstance.getHostId(), se.getMessage());
            onPeerManagerDisconnects(appInstance);
        } catch (IOException ioe) {
            Logger.log("Exception while writing to socket for peer manager %s %s %s", appInstance.getPeerManagerId(), appInstance.getHostId(), ioe.getMessage());
            ioe.printStackTrace();//todo
        }
    }

    private void runUdpPeerRegistrationThread() {
        new Thread(() -> {
            Integer bufSize = settings.getInt(Settings.packetBufferSize);
            while (true) {
                byte[] buf = new byte[bufSize];
                DatagramPacket packet = new DatagramPacket(buf, buf.length);
                try {
                    udpSocket.receive(packet);
                    Logger.logPacket(id, packet, false);
                    boolean offered = peerRegistrationQueue.offer(packet);
                    if (!offered) {
                        Logger.log("Peer registration queue is full. Peer lost: %s:%s", packet.getAddress().getHostAddress(), packet.getPort()); //todo
                    }
                } catch (SocketTimeoutException e) {
                    //ignore
                } catch (SocketException e) {
                    Logger.log("Udp peer registration socket closed");
                    e.printStackTrace(); //todo
                    return;
                } catch (Exception e) {
                    throw new RuntimeException(e); //todo
                }
            }
        }).start();
    }

    private void processPeerRequest(DatagramPacket packet) {
        try {
            String message = new String(packet.getData(), StandardCharsets.UTF_8).trim();
            Logger.log("Hub %s received peer: %s", this.id, message);
            Host host = new Host(packet.getAddress().getHostAddress(), packet.getPort(), Protocol.UDP);
            Peer peer = Utils.fromJson(message, Peer.class);
            peer.setHost(host);

            AppInstance appInstance = instances.get(peer.getPeerManagerId());
            if (appInstance != null) {
                //todo remove
                appInstance.onPeerStateChanged(peer);
            }

            peers.put(peer.getId(), peer);

            notifyPeerStateChanged(peer, peer.getState());
        } catch (Exception e) {
            e.printStackTrace();//todo
        }
    }

    public void stop() throws IOException {
        serverSocket.close();
        udpSocket.close();
        peerRegistrationWorker.interrupt();
        acceptNewPeerManagerConnectionsWorker.interrupt();
    }

    public Map<String, Object> getStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("size", peers.keySet().size());

        int clientRouters = 0;
        int serverRouters = 0;
        int clientProxies = 0;
        int serverProxies = 0;
        for (Peer peer : peers.values()) {
            if (peer.isRouter() && peer.getMode() == Mode.CLIENT) {
                clientRouters++;
            } else if (peer.isRouter() && peer.getMode() == Mode.SERVER) {
                serverRouters++;
            } else if (!peer.isRouter() && peer.getMode() == Mode.SERVER) {
                serverProxies++;
            }else if (!peer.isRouter() && peer.getMode() == Mode.CLIENT) {
                clientProxies++;
            }
        }
        stats.put("clientRouters", clientRouters);
        stats.put("clientProxies", clientProxies);
        stats.put("serverRouters", serverRouters);
        stats.put("serverProxies", serverProxies);
        return stats;
    }
}