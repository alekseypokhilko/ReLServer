package net.relserver.core.port;

public class PortPair {
    private final UdpPort p2pPort;
    private final UdpPort targetPort;

    public PortPair(UdpPort p2pPort, UdpPort targetPort) {
        this.p2pPort = p2pPort;
        this.targetPort = targetPort;
    }

    public UdpPort getP2pPort() {
        return p2pPort;
    }

    public UdpPort getTargetPort() {
        return targetPort;
    }
}
