package io.cattle.platform.app;

import io.cattle.platform.app.components.Framework;
import io.cattle.platform.app.components.Model;

import java.io.File;
import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AppStartup {

    private static final Logger CONSOLE_LOG = LoggerFactory.getLogger("ConsoleStatus");

    public AppStartup() {
        long start = System.currentTimeMillis();
        try {
            init();
            CONSOLE_LOG.info("[DONE ] [{}ms] App initialization succeeded", (System.currentTimeMillis() - start));
        } catch (IOException e) {
            throw new IllegalStateException("Failed to start applicaiton", e);
        }
    }

    public void init() throws IOException {
        Framework framework = new Framework();
        Model model = new Model(framework);
        framework.start();
    }

    protected void mkdirs(File dir) throws IOException {
        if (!dir.exists() && !dir.mkdirs()) {
            throw new IOException("Failed to create directory [" + dir + "]");
        }
    }

    public static void main(String... args) {
        AppStartup startup = new AppStartup();
    }
}
