package io.cattle.platform.servicediscovery.dao.impl;

import static io.cattle.platform.core.model.tables.AccountTable.*;
import static io.cattle.platform.core.model.tables.CertificateTable.*;
import static io.cattle.platform.core.model.tables.HealthcheckInstanceTable.*;
import static io.cattle.platform.core.model.tables.InstanceHostMapTable.*;
import static io.cattle.platform.core.model.tables.InstanceTable.*;
import static io.cattle.platform.core.model.tables.ServiceExposeMapTable.*;
import static io.cattle.platform.core.model.tables.ServiceTable.*;
import io.cattle.platform.configitem.version.ConfigItemStatusManager;
import io.cattle.platform.configitem.version.dao.ConfigItemStatusDao;
import io.cattle.platform.core.constants.InstanceConstants;
import io.cattle.platform.core.constants.LoadBalancerConstants;
import io.cattle.platform.core.model.Account;
import io.cattle.platform.core.model.Certificate;
import io.cattle.platform.core.model.Environment;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.core.model.Service;
import io.cattle.platform.core.model.ServiceConsumeMap;
import io.cattle.platform.core.model.tables.records.InstanceRecord;
import io.cattle.platform.core.model.tables.records.ServiceRecord;
import io.cattle.platform.db.jooq.dao.impl.AbstractJooqDao;
import io.cattle.platform.json.JsonMapper;
import io.cattle.platform.object.ObjectManager;
import io.cattle.platform.object.util.DataAccessor;
import io.cattle.platform.servicediscovery.api.constants.ServiceDiscoveryConstants;
import io.cattle.platform.servicediscovery.api.constants.ServiceDiscoveryConstants.KIND;
import io.cattle.platform.servicediscovery.api.dao.ServiceConsumeMapDao;
import io.cattle.platform.servicediscovery.api.dao.ServiceDao;
import io.cattle.platform.servicediscovery.service.ServiceDiscoveryService;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

public class ServiceDaoImpl extends AbstractJooqDao implements ServiceDao {
    public static final String METADATA_REVISION_UPDATE_CONFIG = "metadata-revision-update";

    @Inject
    JsonMapper jsonMapper;
    @Inject
    ObjectManager objectManager;
    @Inject
    ServiceConsumeMapDao consumeMapDao;
    @Inject
    ServiceDiscoveryService sdService;
    @Inject
    ConfigItemStatusManager itemManager;
    @Inject
    ConfigItemStatusDao configItemStatusDao;

    @Override
    public List<? extends Service> getServicesOnHost(long hostId) {
        return create().select(SERVICE.fields())
                .from(SERVICE)
                .join(SERVICE_EXPOSE_MAP)
                    .on(SERVICE_EXPOSE_MAP.SERVICE_ID.eq(SERVICE.ID))
                .join(INSTANCE_HOST_MAP)
                    .on(SERVICE_EXPOSE_MAP.INSTANCE_ID.eq(INSTANCE_HOST_MAP.INSTANCE_ID))
                .where(INSTANCE_HOST_MAP.HOST_ID.eq(hostId))
                .and(INSTANCE_HOST_MAP.REMOVED.isNull())
                .and(SERVICE_EXPOSE_MAP.REMOVED.isNull())
                .and(SERVICE.REMOVED.isNull())
                .fetchInto(ServiceRecord.class);
    }

    @Override
    public List<? extends Instance> getInstancesWithHealtcheckEnabled(long accountId) {
        return create().select(INSTANCE.fields())
                .from(INSTANCE)
                .join(HEALTHCHECK_INSTANCE)
                .on(HEALTHCHECK_INSTANCE.INSTANCE_ID.eq(INSTANCE.ID))
                .and(HEALTHCHECK_INSTANCE.REMOVED.isNull())
                .and(INSTANCE.REMOVED.isNull())
                .and(INSTANCE.STATE.in(InstanceConstants.STATE_STARTING, InstanceConstants.STATE_RUNNING)
                        .and(INSTANCE.ACCOUNT_ID.eq(accountId)))
                .fetchInto(InstanceRecord.class);
    }

