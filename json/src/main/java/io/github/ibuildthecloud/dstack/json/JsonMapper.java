package io.github.ibuildthecloud.dstack.json;

import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;
import java.util.Collection;

public interface JsonMapper {

    <T> T readValue(byte[] bytes, Class<T> type) throws IOException;

    <T> T readValue(String text, Class<T> type) throws IOException;

    String writeValueAsString(Object object) throws IOException;

    byte[] writeValueAsBytes(Object data) throws IOException;

    void writeValue(OutputStream baos, Object object) throws IOException;

    @SuppressWarnings("rawtypes")
    <T> T readCollectionValue(String content, Class<? extends Collection> collectionClass, Class<?> elementsClass)
            throws IOException;

    <T> T convertValue(Object fromValue, Class<T> toValueType);

    /**
     * @deprecated
     * 
     * This method provides a toJson() method to match Gson. It is preferable to
     * use writeValueAsString() as that method throws an IOException.
     * 
     * @param object
     * @return
     */
    String toJson(Object object);
    
    /**
     * @deprecated
     * 
     * This method provides a toJson() method to match Gson.
     * 
     * @param object
     * @param os
     */
    void toJson(Object object, Writer os);

    /**
     * @deprecated
     * 
     * This method provides a fromJson() method to match Gson. It is preferable
     * to use readValue() as that method throws an IOException.
     * 
     * @param text
     * @param type
     * @return
     */
    <T> T fromJson(String text, Class<T> type);

}
