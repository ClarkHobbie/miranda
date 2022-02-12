package com.ltsllc.miranda;

import com.ltsllc.commons.util.ImprovedRandom;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.config.Configurator;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.*;

class ClusterHandlerTest {
    @BeforeAll
    public static void setup () {
        Configurator.setRootLevel(Level.DEBUG);
    }
    @Test
    void newConnection () {
        Cluster cluster = new Cluster();
        cluster.setNodes(new ArrayList<>());
        ImprovedRandom improvedRandom = new ImprovedRandom();
        Cluster.setRandomNumberGenerator(improvedRandom);

        ClusterHandler clusterHandler = new ClusterHandler();
        clusterHandler.sessionCreated(null);
    }

}