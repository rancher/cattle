package io.cattle.platform.systemstack.process;

import static io.cattle.platform.core.model.tables.AccountTable.*;
import static io.cattle.platform.core.model.tables.ProjectTemplateTable.*;

import io.cattle.platform.core.constants.AccountConstants;
import io.cattle.platform.core.constants.NetworkConstants;
import io.cattle.platform.core.dao.NetworkDao;
import io.cattle.platform.core.model.Account;
import io.cattle.platform.core.model.Network;
import io.cattle.platform.core.model.ProjectTemplate;
import io.cattle.platform.deferred.util.DeferredUtils;
import io.cattle.platform.engine.handler.HandlerResult;
import io.cattle.platform.engine.handler.ProcessPreListener;
import io.cattle.platform.engine.process.ProcessInstance;
import io.cattle.platform.engine.process.ProcessState;
import io.cattle.platform.object.meta.ObjectMetaDataManager;
import io.cattle.platform.object.resource.ResourceMonitor;
import io.cattle.platform.object.resource.ResourcePredicate;
import io.cattle.platform.object.util.DataUtils;
import io.cattle.platform.process.common.handler.AbstractObjectProcessLogic;
import io.cattle.platform.systemstack.catalog.CatalogService;

import java.util.Map;

import javax.inject.Inject;

public class EnvironmentUpgrade extends AbstractObjectProcessLogic implements ProcessPreListener {

    @Inject
    SystemStackTrigger systemStackTrigger;
    @Inject
    ResourceMonitor resourceMonitor;
    @Inject
    NetworkDao networkDao;

    @Override
    public String[] getProcessNames() {
        return new String[] { "account.upgrade" };
    }

    @Override
    public HandlerResult handle(ProcessState state, ProcessInstance process) {
        Account env = (Account)state.getResource();
        if (!AccountConstants.PROJECT_KIND.equals(env.getKind())) {
            return null;
        }

        String version = env.getVersion();
        if (AccountConstants.ACCOUNT_VERSION.get().equals(version)) {
            return null;
        }

        checkDefaultNetwork(env);

        assignTemplate(env);

        Network network = getIpSectNetwork(env);
        networkDao.migrateToNetwork(network);

        objectManager.reload(env);
        return null;
    }

    protected void checkDefaultNetwork(Account account) {
        Map<String, Object> fields = DataUtils.getWritableFields(account);
        boolean changed = fields.remove(AccountConstants.FIELD_DEFAULT_NETWORK_ID) != null;
        if (changed) {
            objectManager.persist(account);
        }
    }

    protected Network getIpSectNetwork(final Account account) {
        resourceMonitor.waitFor(account, new ResourcePredicate<Account>() {
            @Override
            public boolean evaluate(Account obj) {
                return findIpSecNetwork(account) != null;
            }

            @Override
            public String getMessage() {
                return "default network to be create";
            }
        });

        return findIpSecNetwork(account);
    }

    protected Network findIpSecNetwork(Account account) {
        return objectManager.findAny(Network.class,
                ObjectMetaDataManager.KIND_FIELD, NetworkConstants.KIND_CNI,
                ObjectMetaDataManager.REMOVED_FIELD, null);
    }

    protected void assignTemplate(final Account env) {
        if (env.getProjectTemplateId() != null) {
            return;
        }

        final ProjectTemplate template = objectManager.findAny(ProjectTemplate.class,
                PROJECT_TEMPLATE.IS_PUBLIC, true,
                PROJECT_TEMPLATE.REMOVED, null,
                PROJECT_TEMPLATE.NAME, CatalogService.DEFAULT_TEMPLATE.get());

        if (template == null) {
            throw new IllegalStateException("Failed to find default template for upgrade");
        }

        DeferredUtils.nest(new Runnable() {
            @Override
            public void run() {
                systemStackTrigger.trigger(env.getId());
                objectManager.setFields(env, ACCOUNT.PROJECT_TEMPLATE_ID, template.getId());
            }
        });
    }

}
