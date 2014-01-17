package io.github.ibuildthecloud.dstack.iaas.api.auth;

import io.github.ibuildthecloud.dstack.api.auth.impl.ArchaiusPolicyOptions;
import io.github.ibuildthecloud.dstack.core.model.Account;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class AchaiusPolicyOptionsFactory {

    Map<String,ArchaiusPolicyOptions> options = new ConcurrentHashMap<String, ArchaiusPolicyOptions>();

    public ArchaiusPolicyOptions getOptions(Account account) {
        String kind = account.getKind();

        ArchaiusPolicyOptions opts = options.get(kind);
        if ( opts != null ) {
            return opts;
        }

        opts = new ArchaiusPolicyOptions(kind);
        options.put(kind, opts);

        return opts;
    }

}
