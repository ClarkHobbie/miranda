package com.ltsllc.miranda.cluster;

import com.ltsllc.commons.LtsllcException;
import com.ltsllc.commons.util.Utils;
import com.ltsllc.miranda.Miranda;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.config.Configurator;
import org.apache.mina.core.session.IoSession;
import org.junit.After;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

class ClusterHandlerTest {
    @BeforeAll
    public static void setup () {
        Configurator.setRootLevel(Level.DEBUG);
    }
    
    @Test
    void messageReceived () throws LtsllcException, IOException {
        Miranda miranda = new Miranda();
        miranda.loadProperties();

        ClusterHandler clusterHandler = new ClusterHandler();
        clusterHandler.setUuid(UUID.fromString("123e4567-e89b-42d3-a456-556642440000"));
        MessageCache messageCache = new MessageCache();
        messageCache.initialize();

        String strMessage = null;
        strMessage = ClusterHandler.MESSAGE;
        strMessage += " ID: ";
        UUID uuid = UUID.randomUUID();
        strMessage += uuid;
        strMessage += " STATUS: HTTP://GOOGLE.COM";
        strMessage += " DELIVERY: HTTP://GOOGLE.COM";
        strMessage += " CONTENTS: ";
        byte[] contents = {1,2,3};
        strMessage += Utils.hexEncode(contents);
        strMessage.toUpperCase();


        //
        // ensure that it runs correctly in the START state
        //
        clusterHandler.setState(ClusterConnectionStates.START);
        IoSession mockIoSession = Mockito.mock(IoSession.class);
        //
        // when(mockIoSession.write("ERROR")).thenReturn(null);
        clusterHandler.setState(ClusterConnectionStates.START);
        clusterHandler.messageReceived(mockIoSession, strMessage);
        assert(clusterHandler.getState() == ClusterConnectionStates.START);

        Mockito.verify(mockIoSession).write("ERROR");


        //
        // when in the START state clusterHandler does respond to the START message
        //
        strMessage = "START";
        clusterHandler.setState(ClusterConnectionStates.START);
        mockIoSession = Mockito.mock(IoSession.class);
        Mockito.when(mockIoSession.write("START")).thenReturn(null);
        clusterHandler.messageReceived(mockIoSession, strMessage);
        assert(clusterHandler.getState() == ClusterConnectionStates.GENERAL);
        Mockito.verify(mockIoSession).write("START");

        //
        // clusterHandler also responds to NEW NODE when in the START state
        //
        strMessage = "NEW NODE 123e4567-e89b-42d3-a456-556642440000";
        clusterHandler.setState(ClusterConnectionStates.START);
        mockIoSession = Mockito.mock(IoSession.class);
        Mockito.when(mockIoSession.write("NEW NODE CONFIRMED 123e4567-e89b-42d3-a456-556642440000")).thenReturn(null);
        clusterHandler.messageReceived(mockIoSession, strMessage);
        System.out.println(clusterHandler.getState());
        assert(clusterHandler.getState() == ClusterConnectionStates.NEW_NODE);
        Mockito.verify(mockIoSession).write("NEW NODE CONFIRMED 123e4567-e89b-42d3-a456-556642440000");
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

        IoSession mockIoSession = Mockito.mock(IoSession.class);
        List<IoSession> nodeList = new ArrayList<>();
        nodeList.add(mockIoSession);

        Cluster cluster = new Cluster();
        cluster.setNodes(nodeList);

        ClusterHandler clusterHandler = new ClusterHandler();
        clusterHandler.sessionClosed(mockIoSession);

        assert (nodeList.size() < 1);
    }


}