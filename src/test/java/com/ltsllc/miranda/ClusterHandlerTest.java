package com.ltsllc.miranda;

import com.ltsllc.commons.util.ImprovedRandom;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.config.Configurator;
import org.apache.mina.core.session.IoSession;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.easymock.EasyMock.mock;

class ClusterHandlerTest {
    @BeforeAll
    public static void setup () {
        Configurator.setRootLevel(Level.DEBUG);
    }

    @Test
    void sessionCreated () {
        Configurator.setRootLevel(Level.DEBUG);

        Cluster cluster = new Cluster();
        cluster.setNodes(new ArrayList<>());

        ClusterHandler clusterHandler = new ClusterHandler();
        clusterHandler.sessionCreated(null);

        List<IoSession> list = cluster.getNodes();
        assert (list.size() >0);
    }

    @Test
    public void sessionClosed () throws Exception {
        Configurator.setRootLevel(Level.DEBUG);

        IoSession mockIoSession = mock(IoSession.class);
        List<IoSession> nodeList = new ArrayList<>();
        nodeList.add(mockIoSession);

        Cluster cluster = new Cluster();
        cluster.setNodes(nodeList);

        ClusterHandler clusterHandler = new ClusterHandler();
        clusterHandler.sessionClosed(mockIoSession);

        assert (nodeList.size() < 1);
    }

}