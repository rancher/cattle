package io.cattle.platform.object.util;

import javax.annotation.PostConstruct;

import org.apache.commons.beanutils.BeanUtilsBean;
import org.apache.commons.beanutils.ConvertUtilsBean;
import org.apache.commons.beanutils.converters.IntegerConverter;
import org.apache.commons.beanutils.converters.LongConverter;

public class CommonsConverterStartup {

    @PostConstruct
    public void init() {
        ConvertUtilsBean service = BeanUtilsBean.getInstance().getConvertUtils();
        service.register(new LongConverter(null), Long.class);
        service.register(new IntegerConverter(null), Integer.class);
    }

}
