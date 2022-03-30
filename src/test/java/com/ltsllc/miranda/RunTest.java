package com.ltsllc.miranda;

import com.ltsllc.commons.LtsllcException;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.config.Configurator;
import org.eclipse.jetty.server.Connector;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class RunTest {
    public static final Logger logger = LogManager.getLogger();
     @BeforeAll
     public static void setUp () {
        Configurator.setRootLevel(Level.DEBUG);
     }

    @Test
    public void go () throws Exception {
        Miranda miranda = Miranda.getInstance();
        miranda.loadProperties();
        String[] args = {};
        miranda.startUp(args);

        boolean guard = false;
        System.out.println("guard = " + guard);
        while (guard) {
            miranda.mainLoop();
        }
    }
}
