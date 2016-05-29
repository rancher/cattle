package io.cattle.platform.servicediscovery.process;

import io.cattle.platform.allocator.service.CacheManager;
import io.cattle.platform.archaius.util.ArchaiusUtil;
import io.cattle.platform.core.constants.HostConstants;
import io.cattle.platform.core.constants.InstanceConstants;
import io.cattle.platform.core.model.Host;
import io.cattle.platform.engine.handler.HandlerResult;
import io.cattle.platform.engine.handler.ProcessPostListener;
import io.cattle.platform.engine.process.ProcessInstance;
import io.cattle.platform.engine.process.ProcessState;
import io.cattle.platform.process.common.handler.AbstractObjectProcessLogic;
import io.cattle.platform.util.type.Priority;
import io.github.ibuildthecloud.gdapi.id.IdFormatter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import org.apache.http.HttpResponse;
import org.apache.http.client.fluent.Request;
import org.apache.http.conn.HttpHostConnectException;
import org.apache.http.message.BasicNameValuePair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class HostRemovePostListener extends AbstractObjectProcessLogic implements ProcessPostListener, Priority {
    private static final Logger log = LoggerFactory.getLogger(InstancePurgePostListener.class);
    private static final String SCHEDULER_URL = ArchaiusUtil.getString("system.stack.scheduler.url").get();

    @Override
    public String[] getProcessNames() {
        return new String[] { "host.remove" };
    }

    @Override
    public HandlerResult handle(ProcessState state, ProcessInstance process) {
        final Host host = (Host) state.getResource();
        CacheManager cm = CacheManager.getCacheManagerInstance(this.objectManager);
        cm.removeHostInfo(host.getId());
        removeHostCacheFromScheduler(host.getId(), host.getAccountId());

        return null;
    }


    @Inject
    IdFormatter idFormatter;

    public void removeHostCacheFromScheduler(Long hostId, Long envId) {
        String ALLOCATE_CPU_MEMORY_URL = SCHEDULER_URL + "/remove-host";
        List<BasicNameValuePair> requestData = new ArrayList<>();

        requestData.add(new BasicNameValuePair("hostId", (String) idFormatter.formatId(HostConstants.TYPE, hostId)));
        requestData.add(new BasicNameValuePair("envId", "1a" + envId.toString()));

        HttpResponse response;
        try {
            response = Request.Post(ALLOCATE_CPU_MEMORY_URL).addHeader("Accept", "application/json")
                    .bodyForm(requestData).execute().returnResponse();
            int statusCode = response.getStatusLine().getStatusCode();
            if (statusCode != 200) {
                log.error("statusCode: {}", statusCode);
            }

        } catch (HttpHostConnectException ex) {
            log.error("Scheduler Service not reachable at [{}]", ALLOCATE_CPU_MEMORY_URL);
            return;
        } catch (IOException e) {
            log.error((e.getStackTrace()).toString(), e);
        }

        return;
    }
    @Override
    public int getPriority() {
        return Priority.DEFAULT;
    }
}
