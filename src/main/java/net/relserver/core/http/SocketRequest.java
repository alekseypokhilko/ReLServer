package net.relserver.core.http;

import net.relserver.core.api.model.Request;
import net.relserver.core.util.Logger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.Socket;

public class SocketRequest {
    private final Socket socket;
    private final PrintWriter out;
    private final BufferedReader in;

    private SocketRequest(String ip, int port) throws IOException {
        socket = new Socket();
        socket.connect(new InetSocketAddress(ip, port), 5000);
        out = new PrintWriter(socket.getOutputStream(), true);
        in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
    }

    public static String execute(String host, int port, Request request) {
        try {
            SocketRequest socketRequest = new SocketRequest(host, port);
            String response = socketRequest.execute(request);
            socketRequest.close();
            return response;
        } catch (Exception e) {
            return null;
        }
    }

    public String execute(Request request) {
        try {
            out.println(request.toString());
            return in.readLine();
        } catch (Exception e) {
            Logger.log("Exception execute request: %s", e.getMessage());
            return null;
        }
    }

    private void close() {
        try {
            socket.close();
        } catch (IOException e) {
            Logger.log("Exception while closing client socket: %s", e.getMessage());
        }
    }
}
