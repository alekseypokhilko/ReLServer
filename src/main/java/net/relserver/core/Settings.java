package net.relserver.core;

import net.relserver.core.peer.Mode;
import net.relserver.core.util.Logger;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class Settings {
    public static final String mode = "mode";
    public static final String hubIp = "hubip";
    public static final String hubServicePort = "hubserviceport";
    public static final String hubRegistrationPort = "hubregistrationport";
    public static final String localServerIp = "localserverip";
    public static final String appPort = "appport";
    public static final String appId = "appid";
    public static final String socketTimeout = "sockettimeout";
    public static final String udpRegistrationPacketCount = "udpregistrationpacketcount";
    public static final String udpRegistrationPacketDelay = "udpregistrationpacketdelay";
    public static final String peerPollIntervalMs = "peerpollintervalMs";
    public static final String packetBufferSize = "packetbuffersize";
    public static final String log = "log";
    public static final String logPacket = "logPacket";
    private static final Set<String> KEYS = Set.of(
            mode, hubIp, hubServicePort, hubRegistrationPort, localServerIp, appPort, appId,
            socketTimeout, udpRegistrationPacketCount, peerPollIntervalMs, packetBufferSize,
            udpRegistrationPacketDelay, log, logPacket
    );

    private final Map<String, String> params = new HashMap<>();

    public Settings() {
        init();
    }

    public Settings(String[] args) {
        init();

        if (args == null) {
            return;
        }

        for (String arg : args) {
            try {
                if (arg.startsWith("-")) {
                    String[] tokens = arg.split("=");
                    String param = tokens[0].replace("-", "").toLowerCase(Locale.ENGLISH);
                    if (KEYS.contains(param)) {
                        params.put(param, tokens[1]);
                    } else {
                        throw new IllegalArgumentException(param);
                    }
                }
            } catch (IllegalArgumentException e) {
                Logger.log("Illegal option: %s", e.getMessage());
            }
        }
    }

    private void init() {
        params.put(mode, "CLIENT_SERVER");
        params.put(localServerIp, "127.0.0.1");

        params.put(hubServicePort, "9000");
        params.put(hubRegistrationPort, "9001");

        params.put(socketTimeout, "3000");
        params.put(udpRegistrationPacketCount, "1");
        params.put(udpRegistrationPacketDelay, "100");
        params.put(peerPollIntervalMs, "1000");
        params.put(packetBufferSize, "8192");
    }

    public Mode getMode() {
        return Mode.valueOf(params.get(mode).toUpperCase(Locale.ENGLISH));
    }

    public String getLocalServerIp() {
        return params.get(localServerIp);
    }

    public Integer getInt(String key) {
        String value = params.get(key);
        return value == null ? null : Integer.parseInt(value);
    }

    public Long getLong(String key) {
        String value = params.get(key);
        return value == null ? null : Long.parseLong(value);
    }

    public String getString(String key) {
        return params.get(key);
    }

    public String set(String key, String value) {
        return params.put(key, value);
    }

    @Override
    public String toString() {
        return params.toString();
    }
}
