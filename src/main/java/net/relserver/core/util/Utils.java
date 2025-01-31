package net.relserver.core.util;

import com.google.gson.Gson;
import net.relserver.core.Constants;
import net.relserver.core.api.Port;
import net.relserver.core.peer.Peer;

import java.net.DatagramPacket;
import java.util.function.Function;

public final class Utils {

    private Utils() {
        throw new IllegalStateException();
    }

    public static void sendWithRetry(Port<DatagramPacket> port, DatagramPacket packet, Peer peer, int retry, long delay, Function<String, Peer> peerSupplier) {
        new Thread(() -> {
            int count = 0;
            //String message = new String(packet.getData()).trim(); //debug
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

    public static String toJson(Object o) {
        return new Gson().toJson(o);
    }

    public static <T> T fromJson(String str, Class<T> cls) {
        return new Gson().fromJson(str, cls);
    }

    public static boolean isHandshake(byte[] packetData) {
        return packetData.length > 3
                && packetData[0] == Constants.HANDSHAKE_PREFIX_BYTES[0]
                && packetData[1] == Constants.HANDSHAKE_PREFIX_BYTES[1]
                && packetData[2] == Constants.HANDSHAKE_PREFIX_BYTES[2];
    }
}
