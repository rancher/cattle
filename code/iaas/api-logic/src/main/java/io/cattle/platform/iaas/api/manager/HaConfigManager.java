package io.cattle.platform.iaas.api.manager;

import io.cattle.platform.archaius.util.ArchaiusUtil;
import io.cattle.platform.core.util.SettingsUtils;
import io.cattle.platform.framework.encryption.impl.Aes256Encrypter;
import io.cattle.platform.object.util.DataAccessor;
import io.cattle.platform.util.type.CollectionUtils;
import io.github.ibuildthecloud.gdapi.context.ApiContext;
import io.github.ibuildthecloud.gdapi.exception.ClientVisibleException;
import io.github.ibuildthecloud.gdapi.factory.SchemaFactory;
import io.github.ibuildthecloud.gdapi.model.ListOptions;
import io.github.ibuildthecloud.gdapi.model.Resource;
import io.github.ibuildthecloud.gdapi.model.impl.ResourceImpl;
import io.github.ibuildthecloud.gdapi.request.ApiRequest;
import io.github.ibuildthecloud.gdapi.request.resource.impl.AbstractNoOpResourceManager;
import io.github.ibuildthecloud.gdapi.url.UrlBuilder;
import io.github.ibuildthecloud.gdapi.util.ResponseCodes;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.lang.ProcessBuilder.Redirect;
import java.net.URL;
import java.security.Key;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.TimeZone;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.IOUtils;

import com.netflix.config.DynamicBooleanProperty;
import com.netflix.config.DynamicIntProperty;
import com.netflix.config.DynamicStringProperty;

import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;

public class HaConfigManager extends AbstractNoOpResourceManager {

    public static DynamicStringProperty DB = ArchaiusUtil.getString("db.cattle.database");
    public static DynamicStringProperty DB_HOST = DB.get().equals("mysql")
            ? ArchaiusUtil.getString("db.cattle.mysql.host")
            : ArchaiusUtil.getString("db.cattle.postgres.host");
    public static DynamicStringProperty DB_PORT = DB.get().equals("mysql")
            ? ArchaiusUtil.getString("db.cattle.mysql.port")
            : ArchaiusUtil.getString("db.cattle.postgres.port");
    public static DynamicStringProperty DB_NAME = DB.get().equals("mysql")
            ? ArchaiusUtil.getString("db.cattle.mysql.name")
            : ArchaiusUtil.getString("db.cattle.postgres.name");
    public static DynamicStringProperty DB_USER = ArchaiusUtil.getString("db.cattle.username");
    public static DynamicStringProperty DB_PASS = ArchaiusUtil.getString("db.cattle.password");
    private static DynamicBooleanProperty HA_ENABLED = ArchaiusUtil.getBoolean("ha.enabled");
    private static DynamicIntProperty HA_CLUSTER_SIZE = ArchaiusUtil.getInt("ha.cluster.size");

    @Inject
    Aes256Encrypter encrypter;
    @Inject
    SettingsUtils settingsUtils;
    Configuration configuration;
    Template template;

    @Override
    protected Object listInternal(SchemaFactory schemaFactory, String type, Map<Object, Object> criteria, ListOptions options) {
        return Arrays.asList(getHaConfig());
    }

    protected Resource getHaConfig() {
        Map<String, Object> data = new HashMap<>();
        String host = DB_HOST.get();
        data.put("dbHost", host);
        data.put("enabled", HA_ENABLED.get());
        data.put("clusterSize", HA_CLUSTER_SIZE.get());

        if ("localhost".equals(host) || "127.0.0.1".equals(host)) {
            try {
                data.put("dbSize", dbSize());
            } catch (NumberFormatException | IOException e) {
                data.put("dbSize", -1);
            }
        }

        ResourceImpl resource = new ResourceImpl("haConfig", "haConfig", data);
        UrlBuilder builder = ApiContext.getUrlBuilder();
        resource.getLinks().put("dbdump", builder.resourceLink(resource, "dbdump"));
        resource.getActions().put("createscript", builder.actionLink(resource, "createscript"));
        return resource;
    }

    @Override
    protected Object getLinkInternal(String type, String id, String link, ApiRequest request) {
        if ("dbdump".equalsIgnoreCase(link)) {
            try {
                return dbDump(request);
            } catch (IOException | InterruptedException e) {
                throw new ClientVisibleException(ResponseCodes.INTERNAL_SERVER_ERROR, "DumpFailed", "Failed to create database dump", e.getMessage());
            }
        }
        return null;
    }

    @Override
    protected Object resourceActionInternal(Object obj, ApiRequest request) {
        if ("createscript".equalsIgnoreCase(request.getAction())) {
            try {
                return getScript(request);
            } catch (IOException | TemplateException e) {
                throw new ClientVisibleException(ResponseCodes.INTERNAL_SERVER_ERROR, "ScriptFailed", "Failed to create script", e.getMessage());
            }
        }
        return null;
    }

