package org.apache.cloudstack.spring.module.context;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.xml.DefaultDocumentLoader;
import org.w3c.dom.Document;
import org.xml.sax.EntityResolver;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;

public class TimedDocumentLoader extends DefaultDocumentLoader {

    private static final Log logger = LogFactory.getLog(TimedDocumentLoader.class);

    private static long time = 0;

    @Override
    public Document loadDocument(InputSource inputSource, EntityResolver entityResolver, ErrorHandler errorHandler,
            int validationMode, boolean namespaceAware) throws Exception {
        long start = System.currentTimeMillis();
        try {
            return super.loadDocument(inputSource, entityResolver, errorHandler, validationMode, namespaceAware);
        } finally {
            long current = System.currentTimeMillis() - start;
            time += current;
            logger.debug("XML processing time [" + current + "] ms, total [" + time + "] ms");
        }
    }

}
