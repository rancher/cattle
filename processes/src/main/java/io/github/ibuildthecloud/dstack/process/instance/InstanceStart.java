package io.github.ibuildthecloud.dstack.process.instance;

import io.github.ibuildthecloud.dstack.core.model.Instance;
import io.github.ibuildthecloud.dstack.core.model.Volume;
import io.github.ibuildthecloud.dstack.engine.handler.HandlerResult;
import io.github.ibuildthecloud.dstack.engine.process.ProcessInstance;
import io.github.ibuildthecloud.dstack.engine.process.ProcessState;
import io.github.ibuildthecloud.dstack.process.base.AbstractDefaultProcessHandler;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.inject.Inject;
import javax.inject.Named;

import com.google.common.util.concurrent.ListeningExecutorService;

@Named
public class InstanceStart extends AbstractDefaultProcessHandler {

    ListeningExecutorService executorService;

    @Override
    public HandlerResult handle(ProcessState state, ProcessInstance process) {
        final Instance instance = (Instance)state.getResource();

        Map<String,Object> result = new ConcurrentHashMap<String,Object>();

        allocate(instance);

//        ListenableFuture<?> storage = executorService.submit(new Runnable() {
//            @Override
//            public void run() {
                storage(instance);
//            }
//        });

//        ListenableFuture<?> networking = executorService.submit(new Runnable() {
//            @Override
//            public void run() {
//
//            }
//        });

//        AsyncUtils.waitAll(storage, networking);

        return new HandlerResult(result);
    }

    protected void allocate(Instance instance) {
        ProcessInstance pi = getObjectProcessManager().createProcessInstance("instance.allocate", instance, null);
        pi.execute();
    }

    protected void storage(Instance instance) {
        List<Volume> volumes = getObjectManager().children(instance, Volume.class);

        for ( Volume volume : volumes ) {
            activate(volume, null);
        }
    }

    public ListeningExecutorService getExecutorService() {
        return executorService;
    }

    @Inject
    public void setExecutorService(ListeningExecutorService executorService) {
        this.executorService = executorService;
    }

}
