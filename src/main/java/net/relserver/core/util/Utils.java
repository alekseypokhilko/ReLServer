package net.relserver.core.util;

import net.relserver.core.port.Port;
import net.relserver.core.peer.Peer;

import java.util.function.Function;

public final class Utils {

    private Utils() {
        throw new IllegalStateException();
    }

    public static <T> void sendWithRetry(Port<T> port, T packet, Peer peer, int retry, long delay, Function<String, Peer> peerSupplier) {
        new Thread(() -> {
            int count = 0;
            while (true) {
                //packets can reach their destination after receiving information about the remote proxy
                Peer actual = peerSupplier.apply(peer.getId());

                try {
                    port.send(packet, actual.getHost());
                    break;
                } catch (Exception e) {
                    try {
                        Thread.sleep(delay); //todo
                    } catch (Exception ignore) {
                    }
                }
                count++;
                if (count > retry) {
                    Logger.log("Not found a remote peer. Retry limit exceeds for sending to: %s", actual);
                    break;
                }
            }
        }, "sendWithRetry-" + System.currentTimeMillis()).start();
    }

    public static String valueOrElseNull(String value) {
        return "null".equals(value) ? null : value;
    }
}
