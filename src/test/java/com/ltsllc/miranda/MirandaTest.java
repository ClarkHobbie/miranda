package com.ltsllc.miranda;

import com.ltsllc.commons.LtsllcException;
import com.ltsllc.commons.util.ImprovedProperties;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.config.Configurator;
import org.easymock.EasyMockExtension;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.*;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Test the main functions of Miranda
 */
class MirandaTest {

    public static final Logger logger = LogManager.getLogger();

    /**
     * Set the default logging level to DEBUG
     */
    @BeforeAll
    public static void setup () {
        Configurator.setRootLevel(Level.DEBUG);
    }

    @Test
    void loadProperties() throws LtsllcException {
        Miranda miranda = new Miranda();
        miranda.loadProperties();
    }

    @Test
    void startMessagePort () throws LtsllcException {
        Miranda miranda = new Miranda();
        miranda.loadProperties();
        miranda.startMessagePort(1234);
    }

    @Test
    void processArguments () throws LtsllcException {
        Miranda miranda = new Miranda();
        miranda.loadProperties();
        String[] args = {"-one", "two"};
        Properties properties = miranda.processArguments(args);
        assert (properties.getProperty("o").equals("o"));
        assert (properties.getProperty("n").equals("n"));
        assert (properties.getProperty("e").equals("two"));
        String[] args2 = { "-l", "two","-RT"};
        Properties properties2 = miranda.processArguments(args2);
        assert (properties2.getProperty("l").equals("two"));
        assert (properties2.getProperty("R").equals("R"));
        assert (properties2.getProperty("T").equals("T"));
    }

    @Test
    void processArgument () {
        Miranda miranda = new Miranda();
        ImprovedProperties improvedProperties = new ImprovedProperties();
        miranda.processArgument("prev", "-abc", improvedProperties);
        assert (improvedProperties.getProperty("a").equals("a"));
        assert (improvedProperties.getProperty("b").equals("b"));
        assert (improvedProperties.getProperty("c").equals("c"));

        improvedProperties = new ImprovedProperties();
        miranda.processArgument("prev", "abc", improvedProperties);
        assert (improvedProperties.getProperty("prev").equals("abc"));
    }

    @org.junit.Test
    void biddingMode () throws LtsllcException {
        Miranda miranda = new Miranda();
        Message m1 = new Message();
        Date d1 = new Date();
        Date d2 = new Date();
        UUID uuid1 = new UUID(d1.getTime(), d2.getTime());
        m1.setMessageID(uuid1);
        Date d3 = new Date();
        Date d4 = new Date();
        UUID uuid2 = new UUID(d3.getTime(), d4.getTime());
        Message m2 = new Message();
        m2.setMessageID(uuid2);
        List<Message> list = new ArrayList<>();
        list.add(m1);
        list.add(m2);
        Cluster cluster = new Cluster();
        cluster.setNodes(new ArrayList<>());
        miranda.setCluster(cluster);
        miranda.setSendQueue(list);
        miranda.biddingMode();
        list = Miranda.getSendQueue();
        assert (list.get(0) == m1);
        assert (list.get(1) == m2);

        Message m3 = new Message();
        Date d6 = new Date();
        Date d7 = new Date();
        UUID uuid3 = new UUID(d6.getTime(), d7.getTime());
        m3.setMessageID(uuid3);

        Cluster mockCluster = mock(Cluster.class);
        when(mockCluster.bid(m1)).thenReturn(m1);
        when(mockCluster.bid(m2)).thenReturn(null);
        when(mockCluster.bid(m3)).thenReturn(m3);

        miranda.setCluster(mockCluster);

        list = new ArrayList<>();
        list.add(m1);
        list.add(m2);
        list.add(m3);
        Miranda.setSendQueue(list);

        miranda.biddingMode();

        list = Miranda.getSendQueue();
        assert (list.get(0) == m1);
        assert (list.get(1) == m3);
        assert (list.size() == 2);
    }


}