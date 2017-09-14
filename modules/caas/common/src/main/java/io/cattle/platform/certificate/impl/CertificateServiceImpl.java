package io.cattle.platform.certificate.impl;

import io.cattle.platform.certificate.CertificateService;
import io.cattle.platform.core.constants.NetworkConstants;
import io.cattle.platform.core.constants.ServiceConstants;
import io.cattle.platform.core.dao.DataDao;
import io.cattle.platform.core.model.Service;
import io.cattle.platform.core.model.Stack;
import io.cattle.platform.object.ObjectManager;
import io.cattle.platform.object.util.DataAccessor;
import io.cattle.platform.token.CertSet;
import io.cattle.platform.token.impl.RSAKeyProvider;
import org.apache.commons.codec.binary.Base64;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

public class CertificateServiceImpl implements CertificateService {

    ObjectManager objectManager;
    RSAKeyProvider keyProvider;
    DataDao dataDao;

    public CertificateServiceImpl(ObjectManager objectManager, RSAKeyProvider keyProvider, DataDao dataDao) {
        super();
        this.objectManager = objectManager;
        this.keyProvider = keyProvider;
        this.dataDao = dataDao;
    }

    @Override
    public String getServiceCertificate(final Service service) {
        if (service == null) {
            return null;
        }

        final Stack stack = objectManager.loadResource(Stack.class, service.getStackId());

        if (stack == null) {
            return null;
        }

        String newKey = String.format("service.v3.%d.%s.cert", service.getAccountId(), service.getName());
        return dataDao.getOrCreate(newKey, false, new Callable<String>() {
            @Override
            public String call() throws Exception {
                return generateService(service, stack);
            }
        });
    }

    protected String generateService(Service service, Stack stack) throws Exception {
        @SuppressWarnings("unchecked")
        Map<String, Object> metadata = DataAccessor.fields(service).withKey(ServiceConstants.FIELD_METADATA)
                .withDefault(Collections.EMPTY_MAP).as(Map.class);

        String serviceName = service.getName();
        List<? extends String> configuredSans = DataAccessor.fromMap(metadata).withKey("sans")
            .withDefault(Collections.emptyList()).asList(String.class);
        List<String> sans = new ArrayList<>(configuredSans);

        sans.add(serviceName.toLowerCase());
        sans.add(String.format("%s.%s", serviceName, stack.getName()).toLowerCase());
        sans.add(String.format("%s.%s.%s", serviceName, stack.getName(), NetworkConstants.INTERNAL_DNS_SEARCH_DOMAIN)
                .toLowerCase());

        CertSet certSet = keyProvider.generateCertificate(serviceName, sans.toArray(new String[sans.size()]));
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        certSet.writeZip(baos);

        return Base64.encodeBase64String(baos.toByteArray());
    }

}