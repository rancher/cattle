package io.cattle.platform.iaas.api.auth.dao;

import io.cattle.platform.api.auth.Identity;
import io.cattle.platform.core.model.Account;
import io.cattle.platform.core.model.ProjectMember;
import io.cattle.platform.iaas.api.auth.projects.Member;

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

    void updateAccount(Account account, String name, String kind, String externalId, String externalType);

    List<Account> getAccessibleProjects(Set<Identity> identitySet, boolean isAdmin, Long usingAccount);

    boolean hasAccessToProject(long projectId, Long usingAccount, boolean isAdmin, Set<Identity> identitySet);

    boolean isProjectOwner(long projectId, Long usingAccount, boolean isAdmin, Set<Identity> identitySet);

    boolean isProjectMember(long projectId, Long usingAccount, boolean isAdmin, Set<Identity> identitySet);

    List<? extends ProjectMember> getActiveProjectMembers(long projectId);

    ProjectMember getProjectMember(long id);

    boolean hasAccessToAnyProject(Set<Identity> identities, boolean isAdmin, Long usingAccount);

    List<? extends ProjectMember> setProjectMembers(final Account project, final Set<Member> membersTransformed);

    ProjectMember createProjectMember(Account project, Member member);

    void ensureAllProjectsHaveNonRancherIdMembers(Identity identity);

    List<Account> searchAccounts(String name);

    Account getByName(String name);
}
