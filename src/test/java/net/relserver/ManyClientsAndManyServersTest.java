package net.relserver;

public class ManyClientsAndManyServersTest {
    private static final int SERVER_COUNT = 2;
    private static final int CLIENT_COUNT = 2;

    public static void main(String[] args) throws InterruptedException {
        ReLServerCliRunner.main(new String[]{"-mode=hub", "-log=true"});

        int clientPort = 30000;
        ReLServerCliRunner.main(new String[]{"-mode=client", "-log=true", "-hubIp=127.0.0.1", "-appPort=" + clientPort});
        for (int i = 0; i < CLIENT_COUNT; i++) {
            TestUtils.createClient("client" + i, clientPort);
        }

        //todo bug if client was created first
        int realServerPort = 10000;
        for (int i = 0; i < SERVER_COUNT; i++) {
            int port = realServerPort + i;
            TestUtils.createServer(port, "server" + i);
            ReLServerCliRunner.main(new String[]{"-mode=server", "-log=true", "-hubIp=127.0.0.1", "-appPort=" + port});
            Thread.sleep(300L);
        }
    }
}
