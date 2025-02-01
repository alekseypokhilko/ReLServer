package net.relserver;

import net.relserver.core.hub.HubServer;
import net.relserver.core.proxy.ClientRouter;
import net.relserver.core.proxy.ServerRouter;
import net.relserver.util.TestUtils;

import java.util.ArrayList;
import java.util.List;

public class ManyClientsAndManyServersTest {
    //todo fix floating successful test messages when count > 15
    private static final int SERVER_COUNT = 20;
    private static final int CLIENT_COUNT = 20;

    public static void main(String[] args) throws Exception {
        HubServer hub = ReLServerCliRunner.of(new String[]{"-mode=hub", "-log=true"}).getHub();

        List<ServerRouter> serverRouters = new ArrayList<>();
        int realServerPort = 10000;
        for (int i = 0; i < SERVER_COUNT; i++) {
            int port = realServerPort + i;
            TestUtils.createServer(port, "server" + i);
            ServerRouter server = ReLServerCliRunner.of(new String[]{"-mode=server", "-log=true", "-hubIp=127.0.0.1", "-appPort=" + port}).getServer();
            serverRouters.add(server);
        }

        int clientPort = 30000;
        ClientRouter client = ReLServerCliRunner.of(new String[]{"-mode=client", "-log=true", "-hubIp=127.0.0.1", "-appPort=" + clientPort}).getClient();
        for (int i = 0; i < CLIENT_COUNT; i++) {
            Thread.sleep(100L);
            TestUtils.createClient("client" + i, clientPort);
        }

        Thread.sleep(9000L);
        System.out.println("DONE");
    }
}
