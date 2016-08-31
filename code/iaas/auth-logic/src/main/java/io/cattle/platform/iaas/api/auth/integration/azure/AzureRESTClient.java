package io.cattle.platform.iaas.api.auth.integration.azure;

import io.cattle.platform.api.auth.Identity;
import io.cattle.platform.json.JsonMapper;
import io.github.ibuildthecloud.gdapi.context.ApiContext;
import io.github.ibuildthecloud.gdapi.exception.ClientVisibleException;
import io.github.ibuildthecloud.gdapi.util.ResponseCodes;
import io.cattle.platform.util.type.CollectionUtils;

import java.io.IOException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;

import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpResponse;
import org.apache.http.client.fluent.Request;
import org.apache.http.entity.ContentType;

public class AzureRESTClient extends AzureConfigurable{

    @Inject
    private JsonMapper jsonMapper;
    
    @Inject
    private AzureTokenUtil azureTokenUtil;

    private static final Log logger = LogFactory.getLog(AzureRESTClient.class);

    private Identity getUserIdentity(String azureAccessToken) {
        if (StringUtils.isEmpty(azureAccessToken)) {
            noAccessToken();
        }
        try {
            logger.debug("getUserIdentity for logged in user");
            HttpResponse response = getFromAzure(azureAccessToken, getURL(AzureClientEndpoints.USER, ""));
            if(hasTokenExpired(response)) {
                //refresh token
                refreshAccessToken();
                //retry the request
                azureAccessToken = (String) ApiContext.getContext().getApiRequest().getAttribute(AzureConstants.AZURE_ACCESS_TOKEN);
                response = getFromAzure(azureAccessToken, getURL(AzureClientEndpoints.USER, ""));
            }
            
            int statusCode = response.getStatusLine().getStatusCode();
            if (statusCode >= 300) {
                noAzure(response);
            }
            
            Map<String, Object> jsonData = jsonMapper.readValue(response.getEntity().getContent());
            return jsonToAzureAccountInfo(jsonData).toIdentity(AzureConstants.USER_SCOPE);
        } catch (IOException e) {
            logger.error("Failed to get Azure user account info.", e);
            throw new ClientVisibleException(ResponseCodes.INTERNAL_SERVER_ERROR, AzureConstants.AZURE_CLIENT,
                    "Failed to get Azure user account info.", null);
        } catch(ClientVisibleException ex) {
            logger.error("Failed to get Azure user account info.", ex);
            throw ex;
        } catch(Exception ex) {
            logger.error("Failed to get Azure user account info.", ex);
            throw new RuntimeException(ex);
        }
    }

    private List<Identity> getGroupIdentities(String azureAccessToken) {
        if (StringUtils.isEmpty(azureAccessToken)) {
            noAccessToken();
        }
        try {
            logger.debug("getGroupIdentities for logged in user");
            HttpResponse response = getFromAzure(azureAccessToken, getURL(AzureClientEndpoints.GROUP, ""));
            if(hasTokenExpired(response)) {
                //refresh token
                refreshAccessToken();
                //retry the request
                azureAccessToken = (String) ApiContext.getContext().getApiRequest().getAttribute(AzureConstants.AZURE_ACCESS_TOKEN);
                response = getFromAzure(azureAccessToken, getURL(AzureClientEndpoints.GROUP, ""));
            }
            
            int statusCode = response.getStatusLine().getStatusCode();
            if (statusCode >= 300) {
                noAzure(response);
            }

            Map<String, Object> jsonData = CollectionUtils.toMap(jsonMapper.readValue(response.getEntity().getContent(), Map.class));
            List<AzureAccountInfo> searchResponseList = parseSearchResponseList(jsonData);

            List<Identity> groupIdentities = new ArrayList<Identity>();

            if(searchResponseList != null && !searchResponseList.isEmpty()){
                for(AzureAccountInfo userOrGroup : searchResponseList){
                    groupIdentities.add(userOrGroup.toIdentity(AzureConstants.GROUP_SCOPE));
                }
            }

            return groupIdentities;
        } catch (IOException e) {
            logger.error("Failed to get Azure group memberships.", e);
            throw new ClientVisibleException(ResponseCodes.INTERNAL_SERVER_ERROR, AzureConstants.AZURE_CLIENT,
                    "Failed to get Azure group memberships.", null);
        } catch(ClientVisibleException ex) {
            logger.error("Failed to get Azure group memberships.", ex);
            throw ex;
        } catch(Exception ex) {
            logger.error("Failed to get Azure group memberships.", ex);
            throw new RuntimeException(ex);
        }
    }


