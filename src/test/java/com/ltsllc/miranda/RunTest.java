package com.ltsllc.miranda;

import com.ltsllc.commons.LtsllcException;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.config.Configurator;
import org.eclipse.jetty.server.Connector;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class RunTest {
     @BeforeAll
     public static void setUp () {
        Configurator.setRootLevel(Level.DEBUG);
     }

    @Test
    public void go () throws Exception {
        Miranda miranda = new Miranda();
        miranda.loadProperties();
        String[] args = {};
        miranda.startUp(args);
        while (true) {
            miranda.mainLoop();
        }
    }
}
