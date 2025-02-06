package net.relserver.hub;

import net.relserver.core.api.model.Request;
import net.relserver.core.util.Logger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.function.Consumer;

public class Client {
    private final String hostId;
    private final Socket socket;
    private final PrintWriter out;
    private final BufferedReader in;
    private String peerManagerId;
    private String appId;
    private Consumer<Client> onStop;

    public Client(Socket socket) throws IOException {
        this.socket = socket;
        this.hostId = socket.getInetAddress().getHostAddress() + ":" + socket.getPort();
        out = new PrintWriter(socket.getOutputStream(), true);
        in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
    }

    public String getHostId() {
        return hostId;
    }

    public void setOnStop(Consumer<Client> onStop) {
        this.onStop = onStop;
    }

    public void setPeerManagerId(String peerManagerId) {
        this.peerManagerId = peerManagerId;
    }

    public void setAppId(String appId) {
        this.appId = appId;
    }

    public String getPeerManagerId() {
        return peerManagerId;
    }

    public String getAppId() {
        return appId;
    }

    public void sendMessage(String msg) {
        //Logger.log("Writing for %s to socket: %s", peerManagerId, msg);
        out.println(msg);
    }

    public Request receiveRequest() throws IOException {
        String message = in.readLine();
        return Request.of(message);
    }

    public void stop() {
        try {
            if (!socket.isClosed()) {
                socket.close();
                onStop.accept(this);
            }
        } catch (IOException e) {
            Logger.log("Exception while closing client socket: %s", e.getMessage());
        }
    }
}