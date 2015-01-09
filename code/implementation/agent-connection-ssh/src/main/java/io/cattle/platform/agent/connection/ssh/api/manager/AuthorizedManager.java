package io.cattle.platform.agent.connection.ssh.api.manager;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Map;

import javax.inject.Inject;

import io.cattle.platform.agent.connection.ssh.api.model.Authorized;
import io.cattle.platform.agent.connection.ssh.dao.SshAgentDao;
import io.github.ibuildthecloud.gdapi.context.ApiContext;
import io.github.ibuildthecloud.gdapi.factory.SchemaFactory;
import io.github.ibuildthecloud.gdapi.model.ListOptions;
import io.github.ibuildthecloud.gdapi.request.ApiRequest;
import io.github.ibuildthecloud.gdapi.request.resource.impl.AbstractNoOpResourceManager;

public class AuthorizedManager extends AbstractNoOpResourceManager {

    SshAgentDao agentDao;

    @Override
    protected Object listInternal(SchemaFactory schemaFactory, String type, Map<Object, Object> criteria, ListOptions options) {
        ApiRequest request = ApiContext.getContext().getApiRequest();
        request.setResponseContentType("text/plain");

        try {
            OutputStream os = request.getOutputStream();

            for (String[] keys : agentDao.getClientKeyPairs()) {
                os.write(keys[0].getBytes("UTF-8"));
                os.write('\n');
            }

            return new Object();
        } catch (IOException e) {
            throw new IllegalStateException("Failed to list keys", e);
        }
    }

    @Override
    public Class<?>[] getTypeClasses() {
        return new Class<?>[] { Authorized.class };
    }

    public SshAgentDao getAgentDao() {
        return agentDao;
    }

    @Inject
    public void setAgentDao(SshAgentDao agentDao) {
        this.agentDao = agentDao;
    }

}