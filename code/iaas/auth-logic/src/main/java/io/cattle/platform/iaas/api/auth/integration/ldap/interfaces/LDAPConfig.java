package io.cattle.platform.iaas.api.auth.integration.ldap.interfaces;

public interface LDAPConfig {

    String getServiceAccountUsername();

    String getServiceAccountPassword();

    String getServer();

    int getPort();

    boolean getTls();

    String getDomain();
}
