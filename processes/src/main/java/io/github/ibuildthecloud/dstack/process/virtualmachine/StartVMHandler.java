package io.github.ibuildthecloud.dstack.process.virtualmachine;

import io.github.ibuildthecloud.dstack.db.jooq.generated.tables.records.InstanceRecord;
import io.github.ibuildthecloud.dstack.engine.handler.ProcessHandler;
import io.github.ibuildthecloud.dstack.engine.process.ProcessInstance;
import io.github.ibuildthecloud.dstack.engine.process.ProcessState;
import io.github.ibuildthecloud.dstack.eventing.model.impl.EventVO;
import io.github.ibuildthecloud.dstack.json.JsonMapper;
import io.github.ibuildthecloud.dstack.transport.Transport;
import io.github.ibuildthecloud.dstack.transport.TransportFactory;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

public class StartVMHandler implements ProcessHandler {

    JsonMapper jsonMapper;
    TransportFactory transportFactory;

    @Override
    public String getName() {
        return "Start VM";
    }

    @Override
    public void handle(ProcessState state, ProcessInstance process) {
        InstanceRecord instance = (InstanceRecord)state.getResource();

        try {
            @SuppressWarnings("unchecked")
            Map<String,Object> instanceData = jsonMapper.convertValue(instance, Map.class);
            Map<String,Object> objData = jsonMapper.readValue(instanceData.get("data").toString());

            @SuppressWarnings("unchecked")
            Map<String,Object> fields = (Map<String,Object>)objData.get("fields");
            objData.put("docker.container.Cmd", fields.get("cmd").toString().split(" "));
            objData.put("docker.container.Image", fields.get("image"));

            instanceData.put("data", objData);

            Map<String,Object> data = new HashMap<String, Object>();
            data.put("instance", instanceData);

            EventVO event = new EventVO();
            event.setName("compute.start");
            event.setData(data);

            Transport transport = transportFactory.getTransport(new HashMap<String, String>());
            transport.connect();
            String stringEvent = jsonMapper.writeValueAsString(event);
            transport.send(stringEvent);
            transport.disconnect();
        } catch ( IOException e ) {
            e.printStackTrace();
        }
    }

    public JsonMapper getJsonMapper() {
        return jsonMapper;
    }

    @Inject
    public void setJsonMapper(JsonMapper jsonMapper) {
        this.jsonMapper = jsonMapper;
    }

    public TransportFactory getTransportFactory() {
        return transportFactory;
    }

    @Inject
    public void setTransportFactory(TransportFactory transportFactory) {
        this.transportFactory = transportFactory;
    }

}
