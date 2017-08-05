package io.cattle.platform.revision;

import io.cattle.platform.core.constants.InstanceConstants;
import io.cattle.platform.core.constants.ServiceConstants;
import io.cattle.platform.core.model.Revision;
import io.cattle.platform.object.meta.ObjectMetaDataManager;
import io.cattle.platform.object.util.DataAccessor;
import io.cattle.platform.object.util.ObjectUtils;
import io.cattle.platform.util.resource.UUID;
import io.cattle.platform.util.type.CollectionUtils;
import io.github.ibuildthecloud.gdapi.model.Field;
import io.github.ibuildthecloud.gdapi.model.Schema;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class RevisionDiffomatic {

    private static Set<String> UPDATE_FIELDS = CollectionUtils.set(
            ServiceConstants.FIELD_SCALE,
            ServiceConstants.FIELD_SCALE_MIN,
            ServiceConstants.FIELD_SCALE_MAX,
            ServiceConstants.FIELD_SCALE_INCREMENT);
    private static Set<String> DYNAMIC_FIELDS = CollectionUtils.set(
            ServiceConstants.FIELD_SERVICE_LINKS,
            InstanceConstants.FIELD_PORTS);
    private static Set<String> UPDATE_SKIP_FIELDS = CollectionUtils.set(
            ServiceConstants.FIELD_LAUNCH_CONFIG,
            ServiceConstants.FIELD_SECONDARY_LAUNCH_CONFIGS,
            ServiceConstants.FIELD_COMPLETE_UPDATE,
            ServiceConstants.FIELD_FORCE_UPGRADE,
            ServiceConstants.FIELD_VERSION);

    boolean createRevision;
    Map<String, Object> newRevisionData;
    Revision currentRevision;
    Schema schema;
    String newVersion = UUID.randomUUID().toString();

    public RevisionDiffomatic(Revision currentRevision, Map<String, Object> changes, Schema schema) {
        this.currentRevision = currentRevision;
        this.schema = schema;
        generateNewRevisionData(changes);
    }

    public static void removeUpdateFields(Map<String, Object> data) {
        for (String key : UPDATE_FIELDS) {
            data.remove(key);
        }
    }

    private void generateNewRevisionData(Map<String, Object> changes) {
        newRevisionData = changes;
        if (currentRevision == null) {
            return;
        }

        // Start with current revision as the base
        newRevisionData = new HashMap<>(DataAccessor.fieldMap(currentRevision, InstanceConstants.FIELD_REVISION_CONFIG));

        processLaunchConfigs(changes);
        processServiceFields(changes);
    }

    protected void processLaunchConfigs(Map<String, Object> changes) {
        boolean complete = DataAccessor.fromMap(changes)
                .withDefault(false)
                .withKey(ServiceConstants.FIELD_COMPLETE_UPDATE)
                .as(Boolean.class);
        Map<String, Map<String, Object>> mergedLcs = mergeLaunchConfigs(
                extractLaunchConfigs(newRevisionData),
                extractLaunchConfigs(changes), complete);

        Map<String, Object> primary = mergedLcs.get(ServiceConstants.PRIMARY_LAUNCH_CONFIG_NAME);
        if (primary != null && primary.size() > 0) {
            newRevisionData.put(ServiceConstants.FIELD_LAUNCH_CONFIG, primary);
        }

        addSecondaryLcsInOrder(DataAccessor.fieldMap(currentRevision, InstanceConstants.FIELD_REVISION_CONFIG), changes, mergedLcs);
    }

    protected void processServiceFields(Map<String, Object> changes) {
        changes.forEach((key, value) -> {
            if (UPDATE_SKIP_FIELDS.contains(key)) {
                return;
            }
            if (fieldChanged(key, value) && !UPDATE_FIELDS.contains(key)) {
                createRevision = true;
            }
            newRevisionData.put(key, value);
        });
    }

    protected boolean fieldChanged(String key, Object value) {
        return !ObjectUtils.areObjectsEqual(value, newRevisionData.get(key));
    }

    private void addSecondaryLcsInOrder(Map<String, Object> currentData, Map<String, Object> newData, Map<String, Map<String, Object>> lcs) {
        // Assemble order from current and new
        Set<String> order = new LinkedHashSet<>(getSecondaryList(currentData));
        order.addAll(getSecondaryList(newData));

        List<Map<String, Object>> secondaryLcs = new ArrayList<>();
        for (String name : order) {
            Map<String, Object> lc = lcs.get(name);
            if (lc != null && lc.size() > 0) {
                secondaryLcs.add(lc);
            }
        }

        newRevisionData.put(ServiceConstants.FIELD_SECONDARY_LAUNCH_CONFIGS, secondaryLcs);
    }

    private List<String> getSecondaryList(Map<String, Object> data) {
        return CollectionUtils.toList(data.get(ServiceConstants.FIELD_SECONDARY_LAUNCH_CONFIGS)).stream()
                .map((x) -> {
                    Map<String, Object> lc = CollectionUtils.toMap(x);
                    Object name = lc.get(ObjectMetaDataManager.NAME_FIELD);
                    return name == null ? "_" : name.toString();
                }).collect(Collectors.toList());
    }

    private Map<String, Map<String, Object>> extractLaunchConfigs(Map<String, Object> data) {
        Map<String, Object> primary = CollectionUtils.toMap(data.get(ServiceConstants.FIELD_LAUNCH_CONFIG));
        List<?> secondaries = CollectionUtils.toList(data.get(ServiceConstants.FIELD_SECONDARY_LAUNCH_CONFIGS));

        Map<String, Map<String, Object>> result = new HashMap<>();
        if (primary.size() > 0) {
            result.put(ServiceConstants.PRIMARY_LAUNCH_CONFIG_NAME, primary);
        }

        for (Object val : secondaries) {
            Map<String, Object> secondary = CollectionUtils.toMap(val);
            Object name = secondary.get(ObjectMetaDataManager.NAME_FIELD);
            if (name == null || secondary.size() == 0) {
                continue;
            }
            result.put(name.toString(), secondary);
        }

        return result;
    }

    private Map<String, Map<String, Object>> mergeLaunchConfigs(Map<String, Map<String, Object>> currentLcs,
            Map<String, Map<String, Object>> newLcs, boolean complete) {
        Map<String, Map<String, Object>> result = complete ? new HashMap<>() : new HashMap<>(currentLcs);
        newLcs.forEach((name, lc) -> {
            boolean lcChanged = false;
            boolean applyVersion = false;

            // Delete LaunchConfig
            if (shouldRemove(lc)) {
                createRevision = true;
                result.remove(name);
                return;
            }

            Map<String, Object> currentLc = currentLcs.get(name);
            Map<String, Object> newLc = new HashMap<>();

            // New LaunchConfig
            if (currentLc == null) {
                createRevision = true;
                newLc.putAll(lc);
                newLc.put(ServiceConstants.FIELD_VERSION, newVersion);
                result.put(name, newLc);
                return;
            }

            // Compare LaunchConfigs
            try {
                boolean lcComplete = DataAccessor.fromMap(lc)
                        .withKey(ServiceConstants.FIELD_COMPLETE_UPDATE)
                        .withDefault(false)
                        .as(Boolean.class);
                boolean force = DataAccessor.fromMap(lc)
                        .withKey(ServiceConstants.FIELD_FORCE_UPGRADE)
                        .withDefault(false)
                        .as(Boolean.class);

                if (force) {
                    lcChanged = true;
                    applyVersion = true;
                }

                for (Map.Entry<String, Field> entry : schema.getResourceFields().entrySet()) {
                    String fieldName = entry.getKey();
                    Field field = entry.getValue();

                    if (UPDATE_SKIP_FIELDS.contains(fieldName)) {
                        continue;
                    }

                    // Piecemeal copy fields so that skip fields are not included
                    if (lc.containsKey(fieldName)) {
                        newLc.put(fieldName, lc.get(fieldName));
                    } else if (!lcComplete && currentLc.containsKey(fieldName)) {
                        newLc.put(fieldName, currentLc.get(fieldName));
                    }

                    Object left = currentLc.get(fieldName), right = newLc.get(fieldName);
                    if (left == null) {
                        left = field.getDefault();
                    }
                    if (right == null) {
                        right = field.getDefault();
                    }
                    if (!ObjectUtils.areObjectsEqual(left, right)) {
                        lcChanged = true;
                        if (!DYNAMIC_FIELDS.contains(fieldName)) {
                            applyVersion = true;
                        }
                    }
                }
            } finally {
                if (lcChanged) {
                    createRevision = true;
                    String version = null;
                    if (applyVersion) {
                        // New version
                        version = newVersion;
                    } else if (currentLc != null) {
                        // Use old version
                        version = ObjectUtils.toString(currentLc.get(ServiceConstants.FIELD_VERSION));
                    }

                    if (version == null) {
                        version = "0";
                    }

                    newLc.put(ServiceConstants.FIELD_VERSION, version);
                }
                if (lcChanged || complete) {
                    result.put(name, newLc);
                }
            }
        });

        return result;
    }

    protected boolean shouldRemove(Map<String, Object> lc) {
        Object imageUuid = lc.get(InstanceConstants.FIELD_IMAGE);
        if (imageUuid == null || !StringUtils.equalsIgnoreCase(ServiceConstants.IMAGE_NONE, imageUuid.toString())) {
            return false;
        }
        Object selector = lc.get(ServiceConstants.FIELD_SELECTOR_CONTAINER);
        return !(selector != null && !StringUtils.isBlank(selector.toString()));
    }

    public Map<String, Object> getNewRevisionData() {
        return newRevisionData;
    }

    public boolean isCreateRevision() {
        return createRevision;
    }

    public String getNewVersion() {
        return newVersion;
    }

}