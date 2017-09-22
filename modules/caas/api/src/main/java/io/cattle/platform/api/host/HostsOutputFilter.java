package io.cattle.platform.api.host;

import io.cattle.platform.api.common.CachedOutputFilter;
import io.cattle.platform.core.constants.CommonStatesConstants;
import io.cattle.platform.core.constants.HostConstants;
import io.cattle.platform.core.dao.HostDao;
import io.cattle.platform.core.model.Host;
import io.cattle.platform.framework.secret.SecretsService;
import io.cattle.platform.object.meta.ObjectMetaDataManager;
import io.cattle.platform.object.util.DataAccessor;
import io.github.ibuildthecloud.gdapi.context.ApiContext;
import io.github.ibuildthecloud.gdapi.factory.SchemaFactory;
import io.github.ibuildthecloud.gdapi.model.Resource;
import io.github.ibuildthecloud.gdapi.model.Schema;
import io.github.ibuildthecloud.gdapi.request.ApiRequest;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URL;
import java.util.List;
import java.util.Map;

import static io.cattle.platform.core.constants.HostConstants.*;

public class HostsOutputFilter extends CachedOutputFilter<HostsOutputFilter.Data> {

    private static final Logger log = LoggerFactory.getLogger(HostsOutputFilter.class);

    HostDao hostDao;
    SecretsService serviceService;

    public HostsOutputFilter(HostDao hostDao, SecretsService serviceService) {
        this.hostDao = hostDao;
        this.serviceService = serviceService;
    }

    @Override
    public Resource filter(ApiRequest request, Object original, Resource converted) {
        if (!(original instanceof Host)) {
            return converted;
        }

        Host host = (Host)original;

        Object extracted = converted.getFields().get(HostConstants.EXTRACTED_CONFIG_FIELD);
        if (extracted instanceof String) {
            try {
                if (((String) extracted).startsWith("{")) {
                    extracted = serviceService.decrypt((String)extracted);
                }
            } catch (Exception e) {
                log.error("Failed to decrypt machine extracted config", e);
            }
            converted.getFields().put(HostConstants.EXTRACTED_CONFIG_FIELD, extracted);
        }

        Data data = getCached(request);
        if (data != null) {
            Map<String, Object> fields = converted.getFields();
            fields.put(HostConstants.FIELD_INSTANCE_IDS, data.instancesPerHost.get(host.getId()));

            String agentState = host.getAgentState();
            if (CommonStatesConstants.ACTIVE.equals(host.getState()) && StringUtils.isNotBlank(agentState)) {
                fields.put(ObjectMetaDataManager.STATE_FIELD, agentState);
            }
        }

        return addLinks(original, converted);
    }

    private Resource addLinks(Object original, Resource converted) {
        boolean add = false;
        if (original instanceof Host) {
            if (StringUtils.isNotEmpty((String) DataAccessor.getFields(original).get(EXTRACTED_CONFIG_FIELD))) {
                add = canAccessConfig();
            }
            if (!add && StringUtils.isNotEmpty((String) converted.getFields().get(FIELD_DRIVER))) {
                add = canAccessConfig();
            }
            boolean imported = DataAccessor.fieldBool(original, HostConstants.FIELD_IMPORTED);
            if (imported) {
                Map<String, URL> links = converted.getLinks();
                if (links != null) {
                    links.remove("remove");
                }
            }
        }

        if (add) {
            converted.getLinks().put(CONFIG_LINK, ApiContext.getUrlBuilder().resourceLink(converted, CONFIG_LINK));
        }

        converted.getLinks().put(STORAGE_POOLS_LINK, ApiContext.getUrlBuilder().resourceLink(converted, STORAGE_POOLS_LINK));

        return converted;
    }

    public static boolean canAccessConfig() {
        SchemaFactory schemaFactory = ApiContext.getSchemaFactory();
        Schema machineSchema = schemaFactory == null ? null : schemaFactory.getSchema(Host.class);
        return machineSchema != null && machineSchema.getCollectionMethods().contains(Schema.Method.POST.toString());
    }

    @Override
    protected HostsOutputFilter.Data newObject(ApiRequest apiRequest) {
        List<Long> ids = getIds(apiRequest);
        Data data = new Data();
        data.instancesPerHost = hostDao.getInstancesPerHost(ids, ApiContext.getContext().getIdFormatter());
        return data;
    }

    @Override
    protected Long getId(Object obj) {
        if (obj instanceof Host) {
            return ((Host) obj).getId();
        }
        return null;
    }

    static class Data {
        Map<Long, List<Object>> instancesPerHost;
    }

}
