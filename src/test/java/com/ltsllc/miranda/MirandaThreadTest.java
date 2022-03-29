package com.ltsllc.miranda;

import com.ltsllc.commons.LtsllcException;
import com.ltsllc.commons.io.ImprovedFile;
import com.ltsllc.miranda.cluster.Cluster;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.config.Configurator;
import org.junit.Test;

import java.util.*;

public class MirandaThreadTest {

    @Test
    public void run () throws LtsllcException, InterruptedException {
        Configurator.setRootLevel(Level.DEBUG);
        ImprovedFile temp = new ImprovedFile("message.log");
        ImprovedFile owners = new ImprovedFile("owners.log");

        try {
            Miranda miranda = new Miranda();
            miranda.loadProperties();

            MessageLog.defineStatics(temp, 1000000, owners);

            ImprovedFile messageLog = new ImprovedFile("messages.log");
            ImprovedFile ownersLog = new ImprovedFile("owner.log");

            MessageLog.getInstance().clearAllMessages(messageLog, 104857600, ownersLog);

            MirandaThread mirandaThread = new MirandaThread();
            mirandaThread.setMiranda(miranda);
            mirandaThread.start();
            synchronized (this) {
                wait(1000);
            }

            mirandaThread.setKeepRunning(false);
            assert (!mirandaThread.isKeepRunning());
            //
            // TODO: check for other cases
            //
        } finally {
            if (temp.exists()) {
                temp.delete();
            }

            if (owners.exists()) {
                owners.delete();
            }
        }
    }
}
