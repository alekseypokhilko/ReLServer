package net.relserver.core.api.model;

public class AppStat {
    private final String appId;
    private int clients;
    private int servers;

    public AppStat(String appId, int clients, int servers) {
        this.appId = appId;
        this.clients = clients;
        this.servers = servers;
    }

    public String getAppId() {
        return appId;
    }

    public int getClients() {
        return clients;
    }

    public int getServers() {
        return servers;
    }

    public void incrementClients() {
        this.clients = this.clients + 1;
    }

    public void incrementServers() {
        this.servers = this.servers + 1;
    }

    @Override
    public String toString() {
        return "AppStats{" +
                "appId='" + appId + '\'' +
                ", clients=" + clients +
                ", servers=" + servers +
                '}';
    }
}
