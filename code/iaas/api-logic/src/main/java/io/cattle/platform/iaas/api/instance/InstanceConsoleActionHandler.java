package io.cattle.platform.iaas.api.instance;

import io.cattle.platform.agent.AgentLocator;
import io.cattle.platform.agent.RemoteAgent;
import io.cattle.platform.api.action.ActionHandler;
import io.cattle.platform.archaius.util.ArchaiusUtil;
import io.cattle.platform.async.utils.TimeoutException;
import io.cattle.platform.core.constants.HostConstants;
import io.cattle.platform.core.constants.InstanceConstants;
import io.cattle.platform.core.model.Host;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.eventing.EventCallOptions;
import io.cattle.platform.eventing.model.Event;
import io.cattle.platform.eventing.model.EventVO;
import io.cattle.platform.iaas.event.IaasEvents;
import io.cattle.platform.object.ObjectManager;
import io.cattle.platform.object.meta.ObjectMetaDataManager;
import io.cattle.platform.util.type.CollectionUtils;
import io.github.ibuildthecloud.gdapi.context.ApiContext;
import io.github.ibuildthecloud.gdapi.model.impl.ResourceImpl;
import io.github.ibuildthecloud.gdapi.request.ApiRequest;

import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.apache.commons.lang3.text.StrSubstitutor;

public class InstanceConsoleActionHandler implements ActionHandler {

    private static final String VIEW_URL_FORMAT = "console.url.format.";

    AgentLocator locator;
    ObjectManager objectManager;

    @Override
    public String getName() {
        return "instance.console";
    }

    @Override
    public Object perform(String name, Object obj, ApiRequest request) {
        if ( ! (obj instanceof Instance) ) {
            return null;
        }

        Instance instance = (Instance)obj;

        List<Host> hosts = objectManager.mappedChildren(instance, Host.class);

        if ( hosts.size() != 1 ) {
            return null;
        }

        RemoteAgent agent = locator.lookupAgent(hosts.get(0));

        if ( agent == null ) {
            return null;
        }

        Map<String,Object> data = CollectionUtils.asMap(
                HostConstants.TYPE, hosts.get(0),
                InstanceConstants.TYPE, instance,
                ObjectMetaDataManager.KIND_FIELD, null);
        EventVO<Object> event = EventVO.newEvent(IaasEvents.CONSOLE_ACCESS).withData(data);

        try {
            Event result = agent.callSync(event, new EventCallOptions(0, 5000L));

            if ( result.getData() == null ) {
                return null;
            }

            Map<String,Object> responseData = CollectionUtils.<String,Object>toMap(result.getData());
            Map<String,Object> subData = new HashMap<String,Object>();
            for ( Map.Entry<String, Object> entry : responseData.entrySet() ) {
                if ( entry.getValue() != null ) {
                    subData.put(entry.getKey(), URLEncoder.encode(entry.getValue().toString(), "UTF-8"));
                }
            }

            subData.put("static", ApiContext.getUrlBuilder().staticResource());

            ResourceImpl resource = new ResourceImpl(null, "instanceConsole", responseData);
            if ( responseData.size() > 0 ) {
                String format = ArchaiusUtil.getString(VIEW_URL_FORMAT + responseData.get("kind")).get();
                if ( format != null ) {
                    String url = new StrSubstitutor(subData).replace(format);
                    try {
                        resource.getFields().put("view", url);
                        resource.getLinks().put("view", new URL(url));
                    } catch (MalformedURLException e) {
                    }
                }
            }
            return resource;
        } catch ( TimeoutException e ) {
            return null;
        } catch ( UnsupportedEncodingException e ) {
            return null;
        }
    }

    public AgentLocator getLocator() {
        return locator;
    }

    @Inject
    public void setLocator(AgentLocator locator) {
        this.locator = locator;
    }

    public ObjectManager getObjectManager() {
        return objectManager;
    }

    @Inject
    public void setObjectManager(ObjectManager objectManager) {
        this.objectManager = objectManager;
    }

}