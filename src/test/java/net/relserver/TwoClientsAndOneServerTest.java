package net.relserver;

import net.relserver.util.TestUtils;

public class TwoClientsAndOneServerTest {

    public static void main(String[] args) {
        int realServerPort = 17897;
        TestUtils.createServer(realServerPort, "server1->");

        //prepare real client
        int clientPort1 = realServerPort - 1;
        TestUtils.createClient("client1->", clientPort1, 1);
        TestUtils.createClient("client2->", clientPort1, 1);

        ReLServerCliRunner.main(new String[]{"-mode=hub"});
        ReLServerCliRunner.main(new String[]{"-mode=client", "-hubIp=127.0.0.1", "-appPort=" + clientPort1});
        ReLServerCliRunner.main(new String[]{"-mode=server", "-hubIp=127.0.0.1", "-appPort=" + realServerPort});
    }
}
