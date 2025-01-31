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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

public class PeerManager implements Id {
    private final String id;
    private Socket serviceSocket;
    private final Host service;
    private final Host registrationServiceHost;
    private final App app;
    private final List<Consumer<Peer>> onNewRemotePeerActions = new CopyOnWriteArrayList<>();
    private final Map<String, Peer> pm = new HashMap<>();

    public PeerManager(Settings settings, App app) {
        this.id = Id.generateId(Constants.PEER_MANAGER_PREFIX);
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

        Peer registrationHost = new Peer(id, id, null, null, null, null, app.getId(), registrationServiceHost);
        pm.put(registrationHost.getId(), registrationHost);
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
                while (true) {
                    String peerInfo = in.readLine();
                    Logger.log("Peer manager %s received peer: %s", id, peerInfo);
                    receiveRemotePeer(peerInfo);
                }
            } catch (Exception e) {
                Logger.log("Disconnected from Hub: %s", e.getMessage());
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
            if (!peer.getAppId().equals(app.getId())) {
                return;
            }
            notifyRemotePeerState(peer);
        } catch (NullPointerException e) {
            e.printStackTrace();
            //todo Cannot invoke "String.startsWith(String)" because "peerInfo" is null
        } catch (Exception e) {
            e.printStackTrace();//todo
        }
    }

    private void notifyRemotePeerState(Peer peer) {
        new Thread(() -> {
            for (Consumer<Peer> action : onNewRemotePeerActions) {
                action.accept(peer);  //todo run on scheduler?
            }
        }, "notifyRemotePeerState-" + System.currentTimeMillis()).start();
    }

    public void notifyPeerState(Proxy client, State state) {
        Peer peer = client.getInfo();
        peer.setState(state);
        String message = Utils.toJson(peer);

        byte[] sendData = message.getBytes(StandardCharsets.UTF_8);
        DatagramPacket packet = new DatagramPacket(sendData, sendData.length);
        client.getPort().send(packet, this.registrationServiceHost);
        Logger.log("Registered on hub: %s", message);
    }

    public void subscribeOnRemotePeerChanged(Consumer<Peer> action) {
        this.onNewRemotePeerActions.add(action);
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
