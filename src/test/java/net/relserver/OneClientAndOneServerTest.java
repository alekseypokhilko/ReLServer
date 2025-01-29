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
        ReLServerCliRunner.main(new String[]{"-mode=client","-appPort=" + clientPort});
        ReLServerCliRunner.main(new String[]{"-mode=server","-appPort=" + realServerPort});
    }
}
