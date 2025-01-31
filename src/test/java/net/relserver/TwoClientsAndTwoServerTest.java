package net.relserver;

import net.relserver.util.TestUtils;

public class TwoClientsAndTwoServerTest {

    public static void main(String[] args) {
        int realServerPort = 17897;
        int realServerPort2 = 17898;
        TestUtils.createServer(realServerPort, "server1->");
        TestUtils.createServer(realServerPort2, "server2->");

        //prepare real client
        int clientPort1 = realServerPort - 1;
        TestUtils.createClient("client1->", clientPort1);
        TestUtils.createClient("client2->", clientPort1);

        ReLServerCliRunner.main(new String[]{"-mode=hub", "-log=true" });
        ReLServerCliRunner.main(new String[]{"-mode=client", "-log=true", "-hubIp=127.0.0.1", "-appPort=" + clientPort1});
        ReLServerCliRunner.main(new String[]{"-mode=server", "-log=true",  "-hubIp=127.0.0.1", "-appPort=" + realServerPort});
        ReLServerCliRunner.main(new String[]{"-mode=server", "-log=true",  "-hubIp=127.0.0.1", "-appPort=" + realServerPort2});
    }
}
