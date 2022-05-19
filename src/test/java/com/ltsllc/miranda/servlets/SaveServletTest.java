package com.ltsllc.miranda.servlets;

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
    public void serviceNoChanges () {
        Miranda miranda = new Miranda();

        String table[][] = {
                {Miranda.PROPERTY_UUID, "00000000-0000-0000-0000-000000000000"},
                {Miranda.PROPERTY_CACHE_LOAD_LIMIT, "104856700"},
                {Miranda.PROPERTY_CLUSTER, "on"},
                {Miranda.PROPERTY_CLUSTER_1, "192.168.0.12"},
                {Miranda.PROPERTY_CLUSTER + ".1.port", "2020"},
                {Miranda.PROPERTY_CLUSTER_RETRY, "10000"},
                {Miranda.PROPERTY_COMPACTION_TIME, "100000"},
                {Miranda.PROPERTY_HEART_BEAT_INTERVAL, "5000"},
                {Miranda.PROPERTY_HOST, "192.168.0.20"},
                {Miranda.PROPERTY_LONG_LOGGING_LEVEL, "error"},
                {Miranda.PROPERTY_MESSAGE_LOG, "messages.log"},
                {Miranda.PROPERTY_MESSAGE_PORT, "3030"},
                {Miranda.PROPERTY_OWNER_FILE, "owners.log"},
                {Miranda.PROPERTY_PORT, "2020"},
                {Miranda.PROPERTY_CLUSTER_PORT, "2020"},
                {Miranda.PROPERTY_PROPERTIES_FILE, "miranda.properties"},
                {Miranda.PROPERTY_DEAD_NODE_TIMEOUT, "500"},
                {Miranda.PROPERTY_HEART_BEAT_TIMEOUT, "500"},
                {Miranda.PROPERTY_START_TIMEOUT, "100000"}
        };

        assert (!Miranda.getProperties().isDifferentFrom(table));
    }

    @Test
    public void serviceOneDifferent () {
        String table[][] = {
                {Miranda.PROPERTY_UUID, "00000000-0000-0000-0000-000000000000"},
                {Miranda.PROPERTY_CACHE_LOAD_LIMIT, "104856700"},
                {Miranda.PROPERTY_CLUSTER, "on"},
                {Miranda.PROPERTY_CLUSTER_1, "192.168.0.12"},
                {Miranda.PROPERTY_CLUSTER + ".1.port", "2020"},
                {Miranda.PROPERTY_CLUSTER_RETRY, "10000"},
                {Miranda.PROPERTY_COMPACTION_TIME, "100000"},
                {Miranda.PROPERTY_HEART_BEAT_INTERVAL, "120000"},
                {Miranda.PROPERTY_HOST, "192.168.0.20"},
                {Miranda.PROPERTY_LONG_LOGGING_LEVEL, "error"},
                {Miranda.PROPERTY_MESSAGE_LOG, "messages.log"},
                {Miranda.PROPERTY_MESSAGE_PORT, "3030"},
                {Miranda.PROPERTY_OWNER_FILE, "owners.log"},
                {Miranda.PROPERTY_PORT, "2020"},
                {Miranda.PROPERTY_CLUSTER_PORT, "2020"},
                {Miranda.PROPERTY_PROPERTIES_FILE, "miranda.properties"},
                {Miranda.PROPERTY_DEAD_NODE_TIMEOUT, "750"},
                {Miranda.PROPERTY_HEART_BEAT_TIMEOUT, "500"},
                {Miranda.PROPERTY_START_TIMEOUT, "500"}
        };

        assert (Miranda.getProperties().isDifferentFrom(table));

    }
}
