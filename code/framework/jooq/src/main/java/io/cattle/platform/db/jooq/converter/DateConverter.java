package io.cattle.platform.db.jooq.converter;

import java.sql.Timestamp;
import java.util.Date;

import org.jooq.Converter;

public class DateConverter implements Converter<Timestamp, Date> {

    private static final long serialVersionUID = -3093938632174221235L;

    @Override
    public Date from(Timestamp databaseObject) {
        if (databaseObject == null) {
            return null;
        }
        return new Date(databaseObject.getTime());
    }

    @Override
    public Timestamp to(Date userObject) {
        if (userObject == null) {
            return null;
        }
        return new Timestamp(userObject.getTime());
    }

    @Override
    public Class<Timestamp> fromType() {
        return Timestamp.class;
    }

    @Override
    public Class<Date> toType() {
        return Date.class;
    }

}
