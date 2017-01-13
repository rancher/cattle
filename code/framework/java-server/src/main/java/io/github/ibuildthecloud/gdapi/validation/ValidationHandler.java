package io.github.ibuildthecloud.gdapi.validation;

import static io.github.ibuildthecloud.gdapi.validation.ValidationErrorCodes.*;

import io.github.ibuildthecloud.gdapi.context.ApiContext;
import io.github.ibuildthecloud.gdapi.exception.ClientVisibleException;
import io.github.ibuildthecloud.gdapi.factory.SchemaFactory;
import io.github.ibuildthecloud.gdapi.id.IdFormatter;
import io.github.ibuildthecloud.gdapi.model.Action;
import io.github.ibuildthecloud.gdapi.model.Field;
import io.github.ibuildthecloud.gdapi.model.FieldType;
import io.github.ibuildthecloud.gdapi.model.Resource;
import io.github.ibuildthecloud.gdapi.model.Schema;
import io.github.ibuildthecloud.gdapi.model.Schema.Method;
import io.github.ibuildthecloud.gdapi.model.impl.ValidationErrorImpl;
import io.github.ibuildthecloud.gdapi.request.ApiRequest;
import io.github.ibuildthecloud.gdapi.request.handler.AbstractResponseGenerator;
import io.github.ibuildthecloud.gdapi.util.DateUtils;
import io.github.ibuildthecloud.gdapi.util.RequestUtils;
import io.github.ibuildthecloud.gdapi.util.ResponseCodes;
import io.github.ibuildthecloud.gdapi.util.TypeUtils;

import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.PostConstruct;

