package io.cattle.platform.process.account;

import static io.cattle.platform.core.model.tables.AccountTable.*;
import static io.cattle.platform.core.model.tables.ProjectTemplateTable.*;

import io.cattle.platform.archaius.util.ArchaiusUtil;
import io.cattle.platform.core.constants.AccountConstants;
import io.cattle.platform.core.model.Account;
import io.cattle.platform.core.model.ProjectTemplate;
import io.cattle.platform.engine.handler.HandlerResult;
import io.cattle.platform.engine.handler.ProcessPreListener;
import io.cattle.platform.engine.process.ProcessInstance;
import io.cattle.platform.engine.process.ProcessState;
import io.cattle.platform.process.common.handler.AbstractObjectProcessLogic;

import javax.inject.Named;

import org.apache.commons.lang3.StringUtils;

import com.netflix.config.DynamicStringProperty;

@Named
public class AccountPreCreate extends AbstractObjectProcessLogic implements ProcessPreListener {

    public static final DynamicStringProperty DEFAULT_TEMPLATE = ArchaiusUtil.getString("project.template.default.name");

    @Override
    public String[] getProcessNames() {
        return new String[] { "account.create" };
    }

    @Override
    public HandlerResult handle(ProcessState state, ProcessInstance process) {
        Account account = (Account)state.getResource();
        if (account.getProjectTemplateId() != null || !AccountConstants.PROJECT_KIND.equals(account.getKind())) {
            return null;
        }

        if (StringUtils.isBlank(DEFAULT_TEMPLATE.get())) {
            return null;
        }

        ProjectTemplate template = objectManager.findAny(ProjectTemplate.class,
                PROJECT_TEMPLATE.NAME, DEFAULT_TEMPLATE.get(),
                PROJECT_TEMPLATE.IS_PUBLIC, true,
                PROJECT_TEMPLATE.REMOVED, null);

        return new HandlerResult(
                ACCOUNT.PROJECT_TEMPLATE_ID, template == null ? null : template.getId());
    }


}
