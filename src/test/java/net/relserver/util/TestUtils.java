package net.relserver.util;

public class TestUtils {

    public static TestServer createServer(int realServerPort, String tag) {
        return new TestServer(realServerPort, tag);
    }

    public static TestClient createClient(String tag, int port, int count) {
        return new TestClient(tag, port, count);
    }

}
