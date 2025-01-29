package net.relserver;

public class TwoClientsAndOneServerTest {

    public static void main(String[] args) {
        int realServerPort = 17897;
        TestUtils.createServer(realServerPort, "server1->");

        //prepare real client
        int clientPort1 = realServerPort - 1;
        int clientPort2 = realServerPort - 2;
        TestUtils.createClient("client1->", clientPort1);
        TestUtils.createClient("client2->", clientPort2);

        ReLServerCliRunner.main(new String[]{"-mode=hub"});
        ReLServerCliRunner.main(new String[]{"-mode=client","-appPort=" + clientPort1});
        ReLServerCliRunner.main(new String[]{"-mode=client","-appPort=" + clientPort2});
        ReLServerCliRunner.main(new String[]{"-mode=server","-appPort=" + realServerPort});
    }
}
