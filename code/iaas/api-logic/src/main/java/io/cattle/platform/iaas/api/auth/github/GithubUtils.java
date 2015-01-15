package io.cattle.platform.iaas.api.auth.github;

import io.cattle.platform.token.TokenException;
import io.cattle.platform.token.TokenService;

import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;

public class GithubUtils {

    private TokenService tokenService;

    public String validateAndFetchGithubToken(String jwt) {
        if(StringUtils.isEmpty(jwt)) {
            return null;
        }
        String toParse = null;
        String[] tokenArr = jwt.split("\\s+");
        if (tokenArr.length == 2) {
            if (!StringUtils.equalsIgnoreCase("bearer", StringUtils.trim(tokenArr[0]))) {
                return null;
            }
            toParse = tokenArr[1];
        } else if (tokenArr.length == 1) {
            toParse = tokenArr[0];
        } else {
            return null;
        }
        Map<String, Object> jsonData;
        try {
            jsonData = tokenService.getJsonPayload(toParse, true);
        } catch (TokenException e) { // in case of invalid token
            return null;
        }
        return (String) jsonData.get("access_token");
    }
    
    public String validateAndFetchAccountIdFromToken(String token) {
        if(StringUtils.isEmpty(token)) {
            return null;
        }
        String toParse = null;
        String[] tokenArr = token.split("\\s+");
        if (tokenArr.length == 2) {
            if (!StringUtils.equalsIgnoreCase("bearer", StringUtils.trim(tokenArr[0]))) {
                return null;
            }
            toParse = tokenArr[1];
        } else if (tokenArr.length == 1) {
            toParse = tokenArr[0];
        } else {
            return null;
        }
        Map<String, Object> jsonData;
        try {
            jsonData = tokenService.getJsonPayload(toParse, true);
        } catch (TokenException e) { // in case of invalid token
            return null;
        }
        return (String) jsonData.get("account_id");
    }

    @SuppressWarnings("unchecked")
    public List<String> validateAndFetchAccesibleIdsFromToken(String token) {
        if(StringUtils.isEmpty(token)) {
            return null;
        }
        String toParse = null;
        String[] tokenArr = token.split("\\s+");
        if (tokenArr.length == 2) {
            if (!StringUtils.equalsIgnoreCase("bearer", StringUtils.trim(tokenArr[0]))) {
                return null;
            }
            toParse = tokenArr[1];
        } else if (tokenArr.length == 1) {
            toParse = tokenArr[0];
        } else {
            return null;
        }
        Map<String, Object> jsonData;
        try {
            jsonData = tokenService.getJsonPayload(toParse, true);
        } catch (TokenException e) { // in case of invalid token
            return null;
        }
        return (List<String>) jsonData.get("accessible_ids");
    }
    
    @SuppressWarnings("unchecked")
    public List<String> validateAndFetchOrgIdsFromToken(String token) {
        if(StringUtils.isEmpty(token)) {
            return null;
        }
        String toParse = null;
        String[] tokenArr = token.split("\\s+");
        if (tokenArr.length == 2) {
            if (!StringUtils.equalsIgnoreCase("bearer", StringUtils.trim(tokenArr[0]))) {
                return null;
            }
            toParse = tokenArr[1];
        } else if (tokenArr.length == 1) {
            toParse = tokenArr[0];
        } else {
            return null;
        }
        Map<String, Object> jsonData;
        try {
            jsonData = tokenService.getJsonPayload(toParse, true);
        } catch (TokenException e) { // in case of invalid token
            return null;
        }
        return (List<String>) jsonData.get("org_ids");
    }
    
    @SuppressWarnings("unchecked")
    public List<String> validateAndFetchTeamIdsFromToken(String token) {
        if(StringUtils.isEmpty(token)) {
            return null;
        }
        String toParse = null;
        String[] tokenArr = token.split("\\s+");
        if (tokenArr.length == 2) {
            if (!StringUtils.equalsIgnoreCase("bearer", StringUtils.trim(tokenArr[0]))) {
                return null;
            }
            toParse = tokenArr[1];
        } else if (tokenArr.length == 1) {
            toParse = tokenArr[0];
        } else {
            return null;
        }
        Map<String, Object> jsonData;
        try {
            jsonData = tokenService.getJsonPayload(toParse, true);
        } catch (TokenException e) { // in case of invalid token
            return null;
        }
        return (List<String>) jsonData.get("team_ids");
    }
    
    @Inject
    public void setJwtTokenService(TokenService tokenService) {
        this.tokenService = tokenService;
    }

}
