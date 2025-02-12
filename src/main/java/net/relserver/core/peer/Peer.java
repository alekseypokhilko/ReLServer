package net.relserver.core.peer;

import static net.relserver.core.Constants.SEPARATOR;

public class Peer {
    private static final String NULL_HOST = SEPARATOR + null + SEPARATOR + null + SEPARATOR + null;

    /**
     * Peer manager id where current peer was created
     */
    private final String pmId;
    /**
     * Peer id
     */
    private final String id;
    private State state;
    private final Mode mode;
    /**
     * Remote peer id for p2p connection
     */
    private final String rpId;
    /**
     * Remote peer manager id for notification
     */
    private final String rpmId;
    /**
     * Application id for filtering peers on the hub and creating proxies
     */
    private final String appId;
    private Host host;
    public transient long timestamp = System.currentTimeMillis();

    public static Peer of(String peerManagerId, String id, State state, Mode mode, String remotePeerManagerId, String peerId, String appId, Host host) {
        return new Peer(peerManagerId, id, state, mode, remotePeerManagerId, peerId, appId, host);
    }

    public Peer(String peerManagerId, String id, State state, Mode mode, String remotePeerManagerId, String remotePeerId, String appId, Host host) {
        this.pmId = peerManagerId;
        this.id = id;
        this.state = state;
        this.mode = mode;
        this.rpmId = remotePeerManagerId;
        this.rpId = remotePeerId;
        this.appId = appId;
        this.host = host;
    }

    @Override
    public String toString() {
        return pmId + SEPARATOR +
                id + SEPARATOR +
                state + SEPARATOR +
                mode + SEPARATOR +
                rpmId + SEPARATOR +
                rpId + SEPARATOR +
                appId +
                host();
    }

    private String host() {
        return host == null
                ? NULL_HOST
                : (SEPARATOR + host.getProtocol() + SEPARATOR + host.getIp() + SEPARATOR + host.getPort());
    }

    public String getAppId() {
        return appId;
    }

    public Mode getMode() {
        return mode;
    }

    public String getId() {
        return id;
    }

    public String getRemotePeerId() {
        return rpId;
    }

    public Host getHost() {
        return host;
    }

    public void setHost(Host host) {
        this.host = host;
    }

    public State getState() {
        return state;
    }

    public void setState(State state) {
        this.state = state;
    }

    public String getPeerManagerId() {
        return pmId;
    }

    public String getRemotePeerManagerId() {
        return rpmId;
    }

    public boolean isRouter() {
        return rpId == null;
    }
}
