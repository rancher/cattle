package io.cattle.platform.agent.connection.simulator.impl;

import io.cattle.platform.agent.connection.simulator.AgentConnectionSimulator;
import io.cattle.platform.agent.connection.simulator.AgentSimulatorEventProcessor;
import io.cattle.platform.agent.util.AgentUtils;
import io.cattle.platform.configitem.events.ConfigUpdate;
import io.cattle.platform.configitem.request.ConfigUpdateItem;
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
import java.util.zip.GZIPInputStream;

import javax.inject.Inject;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SimulatorConfigUpdateProcessor implements AgentSimulatorEventProcessor {

    private static final Logger log = LoggerFactory.getLogger(SimulatorConfigUpdateProcessor.class);

    JsonMapper jsonMapper;
    ObjectManager objectManager;

    @Override
    public Event handle(AgentConnectionSimulator simulator, Event event) throws IOException {
        if ( ! IaasEvents.CONFIG_UPDATE.equals(event.getName()) ) {
            return null;
        }

        ConfigUpdate update = jsonMapper.convertValue(event, ConfigUpdate.class);
        String auth = AgentUtils.getAgentAuth(simulator.getAgent(), objectManager);

        if ( auth == null ) {
            throw new IllegalStateException("Failed to get auth for agent [" + simulator.getAgentId() + "]");
        }

        for ( ConfigUpdateItem item : update.getData().getItems() ) {
            try {
                downloadAndPost(auth, update.getData().getConfigUrl(), item);
            } catch (IOException e) {
                throw new IllegalStateException("Failed to download [" + item.getName() + "]", e);
            }
        }

        return EventVO.reply(event);
    }

    protected URLConnection getConnection(String url, String auth) throws IOException {
        URLConnection urlConnection = new URL(url).openConnection();
        urlConnection.addRequestProperty("Authorization", auth);

        return urlConnection;
    }

    protected void downloadAndPost(String auth, String url, ConfigUpdateItem item) throws IOException {
        String contentUrl = (url + "/configcontent/" + item.getName()).toLowerCase();
        URLConnection urlConnection = getConnection(contentUrl, auth);

        log.info("Simulator downloading [{}]", contentUrl);

        InputStream is = new GZIPInputStream(urlConnection.getInputStream());
        TarArchiveInputStream tar = new TarArchiveInputStream(is);

        try {
            TarArchiveEntry entry = null;
            String version = null;

            while ( (entry = tar.getNextTarEntry()) != null ) {
                log.info("Simulator in [{}] file [{}]", item.getName(), entry.getName());

                if ( entry.getName().endsWith("/version") ) {
                    version = IOUtils.toString(tar).trim();
                    log.info("Simulator found version [{}] for [{}]", version, item.getName());
                }
            }

            if ( version == null ) {
                throw new IllegalStateException("Failed to find verions for [" + item.getName() + "]");
            }

            log.info("Simulator POSTing version [{}] for [{}]", version, item.getName());
            HttpURLConnection conn = (HttpURLConnection)getConnection(contentUrl + "?version=" + version, auth);
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
