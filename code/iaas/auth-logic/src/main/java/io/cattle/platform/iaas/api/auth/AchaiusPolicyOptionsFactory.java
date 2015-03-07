package io.cattle.platform.iaas.api.auth;

import io.cattle.platform.api.auth.impl.ArchaiusPolicyOptions;
import io.cattle.platform.core.model.Account;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class AchaiusPolicyOptionsFactory {

    Map<String, ArchaiusPolicyOptions> options = new ConcurrentHashMap<String, ArchaiusPolicyOptions>();

    public ArchaiusPolicyOptions getOptions(Account account) {
        return getOptions(account.getKind());
    }

    public ArchaiusPolicyOptions getOptions(String kind) {
        ArchaiusPolicyOptions opts = options.get(kind);
        if (opts != null) {
            return opts;
        }

        opts = new ArchaiusPolicyOptions(kind);
        options.put(kind, opts);

        return opts;
    }

}
