package io.cattle.platform.iaas.api.auth.integration.ldap.interfaces;

import io.cattle.platform.api.auth.Identity;

import java.util.List;

public interface LDAPConfig {

    String getAccessMode();

    String getName();

    String getServiceAccountUsername();

    String getServiceAccountPassword();

    String getServer();

    Integer getPort();

    Boolean getTls();

    String getLoginDomain();

    String getDomain();

    String getUserSearchField();

    String getGroupSearchField();

    String getUserLoginField();

    String getUserObjectClass();

    String getUserEnabledAttribute();

    String getUserNameField();

    Integer getUserDisabledBitMask();

    String getGroupObjectClass();

    String getGroupNameField();

    String getUserMemberAttribute();

    String getGroupMemberMappingAttribute();

    long getConnectionTimeout();

    List<Identity> getAllowedIdentities();

    String getGroupDNField();

    String getGroupMemberUserAttribute();

}
