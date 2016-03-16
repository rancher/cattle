package io.cattle.platform.iaas.api.auth.dao;

import io.cattle.platform.api.auth.Identity;
import io.cattle.platform.api.auth.Policy;
import io.cattle.platform.core.model.Account;
import io.cattle.platform.core.model.ProjectMember;
import io.cattle.platform.iaas.api.auth.projects.Member;
import io.github.ibuildthecloud.gdapi.id.IdFormatter;
import io.github.ibuildthecloud.gdapi.util.TransformationService;

import java.util.List;
import java.util.Set;

public interface AuthDao {

    Account getAdminAccount();

    Account getAccountById(Long id);

    Account getAccountByKeys(String access, String secretKey, TransformationService transformationService);

    Account getAccountByExternalId(String externalId, String externalType);

    Account getAccountByUuid(String uuid);

    Account createAccount(String name, String kind, String externalId, String externalType);

    Identity getIdentity(Long id, IdFormatter idFormatter);

    Account createProject(String name, String description);

    Account updateAccount(Account account, String name, String kind, String externalId, String externalType);

    List<Account> getAccessibleProjects(Set<Identity> identitySet, boolean isAdmin, Long usingAccount);

    boolean hasAccessToProject(long projectId, Long usingAccount, boolean isAdmin, Set<Identity> identitySet);

    boolean isProjectOwner(long projectId, Long usingAccount, boolean isAdmin, Set<Identity> identitySet);

    boolean isProjectMember(long projectId, Long usingAccount, boolean isAdmin, Set<Identity> identitySet);

    List<? extends ProjectMember> getActiveProjectMembers(long projectId);

    List<? extends ProjectMember> getProjectMembersByIdentity(long projectId, Set<Identity> identities);

    ProjectMember getProjectMember(long id);

    boolean hasAccessToAnyProject(Set<Identity> identities, boolean isAdmin, Long usingAccount);

    List<? extends ProjectMember> setProjectMembers(final Account project, final Set<Member> membersTransformed,
                                                    IdFormatter idFormatter);

    ProjectMember createProjectMember(Account project, Member member);

    void ensureAllProjectsHaveNonRancherIdMembers(Identity identity);

    List<Account> searchUsers(String name);

    Account getByUsername(String username);

    Account getAccountByLogin(String publicValue, String secretValue, TransformationService transformationService);

    String getRole(Account account, Policy policy);

    Account getAccountByAccessKey(String accessKey);
}
