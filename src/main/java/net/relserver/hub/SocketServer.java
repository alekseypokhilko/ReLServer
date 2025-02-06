package net.relserver.hub;

import net.relserver.core.api.model.Operation;
import net.relserver.core.api.model.Request;
import net.relserver.core.util.Logger;
import net.relserver.hub.handler.RequestHandler;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class SocketServer {
    private final ServerSocket serverSocket;
    private final Thread connectionHandler;
    private final Map<Operation, RequestHandler> handlers = new HashMap<>();

    public SocketServer(int port) {
        try {
            serverSocket = new ServerSocket(port);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        connectionHandler = new Thread(this::connectionHandlerLoop, "acceptConnections");
    }

    public void registerHandler(RequestHandler handler) {
        handlers.put(handler.getOperation(), handler);
    }

    private void connectionHandlerLoop() {
        while (!Thread.interrupted()) {
            try {
                Socket socket = serverSocket.accept();
                CompletableFuture.runAsync(() -> handleRequest(socket));
            } catch (Exception se) {
                Logger.log("Exception on connectionHandlerLoop: %s", se.getMessage());
                break;
            }
        }
    }

    private void handleRequest(Socket socket) {
        Client client = null;
        try {
            client = new Client(socket);
            Request request = client.receiveRequest();
            RequestHandler requestHandler = handlers.get(request.getOperation());
            if (requestHandler != null) {
                requestHandler.handle(client, request.getMessage());
            }
        } catch (IOException e) {
            if (client != null) {
                client.stop();
            }
        }
    }

    public void start() {
        connectionHandler.start();
    }

    public void stop() {
        try {
            connectionHandler.interrupt();
            serverSocket.close();
        } catch (IOException e) {
            Logger.log("Exception on serverSocket stop: %s", e.getMessage());
        }
    }
}
