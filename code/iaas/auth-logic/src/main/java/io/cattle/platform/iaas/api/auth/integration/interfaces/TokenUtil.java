package io.cattle.platform.iaas.api.auth.integration.interfaces;

import io.cattle.platform.api.auth.Identity;
import io.cattle.platform.core.model.Account;
import io.cattle.platform.iaas.api.auth.identity.Token;
import io.cattle.platform.object.ObjectManager;
import io.cattle.platform.util.type.Named;

import java.util.List;
import java.util.Set;

public interface TokenUtil extends Named, Configurable {
    Account getAccountFromJWT();

    String tokenType();

    String getJWT();

    Set<Identity> getIdentities();

    boolean findAndSetJWT();

    boolean isAllowed(List<String> idList, Set<Identity> identities);

    String getAccessToken();

    List<String> identitiesToIdList(Set<Identity> identities);

    Account getOrCreateAccount(Identity user, Set<Identity> identities, Account account);

    Token createToken(Set<Identity> identities, Account account);

    String userType();

    boolean createAccount();

    Identity getUser(Set<Identity> identities);

    ObjectManager getObjectManager();
}
