package io.cattle.platform.app;

import io.cattle.platform.app.components.Api;
import io.cattle.platform.app.components.Backend;
import io.cattle.platform.app.components.Bootstrap;
import io.cattle.platform.app.components.Common;
import io.cattle.platform.app.components.DataAccess;
import io.cattle.platform.app.components.Framework;
import io.cattle.platform.app.components.Model;
import io.cattle.platform.util.exception.ExceptionUtils;
import io.github.ibuildthecloud.gdapi.servlet.ApiRequestFilterDelegate;
import io.github.ibuildthecloud.gdapi.version.Versions;
import org.apache.cloudstack.managed.context.ManagedContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.concurrent.Callable;

public class Cattle {

    private static final Logger CONSOLE_LOG = LoggerFactory.getLogger("ConsoleStatus");

    Api api;
    Bootstrap bootstrap;
    Framework framework;
    Model model;
    DataAccess dataAccess;
    Common common;
    Backend backend;

    public Cattle() {
        long start = System.currentTimeMillis();
        ManagedContext.run(() -> {
            try {
                init();
                CONSOLE_LOG.info("[DONE ] [{}ms] App initialization succeeded", (System.currentTimeMillis() - start));
            } catch (IOException e) {
                throw new IllegalStateException("Failed to start applicaiton", e);
            }
        });
    }

    public void start() {
        ManagedContext.run(backend::start);
    }

    private void init() throws IOException {
        time("BOOTSTRAP ", () -> bootstrap = new Bootstrap());
        time("FRAMEWORK ", () -> framework = new Framework(bootstrap));
        time("MODEL     ", () -> model = new Model(framework));
        time("DATAACCESS", () -> dataAccess = new DataAccess(framework));
        time("COMMON    ", () -> common = new Common(framework, dataAccess));
        time("API       ", () -> api = new Api(framework, common, dataAccess));
        time("BACKEND   ", () -> backend = new Backend(framework, common, dataAccess));
    }

    public static void time(String name, Callable<?> callable) throws IOException {
        long start = System.currentTimeMillis();
        try {
            callable.call();
        } catch (IOException e) {
            throw e;
        } catch (Exception e) {
            ExceptionUtils.rethrowExpectedRuntime(e);
        } finally {
            CONSOLE_LOG.info("[{}] [{}ms] initialization succeeded", name, (System.currentTimeMillis() - start));
        }
    }

    public ApiRequestFilterDelegate getApiRequestFilterDelegate() {
        return api.getApiRequestFilterDelegate();
    }

    public Versions getVersions() {
        return api.getVersions();
    }

}