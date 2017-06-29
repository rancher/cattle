package io.cattle.platform.api.handler;

import io.cattle.platform.engine.process.ExitReason;
import io.cattle.platform.engine.process.ProcessInstanceException;
import io.cattle.platform.engine.process.impl.ProcessCancelException;
import io.cattle.platform.engine.process.impl.ProcessExecutionExitException;
import io.cattle.platform.lock.exception.FailedToAcquireLockException;
import io.cattle.platform.util.exception.ExceptionUtils;
import io.github.ibuildthecloud.gdapi.exception.ClientVisibleException;
import io.github.ibuildthecloud.gdapi.request.ApiRequest;
import io.github.ibuildthecloud.gdapi.request.handler.ApiRequestHandler;
import io.github.ibuildthecloud.gdapi.util.ResponseCodes;

import java.io.IOException;

import javax.servlet.ServletException;

import org.jooq.exception.DataAccessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CommonExceptionsHandler implements ApiRequestHandler {

    private static final Logger log = LoggerFactory.getLogger(CommonExceptionsHandler.class);

    @Override
    public void handle(ApiRequest request) throws IOException {
    }

    @Override
    public boolean handleException(ApiRequest request, Throwable t) throws IOException, ServletException {
        if (t instanceof ProcessInstanceException) {
            ProcessInstanceException e = (ProcessInstanceException) t;
            if (e.getExitReason() == ExitReason.RESOURCE_BUSY || e.getExitReason() == ExitReason.CANCELED) {
                throw new ClientVisibleException(ResponseCodes.CONFLICT);
            }
            t = ExceptionUtils.getRootCause(t);
        }

        if (t instanceof ProcessExecutionExitException && ((ProcessExecutionExitException) t).getExitReason() == ExitReason.RESOURCE_BUSY) {
            log.info("Resource busy : {}", t.getMessage());
            throw new ClientVisibleException(ResponseCodes.CONFLICT);
        } else if (t instanceof ProcessExecutionExitException &&
                ((ProcessExecutionExitException) t).getExitReason() == ExitReason.PROCESS_ALREADY_IN_PROGRESS) {
            log.info("Process in progress : {}", t.getMessage());
            throw new ClientVisibleException(ResponseCodes.CONFLICT);
        } else if (t instanceof ProcessExecutionExitException &&
                ((ProcessExecutionExitException) t).getExitReason() == ExitReason.STATE_CHANGED) {
            log.info("State changed: {}", t.getMessage());
            throw new ClientVisibleException(ResponseCodes.CONFLICT);
        } else if (t instanceof FailedToAcquireLockException) {
            log.info("Failed to lock : {}", t.getMessage());
            throw new ClientVisibleException(ResponseCodes.CONFLICT);
        } else if (t instanceof ProcessCancelException) {
            log.info("Process cancel : {}", t.getMessage());
            throw new ClientVisibleException(ResponseCodes.CONFLICT);
        } else if (t instanceof DataAccessException) {
            log.info("Database error : {}", t.getMessage());
            throw new ClientVisibleException(ResponseCodes.CONFLICT);
        }

        return false;
    }


}
