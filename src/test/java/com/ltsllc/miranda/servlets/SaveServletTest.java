package com.ltsllc.miranda.servlets;

import com.ltsllc.commons.LtsllcException;
import com.ltsllc.miranda.Miranda;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.config.Configurator;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class SaveServletTest {
    @BeforeAll
    public static void globalSetup () {
        Configurator.setRootLevel(Level.DEBUG);
    }

    @Test
    public void serviceNoChanges () throws LtsllcException {
        Miranda miranda = new Miranda();
        miranda.loadProperties();

        String table[][] = {
                {Miranda.PROPERTY_CACHE_LOAD_LIMIT, Miranda.PROPERTY_DEFAULT_CACHE_LOAD_LIMIT},
                {Miranda.PROPERTY_CLUSTER, "on"},
                {Miranda.PROPERTY_CLUSTER_1, "10.0.0.49"},
                {Miranda.PROPERTY_USE_HEARTBEATS, Miranda.PROPERTY_DEFAULT_USE_HEART_BEATS},
                {Miranda.PROPERTY_MESSAGE_LOG, Miranda.PROPERTY_DEFAULT_MESSAGE_LOG},
                {Miranda.PROPERTY_BID_TIMEOUT, Miranda.PROPERTY_DEFAULT_BID_TIMEOUT},
                {Miranda.PROPERTY_OWNER_FILE, Miranda.PROPERTY_DEFAULT_OWNER_FILE},
                {"cluster1Port", "2021"},
                {Miranda.PROPERTY_COMPACTION_TIME, Miranda.PROPERTY_DEFAULT_COMPACTION_TIME},
                {Miranda.PROPERTY_THIS_PORT, Miranda.PROPERTY_DEFAULT_CLUSTER_PORT},
                {Miranda.PROPERTY_SCAN_PERIOD, Miranda.PROPERTY_DEFAULT_SCAN_PERIOD},
                {Miranda.PROPERTY_THIS_HOST, "10.0.0.236"},
                {Miranda.PROPERTY_HEART_BEAT_INTERVAL, Miranda.PROPERTY_DEFAULT_HEART_BEAT_INTERVAL},
                {Miranda.PROPERTY_CLUSTER_PORT, Miranda.PROPERTY_DEFAULT_CLUSTER_PORT},
                {Miranda.PROPERTY_DEAD_NODE_TIMEOUT, Miranda.PROPERTY_DEFAULT_DEAD_NODE_TIMEOUT},
                {Miranda.PROPERTY_COALESCE_PERIOD, Miranda.PROPERTY_DEFAULT_COALESCE_PERIOD},
                {Miranda.PROPERTY_MESSAGE_PORT, Miranda.PROPERTY_DEFAULT_MESSAGE_PORT},
                {Miranda.PROPERTY_UUID, "00000000-0000-0000-0000-000000000000"},
                {Miranda.PROPERTY_HEART_BEAT_TIMEOUT, Miranda.PROPERTY_DEFAULT_HEART_BEAT_TIMEOUT},
                {Miranda.PROPERTY_PROPERTIES_FILE, Miranda.PROPERTY_DEFAULT_PROPERTIES_FILE},
                {Miranda.PROPERTY_START_TIMEOUT, Miranda.PROPERTY_DEFAULT_START_TIMEOUT}
        };

        assert (!Miranda.getProperties().isDifferentFrom(table));
    }

    @Test
    public void serviceOneDifferent () {
        String table[][] = {
                {Miranda.PROPERTY_CACHE_LOAD_LIMIT, Miranda.PROPERTY_DEFAULT_CACHE_LOAD_LIMIT},
                {Miranda.PROPERTY_CLUSTER, "on"},
                {Miranda.PROPERTY_CLUSTER_1, "71.237.68.250"},
                {Miranda.PROPERTY_USE_HEARTBEATS, Miranda.PROPERTY_DEFAULT_USE_HEART_BEATS},
                {Miranda.PROPERTY_MESSAGE_LOG, Miranda.PROPERTY_DEFAULT_MESSAGE_LOG},
                {Miranda.PROPERTY_BID_TIMEOUT, Miranda.PROPERTY_DEFAULT_BID_TIMEOUT},
                {Miranda.PROPERTY_OWNER_FILE, Miranda.PROPERTY_DEFAULT_OWNER_FILE},
                {"cluster1Port", "2021"},
                {Miranda.PROPERTY_COMPACTION_TIME, Miranda.PROPERTY_DEFAULT_COMPACTION_TIME},
                {Miranda.PROPERTY_THIS_PORT, Miranda.PROPERTY_DEFAULT_CLUSTER_PORT},
                {Miranda.PROPERTY_SCAN_PERIOD, Miranda.PROPERTY_DEFAULT_SCAN_PERIOD},
                {Miranda.PROPERTY_THIS_HOST, "71.237.68.250"},
                {Miranda.PROPERTY_HEART_BEAT_INTERVAL, Miranda.PROPERTY_DEFAULT_HEART_BEAT_INTERVAL},
                {Miranda.PROPERTY_CLUSTER_PORT, Miranda.PROPERTY_DEFAULT_CLUSTER_PORT},
                {Miranda.PROPERTY_DEAD_NODE_TIMEOUT, Miranda.PROPERTY_DEFAULT_DEAD_NODE_TIMEOUT},
                {Miranda.PROPERTY_COALESCE_PERIOD, Miranda.PROPERTY_DEFAULT_COALESCE_PERIOD},
                {Miranda.PROPERTY_MESSAGE_PORT, Miranda.PROPERTY_DEFAULT_MESSAGE_PORT},
                {Miranda.PROPERTY_UUID, "00000000-0000-0000-0000-000000000000"},
                {Miranda.PROPERTY_HEART_BEAT_TIMEOUT, Miranda.PROPERTY_DEFAULT_HEART_BEAT_TIMEOUT},
                {Miranda.PROPERTY_PROPERTIES_FILE, Miranda.PROPERTY_DEFAULT_PROPERTIES_FILE},
                {Miranda.PROPERTY_START_TIMEOUT, "5"}
        };

        assert (Miranda.getProperties().isDifferentFrom(table));
    }
}
