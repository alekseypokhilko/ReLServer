package net.relserver;

import net.relserver.util.TestUtils;

public class ManyClientsAndManyServersTest {
    private static final int SERVER_COUNT = 20;
    private static final int CLIENT_COUNT = 20;

    public static void main(String[] args) {
        ReLServerCliRunner.main(new String[]{"-mode=hub", "-log=true"});

        int realServerPort = 10000;
        for (int i = 0; i < SERVER_COUNT; i++) {
            int port = realServerPort + i;
            TestUtils.createServer(port, "server" + i);
            ReLServerCliRunner.main(new String[]{"-mode=server", "-log=true", "-hubIp=127.0.0.1", "-appPort=" + port});
        }

        int clientPort = 30000;
        ReLServerCliRunner.main(new String[]{"-mode=client", "-log=true", "-hubIp=127.0.0.1", "-appPort=" + clientPort});
        for (int i = 0; i < CLIENT_COUNT; i++) {
            TestUtils.createClient("client" + i, clientPort);
        }

        //todo fix floating successful test messages
    }
}
