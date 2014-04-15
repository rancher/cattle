package io.cattle.platform.object.serialization.impl;

import io.cattle.platform.json.JsonMapper;
import io.cattle.platform.object.ObjectManager;
import io.cattle.platform.object.meta.ObjectMetaDataManager;
import io.cattle.platform.object.serialization.ObjectSerializer;
import io.cattle.platform.object.serialization.ObjectSerializerFactory;
import io.cattle.platform.object.serialization.ObjectTypeSerializerPostProcessor;
import io.cattle.platform.util.type.InitializationTask;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import javax.inject.Inject;

public class DefaultObjectSerializerFactoryImpl implements ObjectSerializerFactory, InitializationTask {

    private static final Pattern GOOD_CHARS = Pattern.compile("[a-z0-9]", Pattern.CASE_INSENSITIVE);

    JsonMapper jsonMapper;
    ObjectManager objectManager;
    ObjectMetaDataManager metaDataManager;
    Map<String,ObjectTypeSerializerPostProcessor> postProcessorsMap = new HashMap<String, ObjectTypeSerializerPostProcessor>();
    List<ObjectTypeSerializerPostProcessor> postProcessors;

    @Override
    public ObjectSerializer compile(String type, String expression) {
        Action action = parseAction(type, expression);
        DefaultObjectSerializerImpl result =
                new DefaultObjectSerializerImpl(jsonMapper, objectManager, metaDataManager, postProcessorsMap, action, expression);
        result.serialize(null);

        return result;
    }

    @Override
    public void start() {
        for ( ObjectTypeSerializerPostProcessor postProcessor : postProcessors ) {
            for ( String type : postProcessor.getTypes() ) {
                postProcessorsMap.put(type, postProcessor);
            }
        }
    }

    @Override
    public void stop() {
    }

    protected Action parseAction(String type, String expression) {
        /* Should use antlr if this gets too complicated */
        Context c = new Context();

        for ( int i = 0 ; i < expression.length() ; i++ ) {
            char current = expression.charAt(i);
            switch (current) {
            case '|':
            case ',':
                c = endWord(c, current);
                c.done = false;
                break;
            case '.':
                c.dotStart = true;
                c = c.push();
                break;
            case '[':
                c = c.push();
                break;
            case ']':
                c = endWord(c, current);
                c = c.pop(expression);
                break;
            default:
                if ( ! GOOD_CHARS.matcher(Character.toString(current)).matches() || c.done ) {
                    throw new IllegalArgumentException("Bad character [" + current + "] found in [" + expression + "]");
                }
                c.currentWord.append(current);
                break;
            }
        }

        if ( c.currentWord.length() > 0 ) {
            c = endWord(c, ' ');
        }

        if ( c.parent != null ) {
            throw new IllegalStateException("Missing closing ']' in [" + expression + "]");
        }

        return new Action(type, c.currentActions);
    }

    protected static Context endWord(Context c, char current) {
        if ( c.currentWord.length() == 0 && c.currentAction.getName() == null ) {
            throw new IllegalStateException("got [" + current + "] but there is no word before it");
        }

        c.currentAction.setName(c.currentWord.toString());
        c.currentActions.add(c.currentAction);
        c.currentAction = new Action();
        c.currentWord.setLength(0);

        if ( current != '.' ) {
            if ( c.parent != null && c.parent.dotStart ) {
                return endWord(c.parent, current);
            }
        }

        return c;
    }

    private static final class Context {
        Context parent = null;
        List<Action> currentActions = new ArrayList<Action>();
        Action currentAction = new Action();
        StringBuilder currentWord = new StringBuilder();
        boolean dotStart = false;
        boolean done = false;

        Context push() {
            Context child = new Context();
            child.parent = this;
            currentAction.setChildren(child.currentActions);
            this.done = true;
            return child;
        }

        Context pop(String expression) {
            if ( parent == null ) {
                throw new IllegalStateException("To many closing braces in [" + expression + "]");
            }

            return parent;
        }

        @Override
        public String toString() {
            Context current = this;
            while ( current.parent != null ) {
                current = current.parent;
            }
            if ( current.currentWord.length() > 0 ) {
                List<Action> actions = new ArrayList<Action>(current.currentActions);
                actions.add(new Action(current.currentWord.toString() + "*", current.currentAction.getChildren()));
                return actions.toString();
            } else {
                return current.currentActions.toString();
            }
        }
    }

    public JsonMapper getJsonMapper() {
        return jsonMapper;
    }

    @Inject
    public void setJsonMapper(JsonMapper jsonMapper) {
        this.jsonMapper = jsonMapper;
    }

    public ObjectManager getObjectManager() {
        return objectManager;
    }

    @Inject
    public void setObjectManager(ObjectManager objectManager) {
        this.objectManager = objectManager;
    }

    public ObjectMetaDataManager getMetaDataManager() {
        return metaDataManager;
    }

    @Inject
    public void setMetaDataManager(ObjectMetaDataManager metaDataManager) {
        this.metaDataManager = metaDataManager;
    }

    public List<ObjectTypeSerializerPostProcessor> getPostProcessors() {
        return postProcessors;
    }

    @Inject
    public void setPostProcessors(List<ObjectTypeSerializerPostProcessor> postProcessors) {
        this.postProcessors = postProcessors;
    }

}
