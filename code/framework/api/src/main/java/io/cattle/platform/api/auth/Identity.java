package io.cattle.platform.api.auth;

import io.github.ibuildthecloud.gdapi.annotation.Field;
import io.github.ibuildthecloud.gdapi.annotation.Type;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;

@Type(name = "identity", pluralName = "identities")
public class Identity {

    private String externalId = null;
    private String profilePicture= null;
    private String name = null;
    private String externalIdType = null;
    private String profileUrl = null;
    private String login = null;
    private String role = null;
    private String projectId = null;

    @Field(required = false, nullable = true)
    public String getName() {
        return name;
    }

    @Field(required = true, nullable = false)
    public String getExternalId() {
        return externalId;
    }

    @Field(required = true, nullable = false)
    public String getExternalIdType() {
        return externalIdType;
    }

    @Field(required = false, nullable = true)
    public String getAll(){
        return null;
    }

    @Field(required = false, nullable = true)
    public String getId() {
        return externalIdType + ':' + externalId;
    }

    @Field(required = false, nullable = true)
    public String getProfilePicture() {
        return profilePicture;
    }

    @Field(required = false, nullable = true)
    public String getProfileUrl() {
        return profileUrl;
    }

    @Field(required = false, nullable = true)
    public String getLogin() {
        return login;
    }

    @Field(required = false, nullable = true)
    public String getRole() {
        return role;
    }

    @Field(required = false, nullable = true)
    public String getProjectId() {
        return projectId;
    }

    public Identity(String externalIdType, String externalId) {
        this(externalIdType, externalId, null, null, null, null);
    }

    public Identity(String externalIdType, String externalId, String name, String profileUrl, String profilePicture, String login) {
        this.externalId = externalId;
        this.name = name;
        this.externalIdType = externalIdType;
        this.profileUrl = profileUrl;
        this.login = login;
        this.profilePicture = profilePicture;
    }

    public Identity(Identity identity, String role, String projectId){
        this.externalId = identity.getExternalId();
        this.name = identity.getName();
        this.externalIdType = identity.getExternalIdType();
        this.profileUrl = identity.getProfileUrl();
        this.login = identity.getLogin();
        this.profilePicture = identity.getProfilePicture();
        this.projectId = projectId;
        this.role = role;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        Identity identity = (Identity) o;

        return new EqualsBuilder()
                .append(externalId, identity.externalId)
                .append(externalIdType, identity.externalIdType)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(19, 43)
                .append(externalId)
                .append(externalIdType)
                .toHashCode();
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append("externalId", externalId)
                .append("profilePicture", profilePicture)
                .append("name", name)
                .append("externalIdType", externalIdType)
                .append("profileUrl", profileUrl)
                .toString();
    }

    public Identity maskExternalId() {
        return new Identity(externalIdType, login, name, profileUrl, profilePicture, login);
    }

    public static Identity fromId(String id) {
        if (StringUtils.isBlank(id)) {
            return null;
        }
        String[] split = id.split(":", 2);
        if (split.length != 2){
            return null;
        }
        String externalIdType = split[0];
        String externalId = split[1];
        return new Identity(externalIdType, externalId);
    }
}
