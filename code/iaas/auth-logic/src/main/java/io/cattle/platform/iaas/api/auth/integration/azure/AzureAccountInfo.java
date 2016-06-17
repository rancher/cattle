package io.cattle.platform.iaas.api.auth.integration.azure;

import io.cattle.platform.api.auth.Identity;

public class AzureAccountInfo {
    private final String objectId;
    private final String accountName;    
    private final String userPrincipalName;
    private final String thumbNail;
    private final String displayName;

    public AzureAccountInfo(String objectId, String accountName, String thumbNail, String userPrincipalName, String displayName) {
        this.objectId = objectId;
        this.accountName = accountName;
        this.thumbNail = thumbNail;
        this.userPrincipalName = userPrincipalName;
        this.displayName = displayName;
    }

    public String getObjectId() {
        return objectId;
    }

    public String getAccountName() {
        return accountName;
    }

    @Override
    public String toString() {
        return accountName + ':' + objectId + ':' + thumbNail + ':' + userPrincipalName;

    }

    public String getThumbNail() {
        return thumbNail;
    }


    public Identity toIdentity(String scope) {
        return new Identity(scope, objectId, displayName, null, thumbNail, accountName);
    }

    public String getName() {
        return displayName;
    }
}
