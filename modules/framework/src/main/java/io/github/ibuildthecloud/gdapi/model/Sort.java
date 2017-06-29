package io.github.ibuildthecloud.gdapi.model;

import java.net.URL;

import javax.xml.bind.annotation.XmlTransient;

public class Sort {
    public enum SortOrder {
        ASC, DESC;

        private String externalForm;

        private SortOrder() {
            this.externalForm = toString().toLowerCase();
        }

        public String getExternalForm() {
            return externalForm;
        }

        public String getReverseExternalForm() {
            switch (this) {
            case ASC:
                return DESC.getExternalForm();
            default:
                return ASC.getExternalForm();
            }
        }
    }

    String name;
    SortOrder orderEnum = SortOrder.ASC;
    URL reverse;

    public Sort(String name, SortOrder orderEnum, URL reverse) {
        this.name = name;
        this.orderEnum = orderEnum;
        this.reverse = reverse;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getOrder() {
        return orderEnum.getExternalForm();
    }

    @XmlTransient
    public SortOrder getOrderEnum() {
        return orderEnum;
    }

    public void setOrderEnum(SortOrder orderEnum) {
        this.orderEnum = orderEnum;
    }

    public URL getReverse() {
        return reverse;
    }

    public void setReverse(URL reverse) {
        this.reverse = reverse;
    }

}
