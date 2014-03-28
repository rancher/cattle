package io.cattle.platform.agent.connection.ssh;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import org.slf4j.Logger;
import org.slf4j.MDC;

public class LogOutputStream extends OutputStream {

    private static final String AGENT_MDC = "agentId";

    String format;
    String agentId;
    Logger logger;
    boolean error;
    ByteArrayOutputStream baos = new ByteArrayOutputStream();

    public LogOutputStream(Logger logger, String format, String agentId, boolean error) {
        super();
        this.logger = logger;
        this.format = format;
        this.error = error;
        this.agentId = agentId;
    }

    @Override
    public void write(int b) throws IOException {
        if ( b == '\n' ) {
            flush();
        } else {
            baos.write(b);
        }
    }

    @Override
    public void flush() throws IOException {
        if ( baos.size() == 0 ) {
            return;
        }

        String content = new String(baos.toByteArray(), "UTF-8");
        baos.reset();

        if ( ! agentId.equals(MDC.get(AGENT_MDC)) ) {
            MDC.clear();
            MDC.put(AGENT_MDC, agentId);
        }

        if ( error ) {
            logger.error(format, content);
        } else {
            logger.info(format, content);
        }
    }

}
