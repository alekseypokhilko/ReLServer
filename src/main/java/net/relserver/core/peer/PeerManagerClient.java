package net.relserver.core.peer;

import net.relserver.core.util.Logger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.function.Consumer;

public class PeerManagerClient {
    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    private Thread receiver;
    private Consumer<String> onPeerReceived;

    public void startConnection(Host service) throws IOException {
        socket = new Socket();
        socket.connect(new InetSocketAddress(service.getIp(), service.getPort()), 5000);
        out = new PrintWriter(socket.getOutputStream(), true);
        in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
    }

    public void sendMessage(String msg) {
        out.println(msg);
    }

    public String receiveMessage() throws IOException {
        return in.readLine();
    }

    public void subscribeOnPeers(Consumer<String> onPeerReceived) {
        if (this.onPeerReceived == null) {
            this.onPeerReceived = onPeerReceived;
        } else {
            return;
        }
        this.receiver = new Thread(() -> {
            while (!Thread.interrupted()) {
                try {
                    String peerInfo = receiveMessage();
                    onPeerReceived.accept(peerInfo);
                } catch (IOException e) {
                    break;
                }
            }
        }, "peerReceiver");
        receiver.start();
    }

    public void stop() {
        receiver.interrupt();
        try {
            socket.close();
        } catch (IOException e) {
            Logger.log("Exception while closing client socket: %s", e.getMessage());
        }
    }
}
