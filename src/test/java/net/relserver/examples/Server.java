package net.relserver.examples;

import net.relserver.ReLserverCliRunner;

public class Server {
    public static void main(String[] args) {
        //prepare real client
        int port = 7893;
        String hubIp = "195.74.86.111";
        String localServerIp = "192.168.0.23";
        String appId = "mc4_ios";

//        ReLserverCliRunner.main(new String[]{"server"});
        ReLserverCliRunner.main(new String[]{
                "-mode=server",
                "-appId=" + appId,
                "-hubIp=" + hubIp,
                "-localServerIp=" + localServerIp,
                "-appPort=" + port
        });
    }
}
