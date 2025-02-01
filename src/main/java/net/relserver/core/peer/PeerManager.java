package net.relserver.core.peer;

import net.relserver.core.*;
import net.relserver.core.api.Id;
import net.relserver.core.app.App;
import net.relserver.core.api.Proxy;
import net.relserver.core.hub.HubLoader;
import net.relserver.core.util.Logger;
import net.relserver.core.util.Utils;

import java.io.*;
import java.net.DatagramPacket;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class PeerManager implements Id {
    private final String id;
    private Socket serviceSocket;
    private final PeerRegistry peerRegistry;
    private final Host service;
    private final Host registrationServiceHost;
    private final App app;

    public PeerManager(Settings settings, App app, PeerRegistry peerRegistry) {
        this.id = Id.generateId(Constants.PEER_MANAGER_PREFIX);
        this.peerRegistry = peerRegistry;
        this.app = app;

        List<String> hubIps = getHubIps(settings);
        if (hubIps.isEmpty()) {
            throw new IllegalArgumentException("Cannot find Hub to connect");
        }

        String hubIp = hubIps.get(0);
        int hubServicePort = settings.getInt(Settings.hubServicePort);
        int hubRegistrationPort = settings.getInt(Settings.hubRegistrationPort);
        this.service = new Host(hubIp, hubServicePort, Protocol.TCP);
        this.registrationServiceHost = new Host(hubIp, hubRegistrationPort, Protocol.UDP);
    }

    private static List<String> getHubIps(Settings settings) {
        String hubIp = settings.getString(Settings.hubIp);
        List<String> remoteIps;
        List<String> resourceIps;
        if (hubIp != null && !hubIp.isEmpty()) {
            ArrayList<String> ip = new ArrayList<>();
            ip.add(hubIp);
            return ip;
        } else if (!(remoteIps = HubLoader.loadFromRemoteRepository()).isEmpty()) {
            return remoteIps;
        } else if (!(resourceIps = HubLoader.loadFromResourcesFolder()).isEmpty()) {
            return resourceIps;
        } else {
            ArrayList<String> localhostHub = new ArrayList<>();
            localhostHub.add("127.0.0.1");
            return localhostHub;
        }
    }

    public void start() {
        new Thread(() -> {
            try {
                serviceSocket = new Socket();
                serviceSocket.connect(new InetSocketAddress(service.getIp(), service.getPort()), 5000);

                BufferedOutputStream out = new BufferedOutputStream(serviceSocket.getOutputStream());
                String message = this.id + Constants.SEPARATOR + app.getId();
                out.write(message.getBytes(StandardCharsets.UTF_8));
                out.write(Constants.NEW_LINE);
                out.flush();

                BufferedReader in = new BufferedReader(new InputStreamReader(serviceSocket.getInputStream()));
                while (!Thread.interrupted()) {
                    String peerInfo = in.readLine();
                    Logger.log("Peer manager %s received peer: %s", id, peerInfo);
                    receiveRemotePeer(peerInfo);
                }
            } catch (Exception e) {
                Logger.log("Disconnected from Hub: %s", e.getMessage()); //todo throw from main thread
            }
        }, "peerInfoReceiver-" + this.id).start();
        Logger.log("Peer manager %s started with app=%s", id, app);
    }

    private void receiveRemotePeer(String peerInfo) {
        if (peerInfo == null || peerInfo.isEmpty()) {
            return;
        }
        try {
            Peer peer = Utils.fromJson(peerInfo, Peer.class);
            if (!app.getId().equals(peer.getAppId()) && !(!peer.isRouter() && id.equals(peer.getPeerManagerId()))) {
                return;
            }
            peerRegistry.onPeerChanged(peer);
        } catch (Exception e) {
            e.printStackTrace();//todo
        }
    }

    public void notifyPeerState(Proxy proxy, State state) {
        Peer peer = proxy.getPeerPair().getPeer();
        peer.setState(state);
        String message = Utils.toJson(peer);

        byte[] sendData = message.getBytes(StandardCharsets.UTF_8);
        DatagramPacket packet = new DatagramPacket(sendData, sendData.length);
        proxy.getPortPair().getP2pPort().send(packet, this.registrationServiceHost);
        Logger.log("Registered on hub: %s", message);
    }

    @Override
    public String getId() {
        return id;
    }

    public void stop() {
        try {
            if (serviceSocket != null) {
                serviceSocket.close();
            }
        } catch (Exception e) {
            Logger.log("Exception while closing service socket: %s", e.getMessage());
        }
    }
}
