package net.relserver;

import net.relserver.util.TestUtils;

public class OneClientAndOneServerTest {

    public static void main(String[] args) {
        //prepare real server
        int realServerPort = 17897;
        TestUtils.createServer(realServerPort, "server->");

        //prepare real client
        int clientPort = 30000;
        TestUtils.createClient("client", clientPort, 1);

        ReLServerCliRunner.main(new String[]{"-mode=hub", "-log=true", "-logPacket=true"});
        ReLServerCliRunner.main(new String[]{"-mode=client", "-log=true", "-logPacket=true", "-hubIp=127.0.0.1", "-appPort=" + clientPort});
        ReLServerCliRunner.main(new String[]{"-mode=server", "-log=true", "-logPacket=true", "-hubIp=127.0.0.1", "-appPort=" + realServerPort});
    }
}
