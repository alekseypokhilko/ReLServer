package net.relserver;

public class OneClientAndTwoServerTest {

    public static void main(String[] args) {
        int realServerPort = 17897;
        int realServer2Port = 17898;
        TestUtils.createServer(realServerPort, "server1->");
        TestUtils.createServer(realServer2Port, "server2->");

        //prepare real client
        int clientPort = realServerPort - 1;
        TestUtils.createClient("client->", clientPort);

        ReLserverCliRunner.main(new String[]{"-mode=hub"});
        ReLserverCliRunner.main(new String[]{"-mode=client", "-log=v","-appPort=" + clientPort});
        ReLserverCliRunner.main(new String[]{"-mode=server", "-log=v","-appPort=" + realServerPort});
        ReLserverCliRunner.main(new String[]{"-mode=server", "-log=v","-appPort=" + realServer2Port});
    }
}
