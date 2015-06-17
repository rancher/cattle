package io.github.ibuildthecloud.gdapi.context;

import io.github.ibuildthecloud.gdapi.factory.SchemaFactory;
import io.github.ibuildthecloud.gdapi.id.IdFormatter;
import io.github.ibuildthecloud.gdapi.id.IdentityFormatter;
import io.github.ibuildthecloud.gdapi.request.ApiRequest;
import io.github.ibuildthecloud.gdapi.url.DefaultUrlBuilder;
import io.github.ibuildthecloud.gdapi.url.NullUrlBuilder;
import io.github.ibuildthecloud.gdapi.url.UrlBuilder;
import io.github.ibuildthecloud.gdapi.util.TransformationService;

public class ApiContext {

    private static final ThreadLocal<ApiContext> TL = new ThreadLocal<ApiContext>();

    ApiRequest apiRequest;
    IdFormatter idFormatter = new IdentityFormatter();
    Object policy;

    TransformationService transformationService;

    protected ApiContext() {
        super();
    }

    public static ApiContext getContext() {
        return TL.get();
    }

    public static SchemaFactory getSchemaFactory() {
        return getContext().getApiRequest().getSchemaFactory();
    }

    public static ApiContext newContext() {
        ApiContext context = new ApiContext();
        TL.set(context);
        return context;
    }

    public static void remove() {
        TL.remove();
    }

    public static UrlBuilder getUrlBuilder() {
        ApiContext context = ApiContext.getContext();
        if (context != null) {
            UrlBuilder writer = context.getApiRequest().getUrlBuilder();
            if (writer == null) {
                DefaultUrlBuilder urlWriter = new DefaultUrlBuilder(context.getApiRequest(), ApiContext.getSchemaFactory());
                String subContext = context.getApiRequest().getSubContext();
                if (subContext != null) {
                    urlWriter.setSubContext(subContext);
                }

                writer = urlWriter;
                context.getApiRequest().setUrlBuilder(writer);
            }
            return writer;
        }
        return new NullUrlBuilder();
    }

    public ApiRequest getApiRequest() {
        return apiRequest;
    }

    public void setApiRequest(ApiRequest apiRequest) {
        this.apiRequest = apiRequest;
    }

    public Object getPolicy() {
        return policy;
    }

    public void setPolicy(Object policy) {
        this.policy = policy;
    }

    public IdFormatter getIdFormatter() {
        return idFormatter;
    }

    public void setIdFormatter(IdFormatter idFormatter) {
        this.idFormatter = idFormatter;
    }

    public TransformationService getTransformationService() {
        return this.transformationService;
    }

    public void setTransformationService(TransformationService transformationService) {
        this.transformationService = transformationService;
    }
}
