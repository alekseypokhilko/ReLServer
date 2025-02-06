package net.relserver.core.api.model;

import net.relserver.core.Constants;

public class PeerManagerRegistrationRequest {
    private final String pmId;
    private final String appId;

    public PeerManagerRegistrationRequest(String pmId, String appId) {
        this.pmId = pmId;
        this.appId = appId;
    }

    public String getPmId() {
        return pmId;
    }

    public String getAppId() {
        return appId;
    }

    @Override
    public String toString() {
        return Operation.REGISTER_PEER_MANAGER + Constants.SEPARATOR + pmId + Constants.SEPARATOR + appId;
    }
}
