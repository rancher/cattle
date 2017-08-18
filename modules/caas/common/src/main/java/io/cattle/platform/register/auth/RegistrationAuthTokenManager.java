package io.cattle.platform.register.auth;

import io.cattle.platform.core.model.Account;

public interface RegistrationAuthTokenManager {

    TokenAccount validateToken(String token);

    class TokenAccount {
        public TokenAccount(Account account, Long clusterId) {
            this.account = account;
            this.clusterId = clusterId;
        }

        Account account;
        Long clusterId;

        public Account getAccount() {
            return account;
        }

        public void setAccount(Account account) {
            this.account = account;
        }

        public Long getClusterId() {
            return clusterId;
        }

        public void setClusterId(Long clusterId) {
            this.clusterId = clusterId;
        }
    }
}
