package io.cattle.platform.servicediscovery.process;

import static io.cattle.platform.core.model.tables.InstanceHostMapTable.INSTANCE_HOST_MAP;

import io.cattle.platform.allocator.service.CacheManager;
import io.cattle.platform.allocator.service.HostInfo;
import io.cattle.platform.allocator.service.InstanceInfo;
import io.cattle.platform.core.constants.InstanceConstants;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.core.model.InstanceHostMap;
import io.cattle.platform.engine.handler.HandlerResult;
import io.cattle.platform.engine.handler.ProcessPostListener;
import io.cattle.platform.engine.process.ProcessInstance;
import io.cattle.platform.engine.process.ProcessState;
import io.cattle.platform.process.common.handler.AbstractObjectProcessLogic;
import io.cattle.platform.servicediscovery.api.dao.ServiceDao;
import io.cattle.platform.util.type.Priority;

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

public class InstancePurgePostListener extends AbstractObjectProcessLogic implements ProcessPostListener, Priority {

    private static final Logger log = LoggerFactory.getLogger(InstancePurgePostListener.class);

    @Inject
    ServiceDao serviceDao;

    @Override
    public String[] getProcessNames() {
        return new String[] { InstanceConstants.PROCESS_PURGE };
    }

    @Override
    public HandlerResult handle(ProcessState state, ProcessInstance process) {
        Instance instance = (Instance) state.getResource();
        if (instance == null) {
            return null;
        }

        List<InstanceHostMap> instanceHostMappings = objectManager.find(InstanceHostMap.class,
                INSTANCE_HOST_MAP.INSTANCE_ID, instance.getId());

        for (InstanceHostMap mapping : instanceHostMappings) {
            try {
                removeInstanceFromScheduler(instance.getId(), mapping.getHostId(), instance.getAccountId());
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            CacheManager cm = CacheManager.getCacheManagerInstance(this.objectManager);
            HostInfo hostInfo = cm.getHostInfo(mapping.getHostId(), false);
            if (hostInfo == null) {
                return null;
            }
            InstanceInfo instanceInfo = hostInfo.removeInstance(mapping.getInstanceId());
            if (instanceInfo != null) {
                log.debug("removed instance [{}] info from host [{}] info in cache manager", instance.getId(),
                        mapping.getHostId());
            }
        }
        return null;
    }
    
    protected void removeInstanceFromScheduler(Long instanceId, Long hostId, Long envId) throws IOException {
        String REMOVE_HOST_URL = "http://localhost:8090/v1-scheduler/remove-instance";
        List<BasicNameValuePair> requestData = new ArrayList<>();

        requestData.add(new BasicNameValuePair("hostId", "1h" + hostId.toString()));
        requestData.add(new BasicNameValuePair("instanceId", "1i" + instanceId.toString()));
        requestData.add(new BasicNameValuePair("envId", "1a" + envId.toString()));

        HttpResponse response;
        try {
            response = Request.Post(REMOVE_HOST_URL)
                    .addHeader("Accept", "application/json").bodyForm(requestData)
                    .execute().returnResponse();
            int statusCode = response.getStatusLine().getStatusCode();
            if (statusCode != 200) {
                log.error("statusCode: {}", statusCode);
            }

        } catch(HttpHostConnectException ex) {
            log.error("Scheduler Service not reachable at [{}]", REMOVE_HOST_URL);
            return;
        }
        log.info(response.toString());
        return;

    }

    @Override
    public int getPriority() {
        return Priority.DEFAULT;
    }
}