    public List<Certificate> getLoadBalancerServiceCertificates(Service lbService) {
        List<? extends Long> certIds = DataAccessor.fields(lbService)
                .withKey(LoadBalancerConstants.FIELD_LB_CERTIFICATE_IDS).withDefault(Collections.EMPTY_LIST)
                .asList(jsonMapper, Long.class);
        Long defaultCertId = DataAccessor.fieldLong(lbService, LoadBalancerConstants.FIELD_LB_DEFAULT_CERTIFICATE_ID);
        List<Long> allCertIds = new ArrayList<>();
        allCertIds.addAll(certIds);
        allCertIds.add(defaultCertId);
        return create()
                .select(CERTIFICATE.fields())
                .from(CERTIFICATE)
                .where(CERTIFICATE.REMOVED.isNull())
                .and(CERTIFICATE.ID.in(allCertIds))
                .fetchInto(Certificate.class);
    }

    @Override
    public Certificate getLoadBalancerServiceDefaultCertificate(Service lbService) {
        Long defaultCertId = DataAccessor.fieldLong(lbService, LoadBalancerConstants.FIELD_LB_DEFAULT_CERTIFICATE_ID);
        List<? extends Certificate> certs = create()
                .select(CERTIFICATE.fields())
                .from(CERTIFICATE)
                .where(CERTIFICATE.REMOVED.isNull())
                .and(CERTIFICATE.ID.eq(defaultCertId))
                .fetchInto(Certificate.class);
        if (certs.isEmpty()) {
            return null;
        }
        return certs.get(0);
    }

    @Override
    public List<Service> getConsumingLbServices(long serviceId) {
        List<Service> lbServices = new ArrayList<>();
        findConsumingServicesImpl(serviceId, lbServices);
        return lbServices;
    }

    protected void findConsumingServicesImpl(long serviceId, List<Service> lbServices) {
        List<? extends ServiceConsumeMap> consumingServicesMaps = consumeMapDao
                .findConsumingServices(serviceId);
        for (ServiceConsumeMap consumingServiceMap : consumingServicesMaps) {
            Service consumingService = objectManager.loadResource(Service.class, consumingServiceMap.getServiceId());
            if (consumingService.getKind().equalsIgnoreCase(KIND.LOADBALANCERSERVICE.name())) {
                lbServices.add(consumingService);
            } else if (consumingService.getKind().equalsIgnoreCase(KIND.DNSSERVICE.name())) {
                findConsumingServicesImpl(consumingService.getId(), lbServices);
            }
        }
    }

    @Override

    public void incrementMetadataRevision(long accountId, Object object) {
        /*
         * FIXME: yet to figure out optimal way of account metadta revision update
         * 
         * ConfigUpdateRequest request = ConfigUpdateRequest.forResource(Account.class,
         * accountId);
         * request.addItem(METADATA_REVISION_UPDATE_CONFIG);
         * itemManager.updateConfig(request);
         * Long newRevision = configItemStatusDao.getRequestedVersion(request.getClient(),
         * METADATA_REVISION_UPDATE_CONFIG);
         */

        Account account = objectManager.loadResource(Account.class, accountId);
        if (object instanceof Service || object instanceof Instance || object instanceof Environment) {
            Map<String, Object> params = new HashMap<>();
            params.put(ServiceDiscoveryConstants.FIELD_REVISION, account.getMetadataRevision() + 1);
            objectManager.setFields(object, params);
        }
        create()
                .update(ACCOUNT)
                .set(ACCOUNT.METADATA_REVISION, ACCOUNT.METADATA_REVISION.plus(1))
                .where(
                        ACCOUNT.ID.eq(accountId)).execute();
    }
}
