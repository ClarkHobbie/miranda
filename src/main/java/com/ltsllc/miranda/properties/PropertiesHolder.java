package com.ltsllc.miranda.properties;

import com.ltsllc.commons.UncheckedLtsllcException;
import com.ltsllc.commons.util.ImprovedProperties;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.util.*;
import java.util.Properties;

public class PropertiesHolder extends Properties {
    protected ImprovedProperties properties;
    protected Map<com.ltsllc.miranda.properties.Properties, List<PropertyListener>> listeners = new HashMap<>();

    public String setProperty(String propertyName, String value)  {
        try {
            properties.setProperty(propertyName, value);

            com.ltsllc.miranda.properties.Properties property = stringToProperty(propertyName);
            List<PropertyListener> listeners = this.listeners.get(properties);
            if (null != listeners) {
                for (PropertyListener listener : listeners) {
                    firePropertyChanged(listener, property);
                }
            }
        } catch (Throwable t) {
            throw new UncheckedLtsllcException("exception trying to set property", t);
        }
        return value;
    }

    public String getProperty (String propertyName) {
        return properties.getProperty(propertyName);
    }

    public int getIntProperty (String propertyName) {
        return properties.getIntProperty(propertyName);
    }

    public long getLongProperty (String propertyName) {
        return properties.getLongProperty(propertyName);
    }

    public void setIfNull (String propertyName, String propertyValue) {
        properties.setIfNull(propertyName, propertyValue);
    }

    public void load (Reader reader) throws IOException {
        properties.load(reader);
    }

    public void load (InputStream inputStream) throws IOException {
        properties.load(inputStream);
    }

    public com.ltsllc.miranda.properties.Properties stringToProperty(String propertyName) {
        com.ltsllc.miranda.properties.Properties property = com.ltsllc.miranda.properties.Properties.unknown;
        propertyName = propertyName.toLowerCase();

        if (propertyName.equalsIgnoreCase("uuid")) {
            property = com.ltsllc.miranda.properties.Properties.uuid;
        } else if (propertyName.equalsIgnoreCase("cache.loadLimit")) {
            property = com.ltsllc.miranda.properties.Properties.cacheLoadLimit;
        } else if (propertyName.equalsIgnoreCase("cluster")) {
            property = com.ltsllc.miranda.properties.Properties.cluster;
        } else if (propertyName.equalsIgnoreCase("ports.cluster")) {
            property = com.ltsllc.miranda.properties.Properties.clusterPort;
        } else if (propertyName.equalsIgnoreCase("cluster.retry")) {
            property = com.ltsllc.miranda.properties.Properties.clusterRetry;
        } else if (propertyName.startsWith("cluster.1")) {
            property = com.ltsllc.miranda.properties.Properties.cluster1;
        } else if (propertyName.startsWith("cluster.2")) {
            property = com.ltsllc.miranda.properties.Properties.cluster2;
        } else if (propertyName.startsWith("cluster.3")) {
            property = com.ltsllc.miranda.properties.Properties.cluster3;
        } else if (propertyName.startsWith("cluster.4")) {
            property = com.ltsllc.miranda.properties.Properties.cluster4;
        } else if (propertyName.startsWith("cluster.5")) {
            property = com.ltsllc.miranda.properties.Properties.cluster5;
        } else if (propertyName.equalsIgnoreCase("compaction.time")) {
            property = com.ltsllc.miranda.properties.Properties.compaction;
        } else if (propertyName.equalsIgnoreCase("timeouts.deadNode")) {
            property = com.ltsllc.miranda.properties.Properties.deadNodeTimeout;
        } else if (propertyName.equalsIgnoreCase("port")) {
            property = com.ltsllc.miranda.properties.Properties.clusterPort;
        } else if (propertyName.equalsIgnoreCase("heartBeatInterval")) {
            property = com.ltsllc.miranda.properties.Properties.heartBeat;
        } else if (propertyName.equalsIgnoreCase("timeouts.heart_beat")) {
            property = com.ltsllc.miranda.properties.Properties.heartBeatTimeout;
        } else if (propertyName.equalsIgnoreCase("host")) {
            property = com.ltsllc.miranda.properties.Properties.hostName;
        } else if (propertyName.equalsIgnoreCase("loggingLevel")) {
            property = com.ltsllc.miranda.properties.Properties.loggingLevel;
        } else if (propertyName.equalsIgnoreCase("messageLog")) {
            property = com.ltsllc.miranda.properties.Properties.messageLogfile;
        } else if (propertyName.equalsIgnoreCase("messagePort")) {
            property = com.ltsllc.miranda.properties.Properties.messagePort;
        } else if (propertyName.equalsIgnoreCase("ownerFile")) {
            property = com.ltsllc.miranda.properties.Properties.ownerFile;
        } else if (propertyName.equalsIgnoreCase("properties")) {
            property = com.ltsllc.miranda.properties.Properties.propertiesFile;
        } else if (propertyName.equalsIgnoreCase("timeouts.start")) {
            property = com.ltsllc.miranda.properties.Properties.startTimeout;
        }
        return property;
    }

    public void firePropertyChanged (PropertyListener listener, com.ltsllc.miranda.properties.Properties property)
            throws Throwable
    {
        PropertyChangedEvent propertyChangedEvent = new PropertyChangedEvent(this, property);
        listener.propertyChanged(propertyChangedEvent);
    }

    public void listen (PropertyListener listener, com.ltsllc.miranda.properties.Properties property) {
        List<PropertyListener> l = listeners.get(property);

        if (l == null) {
            l = new ArrayList<>();
            listeners.put(property, l);
        }

        l.add(listener);
    }

    public void unlisten (PropertyListener propertyListener, com.ltsllc.miranda.properties.Properties property) {
        List<PropertyListener> l = listeners.get(property);

        if (l == null) {
            return;
        }

        if (l.size() == 1) {
            listeners.remove(property);
            return;
        }

        if (l.contains(propertyListener)) {
            l.remove(propertyListener);
            return;
        }
    }
}
