package net.relserver.core.peer;

import net.relserver.core.Constants;
import net.relserver.core.Id;
import net.relserver.core.Settings;
import net.relserver.core.Utils;
import net.relserver.core.app.App;
import net.relserver.core.proxy.Proxy;

import java.io.*;
import java.net.DatagramPacket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

public class PeerManager implements Id {
    private final String id;
    private final Socket serviceSocket;
    private final Host registrationServiceHost;
    private final App app;
    private final List<Consumer<Peer>> onNewRemotePeerActions = new CopyOnWriteArrayList<>(); //todo deal with not actual handlers

    public PeerManager(Settings settings, App app) {
        this.id = Utils.generateId(Constants.PEER_MANAGER_PREFIX);
        String hubIp = settings.getString(Settings.hubIp);
        int hubServicePort = settings.getInt(Settings.hubServicePort);
        int hubRegistrationPort = settings.getInt(Settings.hubRegistrationPort);

        Host service = new Host(hubIp, hubServicePort, Protocol.TCP);
        Host registrationService = new Host(hubIp, hubRegistrationPort, Protocol.UDP);
        if (service.getProtocol() != Protocol.TCP || registrationService.getProtocol() != Protocol.UDP) {
            throw new NullPointerException("Connection properties must not be null");
        }
        this.app = app;
        this.registrationServiceHost = registrationService;

        try {
            serviceSocket = new Socket(service.getIp(), service.getPort());
        } catch (Exception ex) {
            throw new RuntimeException("Error creating socket: " + ex.getMessage(), ex);
        }

        runPeerInfosReceiverThread();
    }

    private void runPeerInfosReceiverThread() {
        new Thread(() -> {
            try {
                BufferedOutputStream out = new BufferedOutputStream(serviceSocket.getOutputStream());
                String message = this.id + Constants.SEPARATOR + app.getId();
                out.write(message.getBytes(StandardCharsets.UTF_8));
                out.write(Constants.NEW_LINE);
                out.flush();

                BufferedReader in = new BufferedReader(new InputStreamReader(serviceSocket.getInputStream()));
                while (true) {
                    String peerInfo = in.readLine();
                    receiveRemotePeer(peerInfo);
                }
            } catch (IOException e) {
                Utils.log("Disconnected from Hub: " + e.getMessage());
                e.printStackTrace(); //todo
            }
        }, "peerInfoReceiver-" + this.id).start();
    }

    private void receiveRemotePeer(String peerInfo) {
        if (peerInfo == null || peerInfo.isEmpty()) {
            return; //todo notify?
        }
        try {
            Peer peer = Peer.of(peerInfo);
            if (!peer.getAppId().equals(app.getId())){
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
        String message = peer.toString();
        byte[] sendData = message.getBytes(StandardCharsets.UTF_8);
        DatagramPacket packet = new DatagramPacket(sendData, sendData.length);
        client.getPort().send(packet, this.registrationServiceHost);
        Utils.log("Registered on hub: " + message);
    }

    public void subscribeOnRemotePeerChanged(Consumer<Peer> action) {
        synchronized (onNewRemotePeerActions) {
            this.onNewRemotePeerActions.add(action);
        }
    }

    @Override
    public String getId() {
        return id;
    }

    public void close() {
        try {
            serviceSocket.close();
        } catch (Exception e) {
            Utils.log("Exception while closing service socket: " + e.getMessage());
        }
    }
}
