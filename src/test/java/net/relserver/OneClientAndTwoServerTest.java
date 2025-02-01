package net.relserver;

import net.relserver.util.TestUtils;

public class OneClientAndTwoServerTest {

    public static void main(String[] args) {
        int realServerPort = 17897;
        int realServer2Port = 17898;
        TestUtils.createServer(realServerPort, "server1->");
        TestUtils.createServer(realServer2Port, "server2->");

        //prepare real client
        int clientPort = realServerPort - 1;
        TestUtils.createClient("client->", clientPort, 1);

        ReLServerCliRunner.main(new String[]{"-mode=hub"});
        ReLServerCliRunner.main(new String[]{"-mode=client", "-hubIp=127.0.0.1", "-appPort=" + clientPort});
        ReLServerCliRunner.main(new String[]{"-mode=server", "-hubIp=127.0.0.1", "-appPort=" + realServerPort});
        ReLServerCliRunner.main(new String[]{"-mode=server", "-hubIp=127.0.0.1", "-appPort=" + realServer2Port});
    }
}
