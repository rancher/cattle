package io.cattle.platform.iaas.api.auth.github;

import io.cattle.platform.token.TokenException;
import io.cattle.platform.token.TokenService;
import io.cattle.platform.util.type.CollectionUtils;

import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.apache.commons.lang.ObjectUtils;
import org.apache.commons.lang3.StringUtils;

public class GithubUtils {

    private TokenService tokenService;

    public String validateAndFetchGithubToken(String token) {
        Map<String, Object> jsonData = getJsonData(token);
        if (null == jsonData) {
            return null;
        }
        return ObjectUtils.toString(jsonData.get("access_token"), null);
    }

    public String validateAndFetchAccountIdFromToken(String token) {
        Map<String, Object> jsonData = getJsonData(token);
        if (null == jsonData) {
            return null;
        }
        return ObjectUtils.toString(jsonData.get("account_id"), null);
    }

    @SuppressWarnings("unchecked")
    public List<String> validateAndFetchOrgIdsFromToken(String token) {
        Map<String, Object> jsonData = getJsonData(token);
        if (null == jsonData) {
            return null;
        }
        return (List<String>) CollectionUtils.toList(jsonData.get("org_ids"));
    }

    @SuppressWarnings("unchecked")
    public List<String> validateAndFetchTeamIdsFromToken(String token) {
        Map<String, Object> jsonData = getJsonData(token);
        if (null == jsonData) {
            return null;
        }
        return (List<String>) CollectionUtils.toList(jsonData.get("team_ids"));
    }

    @SuppressWarnings("unchecked")
    public AccesibleIds validateAndFetchAccesibleIdsFromToken(String token) {
        Map<String, Object> jsonData = getJsonData(token);
        if (null == jsonData) {
            return null;
        }
        List<String> teamIds = (List<String>) CollectionUtils.toList(jsonData.get("team_ids"));
        List<String> orgIds = (List<String>) CollectionUtils.toList(jsonData.get("org_ids"));
        String accountId = ObjectUtils.toString(jsonData.get("account_id"), null);
        return new AccesibleIds(accountId, teamIds, orgIds);
    }

    public ReverseMappings validateAndFetchReverseMappings(String token) {
        Map<String, Object> jsonData = getJsonData(token);
        if (null == jsonData) {
            return null;
        }
        Map<String, String> teamsMap = CollectionUtils.toMap(jsonData.get("teams_reverse_mapping"));
        Map<String, String> orgsMap = CollectionUtils.toMap(jsonData.get("orgs_reverse_mapping"));
        String userName = ObjectUtils.toString(jsonData.get("username"), null);
        return new ReverseMappings(teamsMap, userName, orgsMap);
    }

    private Map<String, Object> getJsonData(String jwt) {
        if (StringUtils.isEmpty(jwt)) {
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
        return jsonData;
    }

    @Inject
    public void setJwtTokenService(TokenService tokenService) {
        this.tokenService = tokenService;
    }

    public class AccesibleIds {
        String userId;
        List<String> teamIds;
        List<String> orgIds;

        public AccesibleIds(String userId, List<String> teamIds, List<String> orgIds) {
            this.userId = userId;
            this.teamIds = teamIds;
            this.orgIds = orgIds;
        }

        public void setUserId(String userId) {
            this.userId = userId;
        }

        public String getUserId() {
            return userId;
        }

        public List<String> getTeamIds() {
            return teamIds;
        }

        public void setTeamIds(List<String> teamIds) {
            this.teamIds = teamIds;
        }

        public List<String> getOrgIds() {
            return orgIds;
        }

        public void setOrgIds(List<String> orgIds) {
            this.orgIds = orgIds;
        }

    }

    public class ReverseMappings {
        Map<String, String> teamsMap;
        String username;
        Map<String, String> orgMap;

        public ReverseMappings(Map<String, String> teamsMap, String username, Map<String, String> orgMap) {
            this.teamsMap = teamsMap;
            this.username = username;
            this.orgMap = orgMap;
        }

        public Map<String, String> getTeamsMap() {
            return teamsMap;
        }

        public void setTeamsMap(Map<String, String> teamsMap) {
            this.teamsMap = teamsMap;
        }

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public Map<String, String> getOrgMap() {
            return orgMap;
        }

        public void setOrgMap(Map<String, String> orgMap) {
            this.orgMap = orgMap;
        }

    }

}