    @Override
    protected Object updateInternal(String type, String id, Object obj, ApiRequest request) {
        Map<Object, Object> data = new HashMap<>(CollectionUtils.toMap(request.getRequestObject()));
        Boolean enabled = DataAccessor.fromMap(data).withKey("enabled").as(Boolean.class);
        if (enabled != null) {
            settingsUtils.changeSetting("ha.enabled", enabled.toString());
        }
        return getHaConfig();
    }

    protected Object getScript(ApiRequest request) throws IOException, TemplateException {
        Map<Object, Object> data = new HashMap<>(CollectionUtils.toMap(request.getRequestObject()));
        Key key = encrypter.generateKey();

        Long clusterSize = DataAccessor.fromMap(data).withKey("clusterSize")
                .withDefault(3L).as(Long.class);
        settingsUtils.changeSetting("ha.cluster.size", clusterSize.toString());

        data.put("encryptionKey", Base64.encodeBase64String(key.getEncoded()));
        data.put("db", DB.get());
        data.put("dbHost", DB_HOST.get());
        data.put("dbPort", DB_PORT.get());
        data.put("dbUser", DB_USER.get());
        data.put("dbPass", encrypter.encrypt(DB_PASS.get(), key));
        data.put("dbName", DB_NAME.get());
        data.put("containerPrefix", "rancher-ha-");

        HttpServletResponse response = request.getServletContext().getResponse();

        response.setContentType("application/octet-stream");
        response.setHeader("Content-Disposition", "attachment; filename=rancher-ha.sh");
        response.setHeader("Cache-Control", "private");
        response.setHeader("Pragma", "private");
        response.setHeader("Expires", "Sun 26 Jul 1981 18:42:00 GMT");

        URL url = Thread.currentThread().getContextClassLoader().getResource("haconfig/script.ftl");
        Template template = configuration.getTemplate(url.toExternalForm());

        try (OutputStreamWriter writer = new OutputStreamWriter(request.getOutputStream())) {
            template.process(data, writer);
        }

        return new Object();
    }

    protected long dbSize() throws IOException  {
        ProcessBuilder pb = DB.get().equals("mysql")
                ? new ProcessBuilder("mysql", "--skip-column-names", "-s", "-uroot", "-e",
                "SELECT SUM(data_length)/power(1024,2) AS dbsize_mb FROM information_schema.tables WHERE table_schema='cattle' GROUP BY table_schema;")
                : new ProcessBuilder("psql", "cattle", "cattle", "-t", "-q", "-c",
                "SELECT pg_database_size('cattle')/power(1024,2)");
        pb.redirectError(Redirect.INHERIT);
        Process p = pb.start();
        try (InputStream in = p.getInputStream()) {
            return Long.parseLong(IOUtils.toString(in).split("[.]")[0].trim());
        }
    }

    protected Object dbDump(ApiRequest request) throws IOException, InterruptedException {
        ProcessBuilder pb = DB.get().equals("mysql")
                ? new ProcessBuilder("mysqldump", "-uroot", "cattle")
                : new ProcessBuilder("pg_dump", "-Fc", "-Ucattle", "cattle");

        pb.redirectError(Redirect.INHERIT);

        TimeZone tz = TimeZone.getTimeZone("UTC");
        DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
        df.setTimeZone(tz);
        String now = df.format(new Date());

        HttpServletResponse response = request.getServletContext().getResponse();
        String prefix = "rancher-db-dump-" + now;

        response.setContentType("application/octet-stream");
        response.setHeader("Content-Disposition", "attachment; filename=" + prefix + ".zip");
        response.setHeader("Cache-Control", "private");
        response.setHeader("Pragma", "private");
        response.setHeader("Expires", "Sun 26 Jul 1981 18:42:00 GMT");

        Process p = pb.start();


        try (InputStream in = p.getInputStream(); ZipOutputStream out = new ZipOutputStream(response.getOutputStream())) {
            out.putNextEntry(new ZipEntry(prefix + ".sql"));
            IOUtils.copyLarge(in, out);
            out.closeEntry();
        }

        p.waitFor();

        return new Object();
    }

    @PostConstruct
    public void init() throws IOException {
        URL url = Thread.currentThread().getContextClassLoader().getResource("haconfig/script.ftl");
        template = configuration.getTemplate(url.toExternalForm());
    }

    @Override
    public String[] getTypes() {
        return new String[] {"haConfig"};
    }

    @Override
    public Class<?>[] getTypeClasses() {
        return new Class<?>[0];
    }

    public Configuration getConfiguration() {
        return configuration;
    }

    public void setConfiguration(Configuration configuration) {
        this.configuration = configuration;
    }

}