package io.cattle.platform.iaas.api.auth.integration.ldap.interfaces;

public interface LDAPConfig {

    String getAccessMode();

    String getName();

    String getServiceAccountUsername();

    String getServiceAccountPassword();

    String getServer();

    int getPort();

    boolean getTls();

    String getLoginDomain();

    String getDomain();

    String getUserSearchField();

    String getGroupSearchField();

    String getUserLoginField();

    String getUserObjectClass();

    String getUserEnabledAttribute();

    String getUserNameField();

    int getUserDisabledBitMask();

    String getGroupObjectClass();

    String getGroupNameField();

    String getUserMemberAttribute();

    String getGroupMemberMappingAttribute();
}