    public String getAccessToken(String code) {
        if (!isConfigured()) {
            throw new ClientVisibleException(ResponseCodes.INTERNAL_SERVER_ERROR, AzureConstants.CONFIG, "Azure Client and Tenant Id not configured", null);
        }
        
        logger.debug("getAccessToken from Azure");
    
        String[] split = code.split(":", 2);
        if (split.length != 2) {
            throw new ClientVisibleException(ResponseCodes.BAD_REQUEST, "MalformedCode");
        }
        String username = split[0];
        String domain = AzureConstants.AZURE_DOMAIN.get();
        //check if the username has a domain, if not then append the domain.
        if(domain != null && domain != "" && !username.endsWith("@"+domain)) {
            username = username + "@"+ domain;
        }
        
        //testuser1%40ZTAD.onmicrosoft.com&password=testpwd1%21";

        String accessToken, refreshToken;
        try{   
            StringBuilder body = new StringBuilder("scope=openid&grant_type=password&resource=https%3A%2F%2Fgraph.windows.net");
            body.append("&client_id=");
            body.append(URLEncoder.encode(AzureConstants.AZURE_CLIENT_ID.get(), "UTF-8"));
            body.append("&username=");
            body.append(URLEncoder.encode(username, "UTF-8"));
            body.append("&password=");
            body.append(URLEncoder.encode(split[1], "UTF-8")); 
            
            HttpResponse response = Request.Post(AzureConstants.AUTHORITY)
                    .addHeader(AzureConstants.ACCEPT, AzureConstants.APPLICATION_FORM_URL_ENCODED)
                    .bodyString(body.toString(), ContentType.APPLICATION_FORM_URLENCODED).execute().returnResponse();
            int statusCode = response.getStatusLine().getStatusCode();
            if(statusCode >= 300) {
                noAzure(response);
            }

            Map<String, Object> jsonData = jsonMapper.readValue(response.getEntity().getContent());
            accessToken = ObjectUtils.toString(jsonData.get("access_token"));
            refreshToken = ObjectUtils.toString(jsonData.get("refresh_token"));
            
            ApiContext.getContext().getApiRequest().setAttribute(AzureConstants.AZURE_ACCESS_TOKEN, accessToken);
            ApiContext.getContext().getApiRequest().setAttribute(AzureConstants.AZURE_REFRESH_TOKEN, refreshToken);

        } catch(ClientVisibleException ex) {
            throw ex;
        } catch(Exception ex) {
            throw new RuntimeException(ex);
        }

        return accessToken;

    }
    
    
    public void refreshAccessToken() {
        if (!isConfigured()) {
            throw new ClientVisibleException(ResponseCodes.INTERNAL_SERVER_ERROR, AzureConstants.CONFIG, "Azure Client and Tenant Id not configured", null);
        }
        
        logger.debug("Azure accessToken expired, refreshing the token");
        
        String azureAccessToken = (String) ApiContext.getContext().getApiRequest().getAttribute(AzureConstants.AZURE_ACCESS_TOKEN);
        if (StringUtils.isEmpty(azureAccessToken)) {
            noAccessToken();
        }
        
        String azureRefreshToken = (String) ApiContext.getContext().getApiRequest().getAttribute(AzureConstants.AZURE_REFRESH_TOKEN);
        if (StringUtils.isEmpty(azureRefreshToken)) {
            noRefreshToken();
        }
        try{
            //post to the microsoft azure authority
            HttpResponse response = Request.Post(AzureConstants.AUTHORITY)
                    .addHeader(AzureConstants.ACCEPT, AzureConstants.APPLICATION_FORM_URL_ENCODED)
                    .bodyString("grant_type=refresh_token&refresh_token="+azureRefreshToken, ContentType.APPLICATION_FORM_URLENCODED)
                    .execute().returnResponse();
            int statusCode = response.getStatusLine().getStatusCode();
            if(statusCode >= 300) {
                noAzure(response);
            }
            
            Map<String, Object> jsonData = jsonMapper.readValue(response.getEntity().getContent());
            
            String newAccessToken = ObjectUtils.toString(jsonData.get("access_token"));
            String newRefreshToken = ObjectUtils.toString(jsonData.get("refresh_token"));
            ApiContext.getContext().getApiRequest().setAttribute(AzureConstants.AZURE_ACCESS_TOKEN, newAccessToken);
            ApiContext.getContext().getApiRequest().setAttribute(AzureConstants.AZURE_REFRESH_TOKEN, newRefreshToken);
            logger.debug("Storing the new Azure access token to the Account");
            azureTokenUtil.refreshAccessToken();
    
        } catch(ClientVisibleException ex) {
            logger.error("Failed to refreshAccessToken for Azure user.", ex);
            throw ex;
        } catch(Exception ex) {
            logger.error("Failed to refreshAccessToken for Azure user.", ex);
            throw new RuntimeException(ex);
        }

    }

