package io.cattle.platform.agent.connection.simulator.impl;

import io.cattle.platform.agent.connection.simulator.AgentConnectionSimulator;
import io.cattle.platform.agent.connection.simulator.AgentSimulatorEventProcessor;
import io.cattle.platform.agent.util.AgentUtils;
import io.cattle.platform.configitem.events.ConfigUpdate;
import io.cattle.platform.configitem.request.ConfigUpdateItem;
import io.cattle.platform.core.model.Agent;
import io.cattle.platform.eventing.model.Event;
import io.cattle.platform.eventing.model.EventVO;
import io.cattle.platform.iaas.event.IaasEvents;
import io.cattle.platform.json.JsonMapper;
import io.cattle.platform.object.ObjectManager;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.Map;
import java.util.zip.GZIPInputStream;

import javax.inject.Inject;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SimulatorConfigUpdateProcessor implements AgentSimulatorEventProcessor {

    private static final Logger log = LoggerFactory.getLogger(SimulatorConfigUpdateProcessor.class);

    JsonMapper jsonMapper;
    ObjectManager objectManager;

    @Override
    public Event handle(AgentConnectionSimulator simulator, Event event) throws IOException {
        if (!IaasEvents.CONFIG_UPDATE.equals(event.getName())) {
            return null;
        }
        return handle(simulator.getAgent(), event);
    }

    public Event handle(Agent agent, Event event) throws IOException {
        ConfigUpdate update = jsonMapper.convertValue(event, ConfigUpdate.class);
        Map<String, Object> authMap = AgentUtils.getAgentAuth(agent, objectManager);
        if (authMap == null) {
            return null;
        }

        String auth = ObjectUtils.toString(authMap.get("CATTLE_AGENT_INSTANCE_AUTH"), null);
        if (auth == null) {
            throw new IllegalStateException("Failed to get auth for agent [" + agent.getId() + "]");
        }

        for (ConfigUpdateItem item : update.getData().getItems()) {
            downloadAndPost(auth, update.getData().getConfigUrl(), item);
        }

        return EventVO.reply(event);
    }

    protected URLConnection getConnection(String url, String auth) throws IOException {
        URLConnection urlConnection = new URL(url).openConnection();
        urlConnection.addRequestProperty("Authorization", auth);

        return urlConnection;
    }

    protected void downloadAndPost(String auth, String url, ConfigUpdateItem item) throws IOException {
        if (item.getName().equals("metadata-answers") || item.getName().equals("psk")) {
            return;
        }

        String contentUrl = (url + "/configcontent/" + item.getName()).toLowerCase();
        URLConnection urlConnection = getConnection(contentUrl, auth);

        log.info("Simulator downloading [{}]", contentUrl);

        InputStream is = new GZIPInputStream(urlConnection.getInputStream());
        TarArchiveInputStream tar = new TarArchiveInputStream(is);

        try {
            TarArchiveEntry entry = null;
            String version = null;

            while ((entry = tar.getNextTarEntry()) != null) {
                log.info("Simulator in [{}] file [{}]", item.getName(), entry.getName());

                if (entry.getName().endsWith("/version")) {
                    version = IOUtils.toString(tar).trim();
                    log.info("Simulator found version [{}] for [{}]", version, item.getName());
                }
            }

            if (version == null) {
                throw new IllegalStateException("Failed to find versions for [" + item.getName() + "]");
            }

            log.info("Simulator POSTing version [{}] for [{}]", version, item.getName());
            HttpURLConnection conn = (HttpURLConnection) getConnection(contentUrl + "?version=" + version, auth);
            conn.setRequestMethod("PUT");
            IOUtils.closeQuietly(is);
            is = conn.getInputStream();
            /* Fully read response */
            IOUtils.toString(is);
        } finally {
            IOUtils.closeQuietly(tar);
            IOUtils.closeQuietly(is);
        }
    }

    public JsonMapper getJsonMapper() {
        return jsonMapper;
    }

    @Inject
    public void setJsonMapper(JsonMapper jsonMapper) {
        this.jsonMapper = jsonMapper;
    }

    public ObjectManager getObjectManager() {
        return objectManager;
    }

    @Inject
    public void setObjectManager(ObjectManager objectManager) {
        this.objectManager = objectManager;
    }

}
