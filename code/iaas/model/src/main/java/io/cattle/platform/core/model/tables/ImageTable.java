/**
 * This class is generated by jOOQ
 */
package io.cattle.platform.core.model.tables;

/**
 * This class is generated by jOOQ.
 */
@javax.annotation.Generated(value = { "http://www.jooq.org", "3.3.0" }, comments = "This class is generated by jOOQ")
@java.lang.SuppressWarnings({ "all", "unchecked", "rawtypes" })
public class ImageTable extends org.jooq.impl.TableImpl<io.cattle.platform.core.model.tables.records.ImageRecord> {

    private static final long serialVersionUID = 1597183362;

    /**
     * The singleton instance of <code>cattle.image</code>
     */
    public static final io.cattle.platform.core.model.tables.ImageTable IMAGE = new io.cattle.platform.core.model.tables.ImageTable();

    /**
     * The class holding records for this type
     */
    @Override
    public java.lang.Class<io.cattle.platform.core.model.tables.records.ImageRecord> getRecordType() {
        return io.cattle.platform.core.model.tables.records.ImageRecord.class;
    }

    /**
     * The column <code>cattle.image.id</code>.
     */
    public final org.jooq.TableField<io.cattle.platform.core.model.tables.records.ImageRecord, java.lang.Long> ID = createField("id",
            org.jooq.impl.SQLDataType.BIGINT.nullable(false), this, "");

    /**
     * The column <code>cattle.image.name</code>.
     */
    public final org.jooq.TableField<io.cattle.platform.core.model.tables.records.ImageRecord, java.lang.String> NAME = createField("name",
            org.jooq.impl.SQLDataType.VARCHAR.length(255), this, "");

    /**
     * The column <code>cattle.image.account_id</code>.
     */
    public final org.jooq.TableField<io.cattle.platform.core.model.tables.records.ImageRecord, java.lang.Long> ACCOUNT_ID = createField("account_id",
            org.jooq.impl.SQLDataType.BIGINT, this, "");

    /**
     * The column <code>cattle.image.kind</code>.
     */
    public final org.jooq.TableField<io.cattle.platform.core.model.tables.records.ImageRecord, java.lang.String> KIND = createField("kind",
            org.jooq.impl.SQLDataType.VARCHAR.length(255).nullable(false), this, "");

    /**
     * The column <code>cattle.image.uuid</code>.
     */
    public final org.jooq.TableField<io.cattle.platform.core.model.tables.records.ImageRecord, java.lang.String> UUID = createField("uuid",
            org.jooq.impl.SQLDataType.VARCHAR.length(128).nullable(false), this, "");

    /**
     * The column <code>cattle.image.description</code>.
     */
    public final org.jooq.TableField<io.cattle.platform.core.model.tables.records.ImageRecord, java.lang.String> DESCRIPTION = createField("description",
            org.jooq.impl.SQLDataType.VARCHAR.length(1024), this, "");

    /**
     * The column <code>cattle.image.state</code>.
     */
    public final org.jooq.TableField<io.cattle.platform.core.model.tables.records.ImageRecord, java.lang.String> STATE = createField("state",
            org.jooq.impl.SQLDataType.VARCHAR.length(128).nullable(false), this, "");

    /**
     * The column <code>cattle.image.created</code>.
     */
    public final org.jooq.TableField<io.cattle.platform.core.model.tables.records.ImageRecord, java.util.Date> CREATED = createField("created",
            org.jooq.impl.SQLDataType.TIMESTAMP.asConvertedDataType(new io.cattle.platform.db.jooq.converter.DateConverter()), this, "");

    /**
     * The column <code>cattle.image.removed</code>.
     */
    public final org.jooq.TableField<io.cattle.platform.core.model.tables.records.ImageRecord, java.util.Date> REMOVED = createField("removed",
            org.jooq.impl.SQLDataType.TIMESTAMP.asConvertedDataType(new io.cattle.platform.db.jooq.converter.DateConverter()), this, "");

    /**
     * The column <code>cattle.image.remove_time</code>.
     */
    public final org.jooq.TableField<io.cattle.platform.core.model.tables.records.ImageRecord, java.util.Date> REMOVE_TIME = createField("remove_time",
            org.jooq.impl.SQLDataType.TIMESTAMP.asConvertedDataType(new io.cattle.platform.db.jooq.converter.DateConverter()), this, "");

    /**
     * The column <code>cattle.image.data</code>.
     */
    public final org.jooq.TableField<io.cattle.platform.core.model.tables.records.ImageRecord, java.util.Map<String, Object>> DATA = createField("data",
            org.jooq.impl.SQLDataType.CLOB.length(65535).asConvertedDataType(new io.cattle.platform.db.jooq.converter.DataConverter()), this, "");