    public List<AzureAccountInfo> parseSearchResponseList(Map<String, Object> jsonData) {
        List<?> azureValueList = CollectionUtils.toList(jsonData.get("value"));
        List<AzureAccountInfo> azureUserOrGroupList = new ArrayList<AzureAccountInfo>();
        if (azureValueList != null && !azureValueList.isEmpty())
        {
            for(Object azureValue : azureValueList) {
                Map<String, Object> result = CollectionUtils.toMap(azureValue);
                String objectType = ObjectUtils.toString(result.get("objectType"));
                if(objectType != null && (objectType.equalsIgnoreCase("User") || objectType.equalsIgnoreCase("Group"))) {
                    AzureAccountInfo userOrGroupInfo = jsonToAzureAccountInfo(result);
                    azureUserOrGroupList.add(userOrGroupInfo);
                }
            }
        }
        return azureUserOrGroupList;
    }

    public AzureAccountInfo jsonToAzureAccountInfo(Map<String, Object> jsonData) {
        String objectId = ObjectUtils.toString(jsonData.get("objectId"));
        String objectType = ObjectUtils.toString(jsonData.get("objectType"));
        String userPrincipalName = ObjectUtils.toString(jsonData.get("userPrincipalName"));
        String accountName = ObjectUtils.toString(jsonData.get("mailNickname"));

        if("Group".equalsIgnoreCase(objectType)) {
            accountName = ObjectUtils.toString(jsonData.get("displayName"));
        }

        String name = ObjectUtils.toString(jsonData.get("displayName"));
        if (StringUtils.isBlank(name)) {
            name = accountName;
        }
        //String profilePicture = ObjectUtils.toString(jsonData.get(GithubConstants.PROFILE_PICTURE));
        return new AzureAccountInfo(objectId, accountName, null, userPrincipalName, name);
    }

    public boolean hasTokenExpired(HttpResponse response) throws IOException {
        int statusCode = response.getStatusLine().getStatusCode();
        if(statusCode == 401) {
            //if token expired, refresh token.
            Map<String, Object> jsonData = jsonMapper.readValue(response.getEntity().getContent());
            Map<String, Object> azureError = CollectionUtils.toMap(jsonData.get("odata.error"));
            String azureCode = ObjectUtils.toString(azureError.get("code"));
            if("Authentication_ExpiredToken".equalsIgnoreCase(azureCode)) {
                return true;
            }
         }
        return false;
    }

