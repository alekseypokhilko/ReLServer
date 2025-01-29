package net.relserver.core.app;

public class App {
    private final String id;
    private final int port;
    private final String title;

    public App(String id, int port, String title) {
        this.id = id;
        this.port = port;
        this.title = title;
    }

    public String getId() {
        return id;
    }

    public int getPort() {
        return port;
    }

    public String getTitle() {
        return title;
    }

    @Override
    public String toString() {
        return "{" +
                "id='" + id + '\'' +
                ", port=" + port +
                ", title='" + title + '\'' +
                '}';
    }
}
