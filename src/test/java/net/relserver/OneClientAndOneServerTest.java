package net.relserver;

public class OneClientAndOneServerTest {

    public static void main(String[] args) {
        //prepare real server
        int realServerPort = 17897;
        TestUtils.createServer(realServerPort, "server->");

        //prepare real client
        int clientPort = realServerPort + 1;
        TestUtils.createClient("client->", clientPort);

        ReLServerCliRunner.main(new String[]{"-mode=hub"});
        ReLServerCliRunner.main(new String[]{"-mode=client", "-hubIp=127.0.0.1", "-appPort=" + clientPort});
        ReLServerCliRunner.main(new String[]{"-mode=server", "-hubIp=127.0.0.1", "-appPort=" + realServerPort});
    }
}