    public HttpResponse getFromAzure(String azureAccessToken, String url) throws IOException {

        HttpResponse response = Request.Get(url).addHeader(AzureConstants.AUTHORIZATION, "Bearer " +
                "" + azureAccessToken).addHeader(AzureConstants.ACCEPT, AzureConstants.APPLICATION_JSON).execute().returnResponse();
        
        logger.debug("Response from Azure API: "+ response.getStatusLine());

        return response;
    }    

    private void noAccessToken() {
        throw new ClientVisibleException(ResponseCodes.INTERNAL_SERVER_ERROR,
                "AzureAccessToken", "No Azure Access token", null);
    }
    
    private void noRefreshToken() {
        throw new ClientVisibleException(ResponseCodes.INTERNAL_SERVER_ERROR,
                "AzureRefreshToken", "No Azure Refresh token", null);
    }
    
    public void noAzure(HttpResponse response) {
        int statusCode = response.getStatusLine().getStatusCode();

        try {
            Map<String, Object> jsonData = jsonMapper.readValue(response.getEntity().getContent());
            String azureError = (String)jsonData.get("error");
            String azureErrorDesc = (String)jsonData.get("error_description");
            if("invalid_grant".equalsIgnoreCase(azureError) || "unauthorized_client".equalsIgnoreCase(azureError)) {
                throw new ClientVisibleException(ResponseCodes.UNAUTHORIZED);
            }
            logger.error("Error received from Azure: " + azureError);
            logger.error("Error Description received from Azure: " + azureErrorDesc);
            throw new ClientVisibleException(ResponseCodes.SERVICE_UNAVAILABLE, AzureConstants.AZURE_ERROR,
                    "Error from Azure: " + Integer.toString(statusCode), "Details: " +azureError + ", Description: "+azureErrorDesc);
        } catch(ClientVisibleException ex) {
            logger.error("Got error from Azure.", ex);
            throw ex;
        } catch(Exception ex) {
            throw new ClientVisibleException(ResponseCodes.SERVICE_UNAVAILABLE, AzureConstants.AZURE_ERROR,
                    "Error Response from Azure", "Status code from Azure: " + Integer.toString(statusCode));
        } 
    }

    public String getURL(AzureClientEndpoints val, String objectId) {
        String apiEndpoint = AzureConstants.GRAPH_API_ENDPOINT;
        String tenantId = AzureConstants.AZURE_TENANT_ID.get();

        String toReturn;
        switch (val) {
            case USERS:
                toReturn = apiEndpoint + tenantId + "/users/" + objectId;
                break;
            case GROUPS:
                toReturn = apiEndpoint + tenantId  + "/groups/" + objectId;
                break;
            case USER:
                toReturn = apiEndpoint + "me";
                break;
            case GROUP:
                toReturn = apiEndpoint + "me/memberOf";
                break;
            default:
                throw new ClientVisibleException(ResponseCodes.INTERNAL_SERVER_ERROR,
                        "AzureClient", "Attempted to get invalid Api endpoint.", null);
        }
        return toReturn + AzureConstants.GRAPH_API_VERSION;
    }    
    

