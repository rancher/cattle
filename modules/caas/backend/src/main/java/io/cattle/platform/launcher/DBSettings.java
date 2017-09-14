package io.cattle.platform.launcher;

import com.netflix.config.DynamicStringProperty;
import io.cattle.platform.archaius.util.ArchaiusUtil;

public class DBSettings {

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

}