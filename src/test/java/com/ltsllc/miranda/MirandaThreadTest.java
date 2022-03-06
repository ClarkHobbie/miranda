package com.ltsllc.miranda;

import com.ltsllc.commons.LtsllcException;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.config.Configurator;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

public class MirandaThreadTest {

    @Test
    public void run () throws LtsllcException, InterruptedException {
        Configurator.setRootLevel(Level.DEBUG);

        Miranda miranda = new Miranda();
        miranda.loadProperties();

        List<Message> list = new ArrayList<>();
        miranda.setNewMessageQueue(list);

        list = new ArrayList<>();
        miranda.setSendQueue(list);

        MirandaThread mirandaThread = new MirandaThread();
        mirandaThread.setMiranda(miranda);
        mirandaThread.start();
        synchronized (this) {
            wait(1000);
        }

        mirandaThread.setKeepRunning(false);
        assert (!mirandaThread.isKeepRunning());
    }
}
