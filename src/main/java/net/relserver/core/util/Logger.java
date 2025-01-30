package net.relserver.core.util;

import net.relserver.core.Settings;

import java.net.DatagramPacket;
import java.time.LocalDateTime;

public class Logger {
    public static boolean LOG_ENABLED = true;
    public static boolean LOG_PACKET = false;

    public static void log(String format, Object... args) {
        if (LOG_ENABLED) {
            System.out.println("[" + LocalDateTime.now() + "] " + String.format(format, args));
        }
    }

    public static void logPacket(String portId, DatagramPacket receivePacket, boolean send) {
        if (LOG_PACKET) {
            log("%s %s%s:%d '%s'",portId, send ? "sending  to   => " : "received from <= ", receivePacket.getAddress().getHostAddress().trim(), receivePacket.getPort(), new String(receivePacket.getData()).trim());
        }
    }

    public static void init(Settings settings) {
        Logger.LOG_ENABLED = "true".equals(settings.getString(Settings.log));
        Logger.LOG_PACKET = "true".equals(settings.getString(Settings.logPacket));
    }
}
