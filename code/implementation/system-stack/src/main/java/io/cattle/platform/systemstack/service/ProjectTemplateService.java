package io.cattle.platform.systemstack.service;

import static io.cattle.platform.core.model.tables.ProjectTemplateTable.*;

import io.cattle.platform.archaius.util.ArchaiusUtil;
import io.cattle.platform.core.dao.GenericResourceDao;
import io.cattle.platform.core.model.ProjectTemplate;
import io.cattle.platform.object.ObjectManager;
import io.cattle.platform.object.process.ObjectProcessManager;
import io.cattle.platform.object.process.StandardProcess;
import io.cattle.platform.systemstack.catalog.CatalogService;
import io.cattle.platform.task.Task;
import io.cattle.platform.util.type.InitializationTask;
import io.github.ibuildthecloud.gdapi.condition.Condition;
import io.github.ibuildthecloud.gdapi.condition.ConditionType;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;

import javax.inject.Inject;
import javax.inject.Named;

import org.apache.cloudstack.managed.context.NoExceptionRunnable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.netflix.config.DynamicBooleanProperty;

public class ProjectTemplateService implements InitializationTask, Task {

    private static final DynamicBooleanProperty LAUNCH_CATALOG = ArchaiusUtil.getBoolean("catalog.execute");
    private static final DynamicBooleanProperty DEFAULTS = ArchaiusUtil.getBoolean("project.template.defaults");

    private static final Logger log = LoggerFactory.getLogger(ProjectTemplateService.class);

    @Inject
    CatalogService catalogService;
    @Inject @Named("CoreExecutorService")
    ExecutorService executor;
    @Inject
    ObjectManager objectManager;
    @Inject
    ObjectProcessManager processManager;
    @Inject
    GenericResourceDao resourceDao;

    @Override
    public void start() {
        executor.submit(new NoExceptionRunnable() {
            @Override
            protected void doRun() throws Exception {
                while (true) {
                    try {
                        reload();
                        log.info("Loaded project templates from the catalog");
                        break;
                    } catch (IOException e) {
                    }

                    Thread.sleep(1000);
                }
            }
        });
    }

    @Override
    public void run() {
        try {
            reload();
        } catch (IOException e) {
        }
    }

    protected void reload() throws IOException {
        if (!LAUNCH_CATALOG.get() || !DEFAULTS.get()) {
            return;
        }

        List<ProjectTemplate> installedTemplates = objectManager.find(ProjectTemplate.class,
                PROJECT_TEMPLATE.IS_PUBLIC, true,
                PROJECT_TEMPLATE.REMOVED, null,
                PROJECT_TEMPLATE.EXTERNAL_ID, new Condition(ConditionType.LIKE, "catalog://%"));
        Map<String, Map<Object, Object>> templatesToInstall = catalogService.getTemplates(installedTemplates);

        for (ProjectTemplate installed : installedTemplates) {
            // == null means the teamplatesToInstall does not have this externalId key, so remove
            if (templatesToInstall.remove(installed.getExternalId()) == null) {
                log.info("Removing project template [{}]", installed.getExternalId());
                processManager.scheduleStandardProcessAsync(StandardProcess.REMOVE, installed, null);
            }
        }

        for (Map.Entry<String, Map<Object, Object>> entry : templatesToInstall.entrySet()) {
            Map<Object, Object> toInstall = entry.getValue();
            toInstall.put(PROJECT_TEMPLATE.ACCOUNT_ID, null);
            toInstall.put(PROJECT_TEMPLATE.IS_PUBLIC, true);
            toInstall.put(PROJECT_TEMPLATE.EXTERNAL_ID, entry.getKey());

            log.info("Adding project template [{}]", entry.getKey());
            Map<String, Object> data = objectManager.convertToPropertiesFor(ProjectTemplate.class, toInstall);
            resourceDao.createAndSchedule(ProjectTemplate.class, data);
        }
    }

    @Override
    public void stop() {
    }

    @Override
    public String getName() {
        return "project.template.reload";
    }

}
