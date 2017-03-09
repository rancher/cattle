package io.cattle.platform.core.dao;

import io.cattle.platform.core.addon.ServiceLink;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.core.model.InstanceLink;
import io.cattle.platform.core.model.Service;
import io.cattle.platform.core.model.ServiceConsumeMap;

import java.util.List;
import java.util.Map;

public interface ServiceConsumeMapDao {

    ServiceConsumeMap findMapToRemove(long serviceId, long consumedServiceId);

    ServiceConsumeMap findNonRemovedMap(long serviceId, long consumedServiceId, String linkName);

    /**
     * Lists maps to remove referencing services consumed by passed serviceId
     * 
     * @param serviceId
     * @return
     */
    List<? extends ServiceConsumeMap> findConsumedMapsToRemove(long serviceId);

    /**
     * Lists maps to remove consuming service passed by serviceId
     * 
     * @param serviceId
     * @return
     */
    List<? extends ServiceConsumeMap> findConsumingMapsToRemove(long serviceId);

    /**
     * @param serviceId
     * @return list of services consumed by the service specified
     */
    List<? extends ServiceConsumeMap> findConsumedServices(long serviceId);

    /**
     * @param serviceId
     * @return list of services consuming the service specified
     */
    List<? extends ServiceConsumeMap> findConsumingServices(long serviceId);

    /**
     * @param instanceId
     * @param kind
     * @return list of services consumed by the instance specified
     */
    List<? extends ServiceConsumeMap> findConsumedServicesForInstance(long instanceId, String kind);

    /**
     * @param instanceId
     * @return  list of InstanceLink for the instance that are from services, not container links
     */
    List<? extends InstanceLink> findServiceBasedInstanceLinks(long instanceId);

    Instance findOneInstanceForService(long serviceId);

    List<String> findInstanceNamesForService(long serviceId);

    ServiceConsumeMap createServiceLink(Service service, ServiceLink serviceLink);

    List<ServiceConsumeMap> createServiceLinks(List<ServiceLink> serviceLink);

    List<? extends Service> findLinkedServices(long serviceId);

    Map<Long, Long> findConsumedServicesIdsToStackIdsFromOtherAccounts(long accountId);

    Map<Long, Long> findConsumedByServicesIdsToStackIdsFromOtherAccounts(long accountId);

    void removeServiceLink(Service service, ServiceLink serviceLink);
}