import org.apache.commons.beanutils.ConvertUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ValidationHandler extends AbstractResponseGenerator {

    private static final Logger log = LoggerFactory.getLogger(ValidationHandler.class);

    ReferenceValidator referenceValidator;
    Set<String> supportedMethods;

    @Override
    public void generate(ApiRequest request) throws IOException {
        ValidationContext context = new ValidationContext();
        context.schemaFactory = request.getSchemaFactory();
        context.idFormatter = ApiContext.getContext().getIdFormatter();
        context.schema = context.schemaFactory.getSchema(request.getType());

        validateId(request, context);
        validateType(request, context);
        validateAction(request, context);
        validateMethod(request, context);
        validateField(request, context);
    }

    protected void validateAction(ApiRequest request, ValidationContext context) {
        String action = request.getAction();
        if (action == null || !Method.POST.isMethod(request.getMethod())) {
            return;
        }

        Map<String, Action> actions = request.getId() == null ? context.schema.getCollectionActions() : context.schema.getResourceActions();

        if (actions == null || !actions.containsKey(action)) {
            error(INVALID_ACTION, Resource.ACTION);
        }

        if (referenceValidator != null && request.getId() != null) {
            Resource resource = referenceValidator.getResourceId(request.getType(), request.getId());
            if (resource == null) {
                error(ResponseCodes.NOT_FOUND);
            }
            if (!resource.getActions().containsKey(action)) {
                error(ACTION_NOT_AVAILABLE, Resource.ACTION);
            }
        }

        String input = actions.get(action).getInput();
        if (input != null) {
            Schema inputSchema = context.schemaFactory.getSchema(input);
            if (inputSchema == null) {
                log.error("Failed to find input schema [{}] for action [{}] on type [{}]", input, action, request.getType());
                error(ResponseCodes.NOT_FOUND);
            } else {
                context.actionSchema = inputSchema;
            }
        }
    }

    protected void validateType(ApiRequest request, ValidationContext context) {
        if (request.getType() != null && context.schema == null) {
            error(ResponseCodes.NOT_FOUND);
        }
    }

    protected void validateField(ApiRequest request, ValidationContext context) {
        if (RequestUtils.isReadMethod(request.getMethod())) {
            validateReadField(request, context);
        } else {
            validateWriteField(request, context);
        }
    }

    protected void validateReadField(ApiRequest request, ValidationContext context) {
        request.setRequestObject(new HashMap<String, Object>());
    }

    protected void validateWriteField(ApiRequest request, ValidationContext context) {
        if (Method.PUT.isMethod(request.getMethod())) {
            validateOperationField(context.schema, request, false, context);
        } else if (Method.POST.isMethod(request.getMethod())) {
            if (request.getAction() == null) {
                validateOperationField(context.schema, request, true, context);
            } else {
                validateOperationField(context.actionSchema, request, true, context);
            }
        }
    }

    protected void validateOperationField(Schema schema, ApiRequest request, boolean create, ValidationContext context) {
        Map<String, Object> input = RequestUtils.toMap(request.getRequestObject());
        Object obj = validateRawOperationField(schema, request.getType(), input, create, context, request.getId());
        if (obj != null) {
            request.setRequestObject(obj);
        }
    }

    protected Object validateRawOperationField(Schema schema, String type, Map<String, Object> input, boolean create, ValidationContext context, String id) {
        if (schema == null) {
            return null;
        }

        Map<String, Object> sanitized = new LinkedHashMap<String, Object>();
        Map<String, Field> fields = schema.getResourceFields();

        for (Map.Entry<String, Object> entry : input.entrySet()) {
            String fieldName = entry.getKey();
            Object value = entry.getValue();

            if (!create && TypeUtils.ID_FIELD.equals(fieldName)) {
                /* For right now, just never let anyone update "id" */
                continue;
            }

            Field field = fields.get(fieldName);
            if (field == null || !isOperation(field, create)) {
                continue;
            }

            boolean wasNull = value == null && (field.isNullable() || !field.hasDefault());
            value = convert(fieldName, field, value, context);

            if (value != null || wasNull) {
                if (value instanceof List) {
                    for (Object individualValue : (List<?>)value) {
                        if (individualValue == null) {
                            error(NOT_NULLABLE, fieldName);
                        }
                        checkFieldCriteria(type, fieldName, field, individualValue, id);
                    }
                } else {
                    checkFieldCriteria(type, fieldName, field, value, id);
                }
                sanitized.put(fieldName, value);
            }
        }

        for (Map.Entry<String, Field> entry : fields.entrySet()) {
            String fieldName = entry.getKey();
            Field field = entry.getValue();

            if (create && !sanitized.containsKey(fieldName) && field.hasDefault()) {
                sanitized.put(fieldName, field.getDefault());
            }

            if (create && isOperation(field, create) && field.isRequired()) {
                if (!sanitized.containsKey(fieldName)) {
                    error(MISSING_REQUIRED, fieldName);
                }

                if (field.getTypeEnum() == FieldType.ARRAY) {
                    List<Object> list = convertArray(fieldName, null, null, sanitized.get(fieldName), context);
                    if (list != null && list.size() == 0) {
                        error(MISSING_REQUIRED, fieldName);
                    }
                }
            }
        }

        return sanitized;
    }

    protected boolean isOperation(Field field, boolean create) {
        return (create && field.isCreate()) || (!create && field.isUpdate());
    }

    protected Object convert(String fieldName, Field field, Object value, ValidationContext context) {
        return convert(fieldName, field, field.getTypeEnum(), field.getSubTypeEnums(), field.getSubTypes(), value, null, context);
    }

    protected Object convert(String fieldName, Field field, FieldType type, List<FieldType> subTypes, List<String> subTypeNames, Object value,
            String lastSubTypeName, ValidationContext context) {

        if (value == null) {
            return value;
        }

        switch (type) {
        case MAP:
            @SuppressWarnings("unchecked")
            Map<String, Object> map = (Map<String, Object>)checkType(fieldName, value, Map.class);
            return convertMap(fieldName, subTypes, subTypeNames, map, context);
        case ARRAY:
            if (subTypes.size() == 0) {
                return error(INVALID_FORMAT, fieldName);
            }
            return convertArray(fieldName, subTypes, subTypeNames, value, context);
        case BLOB:
            return checkType(fieldName, value, InputStream.class);
        case JSON:
            return value;
        case DATE:
        case BOOLEAN:
        case ENUM:
        case FLOAT:
        case INT:
        case PASSWORD:
        case STRING:
            return convertGenericType(fieldName, value, type);
        case REFERENCE:
            if (subTypeNames.size() == 0) {
                return error(INVALID_FORMAT, fieldName);
            }
            return convertReference(subTypeNames.get(0), fieldName, value, context);
        case NONE:
        case TYPE:
            if (field != null) {
                lastSubTypeName = field.getType();
            }
            Map<String, Object> mapValue = RequestUtils.toMap(value);
            Schema schema = context.schemaFactory.getSchema(lastSubTypeName);
            if (schema != null) {
                ValidationContext validationContext = new ValidationContext();
                validationContext.idFormatter = context.idFormatter;
                validationContext.schema = schema;
                validationContext.schemaFactory = context.schemaFactory;
                return validateRawOperationField(schema, lastSubTypeName, mapValue, true, validationContext, null);
            }
        default:
            throw new IllegalStateException("Do not know how to convert type [" + type + "]");
        }
    }

    protected Object convertReference(String type, String fieldName, Object value, ValidationContext context) {
        String id = context.idFormatter.parseId(value.toString());
        if (id == null) {
            error(INVALID_REFERENCE, fieldName);
        }

        if (referenceValidator != null) {
            Object referenced = referenceValidator.getById(type, id);
            if (referenced == null) {
                error(INVALID_REFERENCE, fieldName);
            }
        }

        try {
            /* Attempt to convert to long */
            return new Long(id);
        } catch (NumberFormatException nfe) {
            return id;
        }
    }

    public static Object convertGenericType(String fieldName, Object value, FieldType type) {
        if (FieldType.DATE == type) {
            return convertDate(fieldName, value);
        }

        if (type.getClasses().length == 0)
            return error(INVALID_FORMAT, fieldName);

        Class<?> clz = type.getClasses()[0];
        value = ConvertUtils.convert(value, clz);
        if (value == null || !clz.isAssignableFrom(value.getClass())) {
            return error(INVALID_FORMAT, fieldName);
        }

        return value;
    }

    protected Object checkType(String fieldName, Object value, Class<?> type) {
        if (type.isAssignableFrom(value.getClass())) {
            return value;
        }
        return error(INVALID_FORMAT, fieldName);
    }

    protected Map<String, Object> convertMap(String fieldName, List<FieldType> subTypes, List<String> subTypesNames, Map<String, Object> value,
            ValidationContext context) {
        Map<String, Object> result = new LinkedHashMap<String, Object>();

        if (subTypes == null) {
            result.putAll(value);
            return result;
        }

        FieldType type = subTypes.get(0);
        for (Map.Entry<String, Object> entry : value.entrySet()) {
            Object item =
                    convert(fieldName, null, type, subTypes.subList(1, subTypes.size()), subTypesNames.subList(1, subTypesNames.size()), entry.getValue(),
                            subTypesNames.get(0), context);
            result.put(entry.getKey(), item);
        }

        return result;
    }

    protected List<Object> convertArray(String fieldName, List<FieldType> subTypes, List<String> subTypesNames, Object value, ValidationContext context) {
        List<Object> result = new ArrayList<Object>();
        List<?> items = null;

        if (value instanceof Object[]) {
            items = Arrays.asList(value);
        } else if (value instanceof List) {
            items = (List<?>)value;
        } else {
            items = Arrays.asList(value);
        }

        if (subTypes == null) {
            result.addAll(items);
            return result;
        }

        FieldType type = subTypes.get(0);
        for (Object item : items) {
            item =
                    convert(fieldName, null, type, subTypes.subList(1, subTypes.size()), subTypesNames.subList(1, subTypesNames.size()), item,
                            subTypesNames.get(0), context);
            result.add(item);
        }

        return result;
    }

    public static Object convertDate(String fieldName, Object value) {
        if (value instanceof Date) {
            return value;
        }
        if (value instanceof Number) {
            return new Date(((Number) value).longValue());
        }
        try {
            if (StringUtils.isBlank(value.toString())) {
                return null;
            }
            return DateUtils.parse(value.toString());
        } catch (ParseException e) {
            return error(INVALID_DATE_FORMAT, fieldName);
        }
    }

    protected void checkFieldCriteria(String type, String fieldName, Field field, Object inputValue, String id) {
        Object value = inputValue;
        Number numVal = null;
        String stringValue = null;

        Long minLength = field.getMinLength();
        Long maxLength = field.getMaxLength();
        Long min = field.getMin();
        Long max = field.getMax();
        List<String> options = field.getOptions();
        String validChars = field.getValidChars();
        String invalidChars = field.getInvalidChars();

        if (value == null && field.getDefault() != null) {
            value = field.getDefault();
        }

        if (value instanceof Number) {
            numVal = (Number)value;
        }

        if (value != null) {
            stringValue = value.toString();
        }

        if (value == null && !field.isNullable()) {
            error(NOT_NULLABLE, fieldName);
        }

        if (value != null && field.isUnique() && referenceValidator != null) {
            if (referenceValidator.getByField(type, fieldName, value, id) != null) {
                error(NOT_UNIQUE, fieldName);
            }
        }

        if (numVal != null) {
            if (min != null && numVal.longValue() < min.longValue()) {
                error(MIN_LIMIT_EXCEEDED, fieldName);
            }
            if (max != null && numVal.longValue() > max.longValue()) {
                error(MAX_LIMIT_EXCEEDED, fieldName);
            }
        }

        if (stringValue != null) {
            if (minLength != null && stringValue.length() < minLength.longValue()) {
                error(MIN_LENGTH_EXCEEDED, fieldName);
            }
            if (maxLength != null && stringValue.length() > maxLength.longValue()) {
                error(MAX_LENGTH_EXCEEDED, fieldName);
            }
        }

        if (options != null && options.size() > 0) {
            if (stringValue != null || !field.isNullable()) {
                if (!options.contains(stringValue)) {
                    error(INVALID_OPTION, fieldName);
                }
            }
        }

        if (validChars != null && stringValue != null) {
            if (!stringValue.matches("^[" + validChars + "]*$")) {
                error(INVALID_CHARACTERS, fieldName);
            }
        }

        if (invalidChars != null && stringValue != null) {
            if (stringValue.matches("^[" + invalidChars + "]*$")) {
                error(INVALID_CHARACTERS, fieldName);
            }
        }
    }

    protected void validateId(ApiRequest request, ValidationContext context) {
        String id = request.getId();
        if (id == null) {
            return;
        }

        // TODO should add some property on whether the ID should be formatted
        if (context.schemaFactory.typeStringMatches(Schema.class, request.getType())) {
            return;
        }

        String formattedId = context.idFormatter.parseId(id);
        if (formattedId == null) {
            error(ResponseCodes.NOT_FOUND);
        } else {
            request.setId(formattedId);
        }
    }

    protected void validateMethod(ApiRequest request, ValidationContext context) {
        String method = request.getMethod();

        if (request.getAction() != null && Method.POST.isMethod(method)) {
            return;
        }

        if (method == null || !supportedMethods.contains(method)) {
            error(ResponseCodes.METHOD_NOT_ALLOWED);
        }

        String type = request.getType();
        String id = request.getId();

        if (type == null || context.schema == null) {
            return;
        }

        List<String> allowed = id == null ? context.schema.getCollectionMethods() : context.schema.getResourceMethods();
        if (!allowed.contains(method)) {
            error(ResponseCodes.METHOD_NOT_ALLOWED);
        }
    }

    protected static Object error(String code, String fieldName) {
        ValidationErrorImpl error = new ValidationErrorImpl(code, fieldName);
        throw new ClientVisibleException(error);
    }

    protected Object error(int code) {
        throw new ClientVisibleException(code);
    }

    @PostConstruct
    public void init() {
        if (supportedMethods == null) {
            supportedMethods = new HashSet<String>();
            for (Method m : Method.values()) {
                supportedMethods.add(m.toString());
            }
        }
    }

    public Set<String> getSupportedMethods() {
        return supportedMethods;
    }

    public void setSupportedMethods(Set<String> supportedMethods) {
        this.supportedMethods = supportedMethods;
    }

    protected static final class ValidationContext {
        SchemaFactory schemaFactory;
        Schema schema;
        Schema actionSchema;
        IdFormatter idFormatter;
    }

    public ReferenceValidator getReferenceValidator() {
        return referenceValidator;
    }

    public void setReferenceValidator(ReferenceValidator referenceValidator) {
        this.referenceValidator = referenceValidator;
    }
}
