package io.cattle.platform.agent.connection.ssh;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.apache.sshd.client.channel.ChannelExec;

public class EofAwareChannelExec extends ChannelExec {

    List<Runnable> eofHandler = new CopyOnWriteArrayList<Runnable>();

    public EofAwareChannelExec(String command) {
        super(command);
    }

    @Override
    public void handleEof() throws IOException {
        super.handleEof();

        for ( Runnable run : eofHandler ) {
            run.run();
        }
    }

    public void onEof(Runnable run) {
        eofHandler.add(run);
    }

}
