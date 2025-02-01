package net.relserver;

import net.relserver.core.hub.HubServer;
import net.relserver.util.TestClient;
import net.relserver.util.TestServer;
import net.relserver.util.TestUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ManyClientsAndManyServersTest {
    private static final int SERVER_COUNT = 5;
    private static final int CLIENT_COUNT = 200;

    public static void main(String[] args) throws Exception {
        HubServer hub = ReLServerCliRunner.of(new String[]{"-mode=hub", /*"-log=true"*/}).getHub();

        List<ReLServer> serverRouters = new ArrayList<>();
        List<TestServer> fakeServers = new ArrayList<>();
        int realServerPort = 10000;
        for (int i = 0; i < SERVER_COUNT; i++) {
            int port = realServerPort + i;
            fakeServers.add(TestUtils.createServer(port, "server" + i));
            ReLServer server = ReLServerCliRunner.of(new String[]{"-mode=server", /*"-log=true",*/ "-hubIp=127.0.0.1", "-appPort=" + port});
            serverRouters.add(server);
        }

        int clientPort = 30000;
        List<TestClient> clients = new ArrayList<>();
        ReLServer clientRouter = ReLServerCliRunner.of(new String[]{"-mode=client", /*"-log=true",*/ "-hubIp=127.0.0.1", "-appPort=" + clientPort});
        for (int i = 0; i < CLIENT_COUNT; i++) {
            Thread.sleep(100L);
            clients.add(TestUtils.createClient("client" + i, clientPort, 1));
        }

        Thread.sleep(9000L);
        Map<String, Object> stats = hub.getStats();
        System.out.println("HUB: " + stats);

        int successedTests = 0;
        int failedTests = 0;
        for (TestClient client : clients) {
            successedTests += client.success;
            failedTests += client.failed;
        }
        System.out.println("TestClient successedTests: " + successedTests);
        System.out.println("TestClient failedTests: " + failedTests);
        int expected = SERVER_COUNT * CLIENT_COUNT;
        //TEST=false | 998<=>1000 //todo fix packet loss
        System.out.println("TEST=" + (successedTests == expected) + " | " + successedTests + "<=>" + expected);
        hub.stop();
        serverRouters.forEach(ReLServer::stop);
        fakeServers.forEach(TestServer::stop);
        clients.forEach(TestClient::stop);
        clientRouter.stop();
    }
}
