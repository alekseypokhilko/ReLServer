package net.relserver.hub.handler;

import net.relserver.core.api.model.Operation;
import net.relserver.hub.Client;

public interface RequestHandler {
    Operation getOperation();
    void handle(Client client, String message);
}
