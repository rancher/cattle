package io.cattle.platform.iaas.api.auth.dao;

import io.cattle.platform.api.auth.ExternalId;
import io.cattle.platform.core.model.Account;
import io.cattle.platform.core.model.ProjectMember;
import io.cattle.platform.iaas.api.auth.github.resource.Member;

import java.util.List;
import java.util.Set;

public interface AuthDao {

    Account getAdminAccount();

    Account getAccountById(Long id);

    Account getAccountByKeys(String access, String secretKey);

    Account getAccountByExternalId(String externalId, String exteralType);

    Account getAccountByUuid(String uuid);

    Account createAccount(String name, String kind, String externalId, String externalType);

    Account createProject(String name, String description);

    Account createDefaultProject(Account account);

    void updateAccount(Account account, String name, String kind, String externalId, String externalType);

    List<Account> getAccessibleProjects(Set<ExternalId> externalIdSet, boolean isAdmin, Long usingAccount);

    boolean hasAccessToProject(long projectId , Long usingAccount, boolean isAdmin, Set<ExternalId> externalIdSet);

    boolean isProjectOwner(long projectId , Long usingAccount, boolean isAdmin, Set<ExternalId> externalIdSet);

    boolean isProjectMember(long projectId , Long usingAccount, boolean isAdmin, Set<ExternalId> externalIdSet);

    Account updateProject(Account project, String name, String description);

    List<? extends ProjectMember> getActiveProjectMembers(long projectId);

    ProjectMember getProjectMember(long id);

    boolean hasAccessToAnyProject(Set<ExternalId> externalIds, boolean isAdmin, Long usingAccount);

    List<? extends ProjectMember> setProjectMembers(final Account project, final Set<Member> membersTransformed);

    ProjectMember createProjectMember(Account project, Member member);

    Account setDefaultProject(Account project, long accountId);

    Account getDefaultProject(Account account);
}
