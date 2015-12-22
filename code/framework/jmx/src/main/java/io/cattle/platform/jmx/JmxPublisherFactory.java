package io.cattle.platform.jmx;

import io.cattle.platform.archaius.util.ArchaiusUtil;
import io.cattle.platform.server.context.ServerContext;

import java.io.InputStream;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.jmxtrans.embedded.EmbeddedJmxTrans;
import org.jmxtrans.embedded.config.ConfigurationParser;
import org.jmxtrans.embedded.output.AbstractOutputWriter;
import org.jmxtrans.embedded.output.GraphiteWriter;

import com.netflix.config.DynamicIntProperty;
import com.netflix.config.DynamicStringMapProperty;
import com.netflix.config.DynamicStringProperty;

public class JmxPublisherFactory {

    private static final DynamicStringProperty GRAPHITE_HOST = ArchaiusUtil.getString("graphite.host");
    private static final DynamicIntProperty JMX_TRANS_EXPORT_INTERVAL = ArchaiusUtil.getInt("jmx.trans.export.interval.seconds");
    private static final DynamicIntProperty JMX_TRANS_QUERY_INTERVAL = ArchaiusUtil.getInt("jmx.trans.query.interval.seconds");

    private static final DynamicIntProperty GRAPHITE_PORT = ArchaiusUtil.getInt("graphite.port");
    private static final DynamicStringMapProperty GRAPHITE_OPTIONS = new DynamicStringMapProperty("graphite.options", (String) null);

    List<URL> resources;
    EmbeddedJmxTrans jmxTrans;

    @PostConstruct
    public void init() throws Exception {
        ConfigurationParser parser = new ConfigurationParser();
        jmxTrans = new EmbeddedJmxTrans();
        jmxTrans.setExportIntervalInSeconds(JMX_TRANS_EXPORT_INTERVAL.get());
        jmxTrans.setQueryIntervalInSeconds(JMX_TRANS_QUERY_INTERVAL.get());

        for (URL resource : resources) {
            InputStream is = null;
            try {
                is = resource.openStream();
                parser.mergeEmbeddedJmxTransConfiguration(is, jmxTrans);
            } finally {
                IOUtils.closeQuietly(is);
            }
        }

        if (!StringUtils.isBlank(GRAPHITE_HOST.get())) {
            Map<String, Object> config = new HashMap<String, Object>();
            config.put(AbstractOutputWriter.SETTING_HOST, GRAPHITE_HOST.get());
            config.put(AbstractOutputWriter.SETTING_PORT, GRAPHITE_PORT.get());

            for (Map.Entry<String, String> entry : GRAPHITE_OPTIONS.getMap().entrySet()) {
                config.put(entry.getKey(), entry.getValue());
            }

            if (!config.containsKey(AbstractOutputWriter.SETTING_NAME_PREFIX) && !StringUtils.isBlank(ServerContext.SERVER_ID.get())) {
                config.put(AbstractOutputWriter.SETTING_NAME_PREFIX, "servers." + ServerContext.SERVER_ID.get());
            }

            GraphiteWriter writer = new GraphiteWriter();
            writer.setEnabled(true);
            writer.setSettings(config);

            jmxTrans.getOutputWriters().add(writer);
        }

        jmxTrans.start();
    }

    public List<URL> getResources() {
        return resources;
    }

    public void setResources(List<URL> resources) {
        this.resources = resources;
    }

}
