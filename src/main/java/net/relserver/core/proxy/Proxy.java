package net.relserver.core.proxy;

import net.relserver.core.Constants;
import net.relserver.core.Id;
import net.relserver.core.util.Logger;
import net.relserver.core.util.Utils;
import net.relserver.core.port.UdpPort;
import net.relserver.core.peer.Peer;

import java.net.DatagramPacket;
import java.nio.charset.StandardCharsets;
import java.util.function.Function;

public interface Proxy extends Id {

    Peer getInfo();

    UdpPort getPort();

    void setRemotePeer(Peer peer);

    default void sendHandshakePacket(Peer peer) {
        String msg = Constants.HANDSHAKE_MESSAGE_PREFIX + ":" + this.getId() + "<=>" + peer.getId();
        Logger.log("Sending handshake: %s", msg);
        byte[] senData = msg.getBytes(StandardCharsets.UTF_8);
        DatagramPacket packet = new DatagramPacket(senData, senData.length);
        Utils.sendWithRetry(getPort(), packet, peer, 10, 100L, this.getPeerSupplier());
    }

    Function<String, Peer> getPeerSupplier();

    @Override
    default String getId() {
        return getInfo().getId();
    }

    void close();
}
