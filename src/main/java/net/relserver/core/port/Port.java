package net.relserver.core.port;

import net.relserver.core.Id;
import net.relserver.core.peer.Host;

import java.util.function.Consumer;

public interface Port<T> extends Id {
    void setOnPacketReceived(Consumer<T> onPacketReceived);
    void send(T packet, Host host);
    void close();
}
