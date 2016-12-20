package io.cattle.platform.core.dao.impl;

import static io.cattle.platform.core.model.tables.AccountTable.*;
import static io.cattle.platform.core.model.tables.CertificateTable.*;

import io.cattle.platform.core.constants.CommonStatesConstants;
import io.cattle.platform.core.dao.CertificateDao;
import io.cattle.platform.db.jooq.dao.impl.AbstractJooqDao;

import javax.inject.Named;

import org.jooq.Record1;

@Named
public class CertificateDaoImpl extends AbstractJooqDao implements CertificateDao {

    @Override
    public String getPublicCA() {
        Record1<String> r = create().select(CERTIFICATE.CERT_CHAIN)
                .from(CERTIFICATE)
                .join(ACCOUNT)
                    .on(ACCOUNT.ID.eq(CERTIFICATE.ACCOUNT_ID))
                .where(CERTIFICATE.STATE.eq(CommonStatesConstants.ACTIVE)
                        .and(ACCOUNT.UUID.like("system-ha-%")))
                .fetchAny();

        return r == null ? null : r.value1();
    }

}