    /**
     * The column <code>cattle.image.url</code>.
     */
    public final org.jooq.TableField<io.cattle.platform.core.model.tables.records.ImageRecord, java.lang.String> URL = createField("url",
            org.jooq.impl.SQLDataType.VARCHAR.length(255), this, "");

    /**
     * The column <code>cattle.image.is_public</code>.
     */
    public final org.jooq.TableField<io.cattle.platform.core.model.tables.records.ImageRecord, java.lang.Boolean> IS_PUBLIC = createField("is_public",
            org.jooq.impl.SQLDataType.BIT.nullable(false).defaulted(true), this, "");

    /**
     * The column <code>cattle.image.physical_size_mb</code>.
     */
    public final org.jooq.TableField<io.cattle.platform.core.model.tables.records.ImageRecord, java.lang.Long> PHYSICAL_SIZE_MB = createField(
            "physical_size_mb", org.jooq.impl.SQLDataType.BIGINT, this, "");

    /**
     * The column <code>cattle.image.virtual_size_mb</code>.
     */
    public final org.jooq.TableField<io.cattle.platform.core.model.tables.records.ImageRecord, java.lang.Long> VIRTUAL_SIZE_MB = createField("virtual_size_mb",
            org.jooq.impl.SQLDataType.BIGINT, this, "");

    /**
     * The column <code>cattle.image.checksum</code>.
     */
    public final org.jooq.TableField<io.cattle.platform.core.model.tables.records.ImageRecord, java.lang.String> CHECKSUM = createField("checksum",
            org.jooq.impl.SQLDataType.VARCHAR.length(255), this, "");

    /**
     * The column <code>cattle.image.format</code>.
     */
    public final org.jooq.TableField<io.cattle.platform.core.model.tables.records.ImageRecord, java.lang.String> FORMAT = createField("format",
            org.jooq.impl.SQLDataType.VARCHAR.length(255), this, "");

    /**
     * The column <code>cattle.image.instance_kind</code>.
     */
    public final org.jooq.TableField<io.cattle.platform.core.model.tables.records.ImageRecord, java.lang.String> INSTANCE_KIND = createField("instance_kind",
            org.jooq.impl.SQLDataType.VARCHAR.length(255), this, "");

    /**
     * Create a <code>cattle.image</code> table reference
     */
    public ImageTable() {
        this("image", null);
    }

    /**
     * Create an aliased <code>cattle.image</code> table reference
     */
    public ImageTable(java.lang.String alias) {
        this(alias, io.cattle.platform.core.model.tables.ImageTable.IMAGE);
    }

    private ImageTable(java.lang.String alias, org.jooq.Table<io.cattle.platform.core.model.tables.records.ImageRecord> aliased) {
        this(alias, aliased, null);
    }

    private ImageTable(java.lang.String alias, org.jooq.Table<io.cattle.platform.core.model.tables.records.ImageRecord> aliased, org.jooq.Field<?>[] parameters) {
        super(alias, io.cattle.platform.core.model.CattleTable.CATTLE, aliased, parameters, "");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public org.jooq.Identity<io.cattle.platform.core.model.tables.records.ImageRecord, java.lang.Long> getIdentity() {
        return io.cattle.platform.core.model.Keys.IDENTITY_IMAGE;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public org.jooq.UniqueKey<io.cattle.platform.core.model.tables.records.ImageRecord> getPrimaryKey() {
        return io.cattle.platform.core.model.Keys.KEY_IMAGE_PRIMARY;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public java.util.List<org.jooq.UniqueKey<io.cattle.platform.core.model.tables.records.ImageRecord>> getKeys() {
        return java.util.Arrays.<org.jooq.UniqueKey<io.cattle.platform.core.model.tables.records.ImageRecord>> asList(
                io.cattle.platform.core.model.Keys.KEY_IMAGE_PRIMARY, io.cattle.platform.core.model.Keys.KEY_IMAGE_IDX_IMAGE_UUID);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public java.util.List<org.jooq.ForeignKey<io.cattle.platform.core.model.tables.records.ImageRecord, ?>> getReferences() {
        return java.util.Arrays
                .<org.jooq.ForeignKey<io.cattle.platform.core.model.tables.records.ImageRecord, ?>> asList(io.cattle.platform.core.model.Keys.FK_IMAGE__ACCOUNT_ID);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public io.cattle.platform.core.model.tables.ImageTable as(java.lang.String alias) {
        return new io.cattle.platform.core.model.tables.ImageTable(alias, this);
    }

    /**
     * Rename this table
     */
    public io.cattle.platform.core.model.tables.ImageTable rename(java.lang.String name) {
        return new io.cattle.platform.core.model.tables.ImageTable(name, null);
    }
}
