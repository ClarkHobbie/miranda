package com.ltsllc.miranda;

import com.ltsllc.commons.LtsllcException;
import com.ltsllc.commons.io.ImprovedFile;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.config.Configurator;
import org.junit.Test;

import java.util.*;

public class MirandaThreadTest {

    @Test
    public void run () throws LtsllcException, InterruptedException {
        Configurator.setRootLevel(Level.DEBUG);

        Miranda miranda = new Miranda();
        miranda.loadProperties();

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
    }
}
