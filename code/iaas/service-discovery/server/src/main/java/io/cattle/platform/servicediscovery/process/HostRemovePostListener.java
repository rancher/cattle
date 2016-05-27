package io.cattle.platform.servicediscovery.process;

import io.cattle.platform.allocator.service.CacheManager;
import io.cattle.platform.core.model.Host;
import io.cattle.platform.engine.handler.HandlerResult;
import io.cattle.platform.engine.handler.ProcessPostListener;
import io.cattle.platform.engine.process.ProcessInstance;
import io.cattle.platform.engine.process.ProcessState;
import io.cattle.platform.process.common.handler.AbstractObjectProcessLogic;
import io.cattle.platform.util.type.Priority;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.HttpResponse;
import org.apache.http.client.fluent.Request;
import org.apache.http.conn.HttpHostConnectException;
import org.apache.http.message.BasicNameValuePair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class HostRemovePostListener extends AbstractObjectProcessLogic implements ProcessPostListener, Priority {
    private static final Logger log = LoggerFactory.getLogger(InstancePurgePostListener.class);

    @Override
    public String[] getProcessNames() {
        return new String[] { "host.remove" };
    }

    @Override
    public HandlerResult handle(ProcessState state, ProcessInstance process) {
        final Host host = (Host) state.getResource();
        CacheManager cm = CacheManager.getCacheManagerInstance(this.objectManager);
        cm.removeHostInfo(host.getId());
        try {
            removeHostCacheFromScheduler(host.getId(), host.getAccountId());
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return null;
    }

    protected void removeHostCacheFromScheduler(Long hostId, Long envId) throws IOException {
        String REMOVE_HOST_URL = "http://localhost:8090/v1-scheduler/remove-host";
        String GET_REMOVE_HOST_URL = "http://localhost:8090/v1-scheduler/remove-host?hostId=1h2&envId=1a5";
        //StringBuilder schedulerUrl = new StringBuilder(REMOVE_HOST_URL);
        List<BasicNameValuePair> requestData = new ArrayList<>();

        requestData.add(new BasicNameValuePair("hostId", "1h" + hostId.toString()));
        requestData.add(new BasicNameValuePair("envId", "1a" + envId.toString()));

        //Map<String, Object> jsonData;

        HttpResponse response;
        try {
            //response = Request.Post(REMOVE_HOST_URL)
                    //.addHeader("Accept", "application/json").bodyForm(requestData)
                    //.execute().returnResponse();
            response = Request.Get(GET_REMOVE_HOST_URL)
                    .execute().returnResponse();
            int statusCode = response.getStatusLine().getStatusCode();
            if (statusCode != 200) {
                log.error("statusCode: {}", statusCode);
            }

        } catch(HttpHostConnectException ex) {
            log.error("Catalog Service not reachable at [{}]", REMOVE_HOST_URL);
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
