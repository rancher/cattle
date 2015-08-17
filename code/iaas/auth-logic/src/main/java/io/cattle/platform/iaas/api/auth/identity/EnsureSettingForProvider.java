package io.cattle.platform.iaas.api.auth.identity;

import io.cattle.platform.api.auth.Identity;
import io.cattle.platform.archaius.util.ArchaiusUtil;
import io.cattle.platform.iaas.api.auth.SecurityConstants;
import io.cattle.platform.iaas.api.auth.SettingsUtils;
import io.cattle.platform.iaas.api.auth.integration.github.GithubConstants;
import io.cattle.platform.iaas.api.auth.integration.github.GithubUtils;
import io.cattle.platform.util.type.InitializationTask;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class EnsureSettingForProvider implements InitializationTask {

    private static final Log logger = LogFactory.getLog(EnsureSettingForProvider.class);

    @Inject
    SettingsUtils settingsUtils;

    @Inject
    GithubUtils githubUtils;

    @Override
    public void start(){
        if (SecurityConstants.SECURITY.get() &&
                StringUtils.isNotBlank(GithubConstants.GITHUB_CLIENT_SECRET.get()) &&
                StringUtils.isNotBlank(GithubConstants.GITHUB_CLIENT_ID.get())){
            if (StringUtils.isBlank(SecurityConstants.AUTH_PROVIDER.get())) {
                settingsUtils.changeSetting(SecurityConstants.AUTH_PROVIDER_SETTING, GithubConstants.CONFIG);
                logger.info("Upgrade detected. Adding setting for :" + SecurityConstants
                        .AUTH_PROVIDER_SETTING);
                String userSetting = "api.auth.github.allowed.users";
                String orgSetting = "api.auth.github.allowed.orgs";
                String allowedUsers = ArchaiusUtil.getString(userSetting).get();
                String allowedOrgs = ArchaiusUtil.getString(orgSetting).get();
                if ("restricted".equalsIgnoreCase(GithubConstants.ACCESS_MODE.get()) &&
                        StringUtils.isBlank(GithubConstants.GITHUB_ALLOWED_IDENTITIES.get()) &&
                        (StringUtils.isNotBlank(allowedOrgs) || StringUtils.isNotBlank(allowedUsers))
                        ){
                    List<Identity> allowedIdentities = new ArrayList<>();
                    List<String> userIds = githubUtils.fromCommaSeparatedString(allowedUsers);
                    for (String userId: userIds){
                        String[] split = userId.split(":");
                        if (split.length == 2) {
                            Identity identity = new Identity(GithubConstants.USER_SCOPE, split[1]);
                            allowedIdentities.add(identity);
                        }
                    }
                    List<String> orgIds = githubUtils.fromCommaSeparatedString(allowedOrgs);
                    for (String orgId: orgIds){
                        String[] split = orgId.split(":");
                        if (split.length == 2){
                            Identity identity = new Identity(GithubConstants.ORG_SCOPE, split[1]);
                            allowedIdentities.add(identity);
                        }
                    }
                    Iterator<Identity> identityIterator = allowedIdentities.iterator();
                    StringBuilder sb= new StringBuilder();
                    while (identityIterator.hasNext()){
                        sb.append(identityIterator.next().getId().trim());
                        if (identityIterator.hasNext()) {
                            sb.append(',');
                        }
                    }
                    settingsUtils.changeSetting(GithubConstants.ALLOWED_IDENTITIES_SETTING, sb.toString());
                    logger.info("Adding new setting for :" + GithubConstants.ALLOWED_IDENTITIES_SETTING);
                } else if ("unrestricted".equalsIgnoreCase(GithubConstants.ACCESS_MODE.get())) {
                    settingsUtils.changeSetting(userSetting, null);
                    settingsUtils.changeSetting(orgSetting, null);
                }
            }
        } else if (StringUtils.isBlank(SecurityConstants.AUTH_PROVIDER.get())) {
            settingsUtils.changeSetting(SecurityConstants.AUTH_PROVIDER_SETTING, SecurityConstants.NO_PROVIDER);
        }
    }

    @Override
    public void stop() {
    }
}
