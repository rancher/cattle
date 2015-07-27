package io.github.ibuildthecloud.gdapi.request.handler;

import io.github.ibuildthecloud.gdapi.request.ApiRequest;
import io.github.ibuildthecloud.gdapi.util.RequestUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;

public class RequestOptionsParser extends AbstractApiRequestHandler {

    private static final String DEFAULT_VALUE = "true";

    List<String> options = new ArrayList<String>();

    @Override
    public void handle(ApiRequest request) throws IOException {
        Map<String, String> requestOptions = request.getOptions();
        Map<String, Object> input = RequestUtils.toMap(request.getRequestObject());

        for (String option : getOptions()) {
            if (input.containsKey(option)) {
                Object value = RequestUtils.makeSingular(input.get(option));
                String stringValue = DEFAULT_VALUE;

                if (value != null && !StringUtils.isBlank(value.toString())) {
                    stringValue = value.toString();
                }

                requestOptions.put(option, stringValue);
            }
        }
    }

    public List<String> getOptions() {
        return options;
    }

    public void setOptions(List<String> options) {
        this.options = options;
    }

}
