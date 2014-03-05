package io.github.ibuildthecloud.agent.connection.simulator.impl;

import io.github.ibuildthecloud.agent.connection.simulator.AgentConnectionSimulator;
import io.github.ibuildthecloud.agent.connection.simulator.AgentSimulatorEventProcessor;
import io.github.ibuildthecloud.dstack.configitem.events.ConfigUpdate;
import io.github.ibuildthecloud.dstack.configitem.request.ConfigUpdateItem;
import io.github.ibuildthecloud.dstack.core.constants.CommonStatesConstants;
import io.github.ibuildthecloud.dstack.core.model.Account;
import io.github.ibuildthecloud.dstack.core.model.Agent;
import io.github.ibuildthecloud.dstack.core.model.Credential;
import io.github.ibuildthecloud.dstack.eventing.model.Event;
import io.github.ibuildthecloud.dstack.eventing.model.EventVO;
import io.github.ibuildthecloud.dstack.iaas.event.IaasEvents;
import io.github.ibuildthecloud.dstack.json.JsonMapper;
import io.github.ibuildthecloud.dstack.object.ObjectManager;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.zip.GZIPInputStream;

import javax.inject.Inject;

import org.apache.commons.codec.binary.Base64;
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
        String auth = getAuth(simulator.getAgent());

        for ( ConfigUpdateItem item : update.getData().getItems() ) {
            try {
                downloadAndPost(auth, update.getData().getConfigUrl(), item);
            } catch (IOException e) {
                throw new IllegalStateException("Failed to download [" + item.getName() + "]", e);
            }
        }

        return EventVO.reply(event);
    }

    protected String getAuth(Agent agent) {
        Account account = objectManager.loadResource(Account.class, agent.getAccountId());
        if ( account == null ) {
            throw new IllegalStateException("No account assigned to agent [" + agent.getId() + "]");
        }

        for ( Credential cred : objectManager.children(account, Credential.class) ) {
            if ( "apiKey".equals(cred.getKind()) && CommonStatesConstants.ACTIVE.equals(cred.getState()) ) {
                try {
                    return "Basic " + Base64.encodeBase64String((cred.getPublicValue() + ":" + cred.getSecretValue()).getBytes("UTF-8"));
                } catch (UnsupportedEncodingException e) {
                    throw new IllegalStateException(e);
                }
            }
        }

        throw new IllegalStateException("Failed to find credentials for account [" + account.getId() +
                "] for agent [" + agent.getId() + "]");
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