    public AzureAccountInfo getAzureUserByName(String username) {
        String azureAccessToken = (String) ApiContext.getContext().getApiRequest().getAttribute(AzureConstants.AZURE_ACCESS_TOKEN);
        if (StringUtils.isEmpty(azureAccessToken)) {
            noAccessToken();
        }
        logger.debug("getAzureUserByName: "+ username);
        try {
            if (StringUtils.isEmpty(username)) {
                throw new ClientVisibleException(ResponseCodes.INTERNAL_SERVER_ERROR,
                        "getAzureUser", "No azure username specified.", null);
            }
            String domain = AzureConstants.AZURE_DOMAIN.get();
            //check if the username has a domain, if not then append the domain.
            if(domain != null && domain != "" && !username.endsWith("@"+domain)) {
                username = username + "@"+ domain;
            }
            
            username = URLEncoder.encode(username, "UTF-8");
            
            String filter = "$filter=userPrincipalName%20eq%20'" + username + "'";
            //String filter = "$filter=startswith(userPrincipalName,'" + username+ "')";

            
            HttpResponse response = getFromAzure(azureAccessToken, getURL(AzureClientEndpoints.USERS, "") + "&"+ filter);
            if(hasTokenExpired(response)) {
                //refresh token
                refreshAccessToken();
                //retry the request
                azureAccessToken = (String) ApiContext.getContext().getApiRequest().getAttribute(AzureConstants.AZURE_ACCESS_TOKEN);
                response = getFromAzure(azureAccessToken, getURL(AzureClientEndpoints.USERS, "") + "&"+ filter);
            }
            
            int statusCode = response.getStatusLine().getStatusCode();
            if (statusCode >= 300) {
                noAzure(response);
            }

            
            Map<String, Object> jsonData = CollectionUtils.toMap(jsonMapper.readValue(response.getEntity().getContent(), Map.class));
            List<AzureAccountInfo> searchResponseList = parseSearchResponseList(jsonData);
            if(searchResponseList != null && !searchResponseList.isEmpty()) {
                return searchResponseList.get(0);
            }
            return null;
        } catch (IOException e) {
            logger.error(e);
            throw new ClientVisibleException(ResponseCodes.SERVICE_UNAVAILABLE, "AzureUnavailable", "Could not retrieve User by name from Azure", null);
        } catch(ClientVisibleException ex) {
            logger.error("Failed to get Azure user account info by name.", ex);
            throw ex;
        } catch(Exception ex) {
            logger.error("Failed to get Azure user account info by name.", ex);
            throw new RuntimeException(ex);
        }

    }


    public AzureAccountInfo getAzureGroupByName(String org) {
        String azureAccessToken = (String) ApiContext.getContext().getApiRequest().getAttribute(AzureConstants.AZURE_ACCESS_TOKEN);
        if (StringUtils.isEmpty(azureAccessToken)) {
            noAccessToken();
        }
        logger.debug("getAzureGroupByName: "+ org);
        try {
            if (StringUtils.isEmpty(org)) {
                throw new ClientVisibleException(ResponseCodes.INTERNAL_SERVER_ERROR,
                        "noAzureGroupName", "No group name specified when retrieving from Azure.", null);
            }
            org = URLEncoder.encode(org, "UTF-8");
            String filter = "$filter=displayName%20eq%20'" + org + "'";
            //String filter = "$filter=startswith(displayName,'" + org+ "')";
            
            HttpResponse response = getFromAzure(azureAccessToken, getURL(AzureClientEndpoints.GROUPS, "") + "&"+ filter);
            if(hasTokenExpired(response)) {
                //refresh token
                refreshAccessToken();
                //retry the request
                azureAccessToken = (String) ApiContext.getContext().getApiRequest().getAttribute(AzureConstants.AZURE_ACCESS_TOKEN);
                response = getFromAzure(azureAccessToken, getURL(AzureClientEndpoints.GROUPS, "") + "&"+ filter);
            }
            
            int statusCode = response.getStatusLine().getStatusCode();
            if (statusCode >= 300) {
                noAzure(response);
            }  

            Map<String, Object> jsonData = CollectionUtils.toMap(jsonMapper.readValue(response.getEntity().getContent
                    (), Map.class));
            List<AzureAccountInfo> searchResponseList = parseSearchResponseList(jsonData);
            if(searchResponseList != null && !searchResponseList.isEmpty()) {
                return searchResponseList.get(0);
            }
            return null;
        } catch (IOException e) {
            logger.error(e);
            throw new ClientVisibleException(ResponseCodes.SERVICE_UNAVAILABLE, "AzureUnavailable", "Could not retrieve Group by name from Azure", null);
        } catch(ClientVisibleException ex) {
            logger.error("Failed to get Azure group info by name.", ex);
            throw ex;
        } catch(Exception ex) {
            logger.error("Failed to get Azure group info by name.", ex);
            throw new RuntimeException(ex);
        }
    }
    
    
     public AzureAccountInfo getUserById(String id) {
        String azureAccessToken = (String) ApiContext.getContext().getApiRequest().getAttribute(AzureConstants.AZURE_ACCESS_TOKEN);
        if (StringUtils.isEmpty(azureAccessToken)) {
            noAccessToken();
        }
        logger.debug("getUserById: "+ id);
        try {
            HttpResponse response = getFromAzure(azureAccessToken, getURL(AzureClientEndpoints.USERS, id));
            if(hasTokenExpired(response)) {
                //refresh token
                refreshAccessToken();
                //retry the request
                azureAccessToken = (String) ApiContext.getContext().getApiRequest().getAttribute(AzureConstants.AZURE_ACCESS_TOKEN);
                response = getFromAzure(azureAccessToken, getURL(AzureClientEndpoints.USERS, id));
            }
            
            int statusCode = response.getStatusLine().getStatusCode();
            if (statusCode >= 300) {
                noAzure(response);
            }  

            Map<String, Object> jsonData = CollectionUtils.toMap(jsonMapper.readValue(response.getEntity().getContent(), Map.class));
            return jsonToAzureAccountInfo(jsonData);
        } catch (IOException e) {
            throw new ClientVisibleException(ResponseCodes.SERVICE_UNAVAILABLE, "AzureUnavailable", "Could not retrieve User by Id from Azure", null);
        } catch(ClientVisibleException ex) {
            logger.error("Failed to get Azure user account info by Id.", ex);
            throw ex;
        } catch(Exception ex) {
            logger.error("Failed to get Azure user account info by Id.", ex);
            throw new RuntimeException(ex);
        }
    }
     
