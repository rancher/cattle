package io.cattle.platform.iaas.api.auth.integration.github;

import io.cattle.platform.iaas.api.auth.TokenUtils;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

public class GithubUtils extends TokenUtils {

    @Override
    protected String accessMode() {
        return GithubConstants.ACCESS_MODE.get();
    }

    @Override
    protected String accessToken() {
        return GithubConstants.GITHUB_ACCESS_TOKEN;
    }

    @Override
    protected boolean isWhitelisted(List<String> idList) {
        if (idList == null || idList.isEmpty()) {
            return false;
        }
        List<String> whitelistedValues = fromCommaSeparatedString(GithubConstants.GITHUB_ALLOWED_IDENTITIES.get());

        for (String id : idList) {
            for (String whiteId: whitelistedValues){
                if (StringUtils.equals(id, whiteId)){
                    return true;
                }
            }
        }
        return false;
    }

    public List<String> fromCommaSeparatedString(String string) {
        if (StringUtils.isEmpty(string)) {
            return new ArrayList<>();
        }
        List<String> strings = new ArrayList<>();
        String[] splitted = string.split(",");
        for (String aSplitted : splitted) {
            String element = aSplitted.trim();
            strings.add(element);
        }
        return strings;
    }

    @Override
    protected String getAccountType() {
        return GithubConstants.USER_SCOPE;
    }

    @Override
    protected String tokenType() {
        return GithubConstants.GITHUB_JWT;
    }
}
