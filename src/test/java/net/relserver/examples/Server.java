package net.relserver.examples;

import net.relserver.ReLServerCliRunner;

public class Server {
    public static void main(String[] args) {
        //prepare real client
//        int port = 7893;
//        String hubIp = "185.242.86.223";
        String localServerIp = "192.168.0.23";
        String appId = "glmc4zh";

        ReLServerCliRunner.main(new String[]{
                "-mode=server",
                "-appId=" + appId,
                "-localServerIp=" + localServerIp,
//                "-log=true",
//                "-logPacket=true",
//                "-hubIp=" + hubIp,
//                "-appPort=" + port
        });
    }
}
