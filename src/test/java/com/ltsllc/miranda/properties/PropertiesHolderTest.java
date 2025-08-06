package com.ltsllc.miranda.properties;

import com.ltsllc.miranda.Miranda;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.config.Configurator;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class PropertiesHolderTest {
    public class PropertyChangeListener implements PropertyListener {
        public boolean wasRun = false;

        @Override
        public void propertyChanged(PropertyChangedEvent propertyChangedEvent) throws Throwable {
            wasRun = true;
        }

    }

    @BeforeAll
    public static void globalSetup () {
        Configurator.setRootLevel(Level.DEBUG);
    }

    @Test
    public void stringToProperty () {
        PropertiesHolder propertiesHolder = new PropertiesHolder();

        Properties[] properties = {
                Properties.uuid,
                Properties.cacheLoadLimit,
                Properties.cluster,
                Properties.cluster1,
                Properties.cluster2,
                Properties.cluster3,
                Properties.cluster4,
                Properties.cluster5,
                Properties.clusterRetry,
                Properties.compaction,
                Properties.heartBeat,
                Properties.hostName,
                Properties.loggingLevel,
                Properties.messageLogfile,
                Properties.messagePort,
                Properties.ownerFile,
                Properties.clusterPort,
                Properties.propertiesFile,
                Properties.deadNodeTimeout,
                Properties.heartBeatTimeout,
                Properties.startTimeout,
                Properties.useHeartbeats
        };

        boolean mismatch = false;
        for (Properties property : properties) {
            String propertyName = property.toString();
            Properties property2 = propertiesHolder.stringToProperty(propertyName);
            if (property2 != property) {
                mismatch = true;
                break;
            }
        }

        assert (!mismatch);
    }

    @Test
    public void isDifferentFromNoDifferences () {
        Miranda miranda = new Miranda();

        String table[][] = {
                {Miranda.PROPERTY_CACHE_LOAD_LIMIT, Miranda.PROPERTY_DEFAULT_CACHE_LOAD_LIMIT},
                {Miranda.PROPERTY_CLUSTER, "on"},
                {Miranda.PROPERTY_CLUSTER_1, "192.168.0.12"},
                {Miranda.PROPERTY_USE_HEARTBEATS, Miranda.PROPERTY_DEFAULT_USE_HEART_BEATS},
                {Miranda.PROPERTY_MESSAGE_LOG, Miranda.PROPERTY_DEFAULT_MESSAGE_LOG},
                {Miranda.PROPERTY_BID_TIMEOUT, Miranda.PROPERTY_DEFAULT_BID_TIMEOUT},
                {Miranda.PROPERTY_OWNER_FILE, Miranda.PROPERTY_DEFAULT_OWNER_FILE},
                {"cluster.1.port", "2021"},
                {Miranda.PROPERTY_COMPACTION_TIME, Miranda.PROPERTY_DEFAULT_COMPACTION_TIME},
                {Miranda.PROPERTY_PORT, Miranda.PROPERTY_DEFAULT_CLUSTER_PORT},
                {Miranda.PROPERTY_SCAN_PERIOD, Miranda.PROPERTY_DEFAULT_SCAN_PERIOD},
                {Miranda.PROPERTY_HOST, "192.168.0.20"},
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
    public void isDifferentFromOneDifference () {
        String table[][] = {
                {Miranda.PROPERTY_UUID, "00000000-0000-0000-0000-000000000000"},
                {Miranda.PROPERTY_CACHE_LOAD_LIMIT, "104856700"},
                {Miranda.PROPERTY_CLUSTER, "on"},
                {Miranda.PROPERTY_CLUSTER_1, "192.168.0.12"},
                {Miranda.PROPERTY_CLUSTER + ".1.port", "2021"},
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
                {Miranda.PROPERTY_START_TIMEOUT, "750"}
        };

        assert (Miranda.getProperties().isDifferentFrom(table));
    }

    @Test
    public void propertyIsDifferentNoDifferences () {
        Miranda miranda = new Miranda();
        String row[] = {Miranda.PROPERTY_DEAD_NODE_TIMEOUT, Miranda.PROPERTY_DEFAULT_DEAD_NODE_TIMEOUT};
        assert (!Miranda.getProperties().propertyIsDifferent(row));
    }

    @Test
    public void propertyIsDifferentDifferences () {
        Miranda miranda = new Miranda();
        String row[] = {Miranda.PROPERTY_DEAD_NODE_TIMEOUT, "750"};
        assert(Miranda.getProperties().propertyIsDifferent(row));
    }

    @Test
    public void listen () {
        Miranda miranda = new Miranda();
        PropertyChangeListener propertyChangeListener = new PropertyChangeListener();
        Miranda.getProperties().listen(propertyChangeListener, Properties.deadNodeTimeout);
        Miranda.getProperties().setProperty(Miranda.PROPERTY_DEAD_NODE_TIMEOUT, "750");
        assert (propertyChangeListener.wasRun);
    }

    @Test
    public void unlisten () {
        Miranda miranda = new Miranda();
        PropertiesHolder propertiesHolder = new PropertiesHolder();
        PropertyChangeListener propertyChangeListener = new PropertyChangeListener();
        Miranda.getProperties().listen(propertyChangeListener, Properties.startTimeout);
        Miranda.getProperties().setProperty("timeouts.start", "750");
        assert(propertyChangeListener.wasRun);
        Miranda.getProperties().unlisten(propertyChangeListener, Properties.startTimeout);
        propertyChangeListener.wasRun = false;
        Miranda.getProperties().setProperty("timeouts.start", "500");
        assert(!propertyChangeListener.wasRun);
    }
}