     public AzureAccountInfo getGroupById(String id) {
         String azureAccessToken = (String) ApiContext.getContext().getApiRequest().getAttribute(AzureConstants.AZURE_ACCESS_TOKEN);
         if (StringUtils.isEmpty(azureAccessToken)) {
             noAccessToken();
         }
         logger.debug("getGroupById: "+ id);
         try {
             HttpResponse response = getFromAzure(azureAccessToken, getURL(AzureClientEndpoints.GROUPS, id));
             if(hasTokenExpired(response)) {
                 //refresh token
                 refreshAccessToken();
                 //retry the request
                 azureAccessToken = (String) ApiContext.getContext().getApiRequest().getAttribute(AzureConstants.AZURE_ACCESS_TOKEN);
                 response = getFromAzure(azureAccessToken, getURL(AzureClientEndpoints.GROUPS, id));
             }
             
             int statusCode = response.getStatusLine().getStatusCode();
             if (statusCode >= 300) {
                 noAzure(response);
             } 
             Map<String, Object> jsonData = CollectionUtils.toMap(jsonMapper.readValue(response.getEntity().getContent(), Map.class));
             return jsonToAzureAccountInfo(jsonData);
         } catch (IOException e) {
             throw new ClientVisibleException(ResponseCodes.SERVICE_UNAVAILABLE, "AzureUnavailable", "Could not retrieve Group by Id from Azure", null);
         } catch(ClientVisibleException ex) {
             logger.error("Failed to get Azure group info by id.", ex);
             throw ex;
         } catch(Exception ex) {
             logger.error("Failed to get Azure group info by id.", ex);
             throw new RuntimeException(ex);
         }
     }

 
    public Set<Identity> getIdentities(String accessToken) {
        Set<Identity> identities = new HashSet<>();
        identities.add(getUserIdentity(accessToken));
        identities.addAll(getGroupIdentities(accessToken));

        return identities;
    }

    @Override
    public String getName() {
        return AzureConstants.AZURE_CLIENT;
    }
    
}
