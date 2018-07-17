package io.cattle.platform.servicediscovery.process;

import static io.cattle.platform.core.model.tables.AccountTable.*;
import io.cattle.platform.agent.instance.service.AgentMetadataService;
import io.cattle.platform.archaius.util.ArchaiusUtil;
import io.cattle.platform.core.constants.AccountConstants;
import io.cattle.platform.core.model.Account;
import io.cattle.platform.object.ObjectManager;
import io.cattle.platform.util.type.InitializationTask;
import io.github.ibuildthecloud.gdapi.condition.Condition;
import io.github.ibuildthecloud.gdapi.condition.ConditionType;

import java.util.List;

import javax.inject.Inject;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.netflix.config.DynamicBooleanProperty;

public class IpsecEnableHealthcheckListener implements InitializationTask {
    private static final DynamicBooleanProperty ENABLE_HEALTHCHECK = ArchaiusUtil.getBoolean("ipsec.service.enable.healthcheck");
    private static final Log logger = LogFactory.getLog(IpsecEnableHealthcheckListener.class);
    
    @Inject
    ObjectManager objectManager;
    @Inject
    AgentMetadataService agentMetadata;
        
    @Override
    public void start() {
        logger.info("TESTTEST");
        Runnable updateMetadata = new Runnable() {
            public void run() {
                logger.info("NAIYA changed!");
                List<Account> accounts = objectManager.find(Account.class, ACCOUNT.KIND, AccountConstants.PROJECT_KIND, 
                        ACCOUNT.REMOVED, new Condition(ConditionType.NULL));
                for(Account account : accounts) {
                        agentMetadata.updateMetadata(account.getId());
                }
            }
        };
        ENABLE_HEALTHCHECK.addCallback(updateMetadata);
    }
    
}
