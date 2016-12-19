package io.cattle.platform.core.cleanup;

import io.cattle.platform.archaius.sources.TransformedEnvironmentProperties;
import io.cattle.platform.archaius.util.ArchaiusUtil;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;

import org.apache.commons.configuration.CompositeConfiguration;
import org.apache.commons.configuration.SystemConfiguration;
import org.jooq.DSLContext;
import org.jooq.conf.Settings;
import org.jooq.impl.DSL;
import org.mariadb.jdbc.Driver;

import com.netflix.config.ConfigurationManager;
import com.netflix.config.DynamicIntProperty;
import com.netflix.config.DynamicStringProperty;

public class Main {

    private static DynamicStringProperty HOST = ArchaiusUtil.getString("db.cattle.mysql.host");
    private static DynamicIntProperty PORT = ArchaiusUtil.getInt("db.cattle.mysql.port");
    private static DynamicStringProperty USER = ArchaiusUtil.getString("db.cattle.username");
    private static DynamicStringProperty PASS = ArchaiusUtil.getString("db.cattle.password");
    private static DynamicStringProperty NAME = ArchaiusUtil.getString("db.cattle.mysql.name");

    public static void main(String... arg) {
        try {
            mainWithError();
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    public static void setup(String name, String value) {
        System.setProperty(name, value);
    }

    public static void runUp(String sql, Connection conn) throws SQLException {
        String idTable = sql.split("\\s+")[1];
        if (idTable.endsWith(",")) {
            idTable = idTable.substring(0, idTable.length()-1);
        }

        long maxId = 0;
        String idSql = "select id from " + idTable + " order by id desc limit 1";
        try (Statement stmt = conn.createStatement()) {
            try (ResultSet rs = stmt.executeQuery(idSql)) {
                rs.next();
                maxId = rs.getLong(1);
            }
        }

        long id = 0;
        long batch = 1000;
        while (id < maxId) {
            id += batch;
            System.out.println("Running for " + id + "/" + maxId);
            run(String.format("%s AND %s.id < %d AND %s.id >= %d", sql, idTable, id, idTable, id-batch), conn);
        }

        run(sql, conn);
    }

    public static void run(String sql, Connection conn) throws SQLException {
        System.out.println("Running: " + sql);
        int count = 0;
        while ((count = runIncremental(sql, conn)) > 0) {
            System.out.println("Updated/Deleted: " + count + " rows");
        }
        System.out.println("Updated/Deleted: " + count + " rows");
    }

    public static int runIncremental(String sql, Connection conn) throws SQLException {
        try (Statement stmt = conn.createStatement()) {
            stmt.executeUpdate(sql);
            return stmt.getUpdateCount();
        }
    }

    public static void mainWithError() throws Exception {
        setup("cleanup.query_limit.rows", "2000");
        setup("main_tables.purge.after.seconds", "600");
        setup("process_instance.purge.after.seconds", "3600");
        setup("events.purge.after.seconds", "3600");
        setup("audit_log.purge.after.seconds", "3600");
        setup("service_log.purge.after.seconds", "3600");
        ConfigurationManager.install(new CompositeConfiguration(Arrays.asList(new TransformedEnvironmentProperties(), new SystemConfiguration())));
        DriverManager.registerDriver(new Driver());
        String url = String.format("jdbc:mysql://%s:%d/%s", HOST.get(), PORT.get(), NAME.get());
        Connection conn = DriverManager.getConnection(url, USER.get(), PASS.get());
        Settings settings = new Settings().withRenderSchema(false);
        DSLContext context = DSL.using(conn, settings);
        TableCleanup cleanup = new TableCleanup();
        cleanup.setConfiguration(context.configuration());

        runUp("DELETE mount FROM mount JOIN volume ON volume.id = mount.volume_id JOIN instance ON instance.id = mount.instance_id WHERE instance.state = 'purged' AND mount.state = 'inactive' AND volume.uri like 'file://%'", conn);
        runUp("DELETE volume_storage_pool_map, volume FROM volume_storage_pool_map JOIN volume ON volume.id = volume_storage_pool_map.volume_id LEFT JOIN mount ON volume.id = mount.volume_id WHERE volume.state in ('inactive', 'active') AND mount.id IS NULL AND volume.uri LIKE 'file://%'", conn);
        runUp("DELETE mount FROM mount WHERE state = 'removed'", conn);
        runUp("DELETE volume_storage_pool_map, volume FROM volume_storage_pool_map JOIN volume ON volume.id = volume_storage_pool_map.volume_id LEFT JOIN mount ON mount.volume_id = volume.id WHERE volume.state = 'purged' AND volume_storage_pool_map.state = 'purged' AND mount.id IS NULL", conn);
        runUp("DELETE volume FROM volume LEFT JOIN volume_storage_pool_map ON volume.id = volume_storage_pool_map.volume_id LEFT JOIN mount ON mount.volume_id = volume.id WHERE volume.state = 'purged' AND mount.id IS NULL AND volume_storage_pool_map.id IS NULL", conn);
        runUp("DELETE mount FROM mount JOIN instance ON instance.id = mount.instance_id JOIN volume ON volume.id = mount.volume_id WHERE instance.state = 'purged' AND volume.state = 'purged' AND volume.uri LIKE 'file://%'", conn);
        runUp("DELETE volume_storage_pool_map FROM volume_storage_pool_map JOIN volume ON volume.id = volume_storage_pool_map.volume_id WHERE volume.state = 'purged'", conn);
        runUp("DELETE instance_host_map FROM instance_host_map JOIN instance ON instance_host_map.instance_id = instance.id WHERE instance.state = 'purged' AND instance_host_map.state = 'inactive'", conn);
        runUp("UPDATE ip_address JOIN host_ip_address_map ON host_ip_address_map.ip_address_id = ip_address.id JOIN host ON host.id = host_ip_address_map.host_id SET ip_address.removed = NOW(), ip_address.state = 'purged' WHERE host.state = 'purged' AND ip_address.removed IS NULL", conn);
        runUp("DELETE host_ip_address_map FROM host_ip_address_map JOIN ip_address ON host_ip_address_map.ip_address_id = ip_address.id JOIN host ON host_ip_address_map.host_id = host.id WHERE host.removed IS NOT NULL", conn);
        runUp("UPDATE image_storage_pool_map JOIN image ON image_storage_pool_map.image_id = image.id LEFT JOIN instance ON instance.image_id = image.id SET image_storage_pool_map.removed = NOW(), image.removed = NOW(), image_storage_pool_map.state = 'purged', image.state = 'purged' WHERE ( instance.state = 'purged' or instance.state IS NULL ) AND image.removed is NULL", conn);
        runUp("UPDATE image LEFT JOIN image_storage_pool_map ON image_storage_pool_map.image_id = image.id LEFT JOIN instance ON instance.image_id = image.id SET image.removed = NOW(), image.state = 'purged' WHERE image_storage_pool_map.id IS NULL AND (instance.removed IS NOT NULL OR instance.id IS NULL) AND image.removed IS NULL", conn);
        runUp("UPDATE storage_pool JOIN storage_pool_host_map ON storage_pool.id = storage_pool_host_map.storage_pool_id JOIN host ON host.id = storage_pool_host_map.host_id SET storage_pool.removed = NOW(), storage_pool_host_map.removed = NOW(), storage_pool.state = 'purged', storage_pool_host_map.state = 'purged' WHERE host.removed IS NOT NULL AND storage_pool.removed IS NULL", conn);
        runUp("UPDATE healthcheck_instance JOIN instance ON instance.id = healthcheck_instance.instance_id SET healthcheck_instance.removed = NOW() WHERE instance.removed IS NOT NULL and healthcheck_instance.removed is NULL", conn);
        runUp("UPDATE instance_link JOIN instance ON instance_link.target_instance_id = instance.id SET instance_link.target_instance_id = NULL WHERE instance.removed IS NOT NULL AND instance_link.removed IS NULL", conn);
        runUp("DELETE project_member FROM project_member JOIN account ON project_member.project_id = account.id WHERE account.removed IS NOT NULL", conn);
        runUp("DELETE network_service_provider_instance_map FROM network_service_provider_instance_map JOIN instance ON instance.id = network_service_provider_instance_map.instance_id WHERE instance.state = 'purged'", conn);
        runUp("DELETE instance_label_map FROM instance_label_map JOIN instance ON instance.id = instance_label_map.instance_id WHERE instance.state = 'purged'", conn);

        int count = 0;
        while ((count = cleanup.runWithCount()) > 0) {
            System.out.println("Deleted: " + count + " rows");
        }
        System.out.println("Deleted: " + count + " rows");
        System.out.println("FINISHED CLEANUP");
    }

}
