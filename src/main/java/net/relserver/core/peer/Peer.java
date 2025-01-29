package net.relserver.core.peer;

import net.relserver.core.Utils;

import java.util.Locale;

import static net.relserver.core.Constants.SEPARATOR;

public class Peer {
    private static final String NULL_HOST = SEPARATOR + null + SEPARATOR + null + SEPARATOR + null;

    /**
     * Peer manager id where current peer was created
     */
    private final String peerManagerId;
    /**
     * Peer id
     */
    private final String id;
    private State state;
    private final Mode mode;
    /**
     * Remote peer id for p2p connection
     */
    private final String remotePeerId;
    /**
     * Remote peer manager id for notification
     */
    private final String remotePeerManagerId;
    /**
     * Application id for filtering peers on the hub and creating proxies
     */
    private final String appId;
    private Host host;

    public static Peer of(String fullInfo) {
        String[] fields = fullInfo.split(SEPARATOR);
        return new Peer(
                Utils.valueOrElseNull(fields[0]),
                Utils.valueOrElseNull(fields[1]),
                State.valueOf(fields[2].toUpperCase(Locale.ENGLISH)),
                Mode.valueOf(fields[3].toUpperCase(Locale.ENGLISH)),
                Utils.valueOrElseNull(fields[4]),
                Utils.valueOrElseNull(fields[5]),
                Utils.valueOrElseNull(fields[6]),
                fullInfo.endsWith(NULL_HOST) ? null : new Host(fields[8], Integer.parseInt(fields[9]), Protocol.valueOf(fields[7]))
        );
    }

    public static Peer of(String info, Host host) {
        Peer peer = Peer.of(info);
        peer.setHost(host);
        return peer;
    }

    public static Peer of(String peerManagerId, String id, State state, Mode mode, String remotePeerManagerId, String peerId, String appId, Host host) {
        return new Peer(peerManagerId, id, state, mode, remotePeerManagerId, peerId, appId, host);
    }

    public Peer(String peerManagerId, String id, State state, Mode mode, String remotePeerManagerId, String remotePeerId, String appId, Host host) {
        this.peerManagerId = peerManagerId;
        this.id = id;
        this.state = state;
        this.mode = mode;
        this.remotePeerManagerId = remotePeerManagerId;
        this.remotePeerId = remotePeerId;
        this.appId = appId;
        this.host = host;
    }

    @Override
    public String toString() {
        return peerManagerId + SEPARATOR +
                id + SEPARATOR +
                state + SEPARATOR +
                mode + SEPARATOR +
                remotePeerManagerId + SEPARATOR +
                remotePeerId + SEPARATOR +
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
        return remotePeerId;
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
        return peerManagerId;
    }

    public String getRemotePeerManagerId() {
        return remotePeerManagerId;
    }

    public boolean isRouter() {
        return remotePeerId == null;
    }
}
