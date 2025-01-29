package net.relserver;

public class OneClientAndOneServerTest {

    public static void main(String[] args) {
        //prepare real server
        int realServerPort = 17897;
        TestUtils.createServer(realServerPort, "server->");

        //prepare real client
        int clientPort = realServerPort + 1;
        TestUtils.createClient("client->", clientPort);

        ReLserverCliRunner.main(new String[]{"-mode=hub", "-log=v"});
        ReLserverCliRunner.main(new String[]{"-mode=client", "-log=v","-appPort=" + clientPort});
        ReLserverCliRunner.main(new String[]{"-mode=server", "-log=v","-appPort=" + realServerPort});
    }
}
