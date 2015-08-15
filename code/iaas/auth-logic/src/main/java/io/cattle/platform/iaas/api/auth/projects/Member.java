package io.cattle.platform.iaas.api.auth.projects;

import io.cattle.platform.api.auth.Identity;
import io.cattle.platform.core.model.ProjectMember;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;

public class Member  {

    private final String profilePicture;
    private final String profileUrl;
    private final String login;
    private final String externalId;
    private final String externalIdType;
    private final String role;
    private final String name;
    private final String projectId;

    public Member(ProjectMember projectMember, String projectId) {
        this.externalId = projectMember.getExternalId();
        this.externalIdType = projectMember.getExternalIdType();
        this.role = projectMember.getRole();
        this.name = projectMember.getName();
        profilePicture = null;
        profileUrl = null;
        login = null;
        this.projectId = projectId;
    }

    public Member(Identity identity, String role) {
        this.externalId = identity.getExternalId();
        this.externalIdType = identity.getExternalIdType();
        this.role = role;
        this.name = identity.getName();
        this.login = identity.getLogin();
        this.profileUrl = identity.getProfileUrl();
        this.profilePicture = identity.getProfilePicture();
        this.projectId = null;
    }

    public Member(Identity identity, String role, String projectId) {
        this.externalId = identity.getExternalId();
        this.externalIdType = identity.getExternalIdType();
        this.role = role;
        this.name = identity.getName();
        this.login = identity.getLogin();
        this.profileUrl = identity.getProfileUrl();
        this.profilePicture = identity.getProfilePicture();
        this.projectId = projectId;
    }

    public String getExternalId() {
        return externalId;
    }

    public String getExternalIdType() {
        return externalIdType;
    }

    public String getRole() {
        return role;
    }

    public String getName() {
        return name;
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        Member member = (Member) o;

        return new EqualsBuilder()
                .append(externalId, member.externalId)
                .append(externalIdType, member.externalIdType)
                .append(role, member.role)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(15, 39)
                .append(externalId)
                .append(externalIdType)
                .append(role)
                .toHashCode();
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append("identity", externalId)
                .append("externalIdType", externalIdType)
                .append("role", role)
                .append("name", name)
                .toString();
    }

    public String getProfilePicture() {
        return profilePicture;
    }

    public String getProfileUrl() {
        return profileUrl;
    }

    public String getLogin() {
        return login;
    }

    public String getProjectId() {
        return projectId;
    }
}
