package io.cattle.platform.configitem.events;

import io.cattle.platform.configitem.events.ConfigUpdated.ConfigUpdatedData;
import io.cattle.platform.eventing.model.EventVO;
import io.cattle.platform.iaas.event.IaasEvents;

public class ConfigUpdated extends EventVO<ConfigUpdatedData> {

    /**
     * Do not use this constructor, only used for JSON unmarshalling
     */
    public ConfigUpdated() {
    }

    public ConfigUpdated(Class<?> clazz, long resourceId, String itemName) {
        setName(IaasEvents.CONFIG_UPDATED);
        setData(new ConfigUpdatedData(clazz, resourceId, itemName));
    }

    public static final class ConfigUpdatedData {
        Class<?> clazz;
        long resourceId;
        String itemName;

        /**
         * Do not use this constructor, only used for JSON unmarshalling
         */
        public ConfigUpdatedData() {
        }

        public ConfigUpdatedData(Class<?> clazz, long resourceId, String itemName) {
            super();
            this.clazz = clazz;
            this.resourceId = resourceId;
            this.itemName = itemName;
        }

        public Class<?> getClazz() {
            return clazz;
        }

        public long getResourceId() {
            return resourceId;
        }

        public String getItemName() {
            return itemName;
        }
    }
}