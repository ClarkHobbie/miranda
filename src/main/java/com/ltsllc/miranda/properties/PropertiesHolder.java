package com.ltsllc.miranda.properties;

import com.ltsllc.commons.UncheckedLtsllcException;
import com.ltsllc.commons.util.ImprovedProperties;
import com.ltsllc.commons.util.Property;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.util.*;
import java.util.Properties;

/**
 * A class that manages some properties and notifies classes of changes to those properties.
 */
public class PropertiesHolder extends Properties  {
    protected ImprovedProperties properties = new ImprovedProperties();
    protected Map<com.ltsllc.miranda.properties.Properties, List<PropertyListener>> listeners = new HashMap<>();

    /**
     * Set a property to a value
     *
     * <P>
     *     This method will notify listeners of the property that have registered via listen.
     * </P>
     * @param propertyName The name of the property to be changed.
     * @param value The new value for the property
     * @return The new value for the property.
     */
    public String setProperty(String propertyName, String value)  {
        try {
            properties.setProperty(propertyName, value);

            com.ltsllc.miranda.properties.Properties property = stringToProperty(propertyName);
            List<PropertyListener> l = this.listeners.get(property);
            if (null != l) {
                for (PropertyListener listener : l) {
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

    public boolean getBooleanProperty  (String propertyName) {
        return properties.getBooleanProperty(propertyName);
    }

    public String getStringProperty (String propertyName) {
        return properties.getStringProperty(propertyName);
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


    /**
     * Convert a string to a com.ltsllc.miranda.properties.Property
     *
     * <P>
     *     This method expects the caller to use the same name as used in the getProperty method --- that is, names
     *     like timeouts.deadNode rather than names like deadNodeTimeout the properties and what they map to are:
     *     <TABLE border="1">
     *         <TR>
     *             <TH>Name</TH>
     *             <TH>Maps To</TH>
     *         </TR>
     *         <TR>
     *             <TD>UUID</TD>
     *             <TD>uuid</TD>
     *         </TR>
     *         <TR>
     *             <TD>cache.loadLimit</TD>
     *             <TD>cacheLoadLimit</TD>
     *         </TR>
     *         <TR>
     *             <TD>cluster</TD>
     *             <TD>cluster</TD>
     *         </TR>
     *         <TR>
     *             <TD>cluster.1.&lt;anything&gt;</TD>
     *             <TD>cluster1</TD>
     *         </TR>
     *         <TR>
     *             <TD>cluster.2.&lt;anything&gt;</TD>
     *             <TD>cluster2</TD>
     *         </TR>
     *         <TR>
     *             <TD>cluster.3.&lt;anything&gt;</TD>
     *             <TD>cluster3</TD>
     *         </TR>
     *         <TR>
     *             <TD>cluster.4.&lt;anything&gt;</TD>
     *             <TD>cluster4</TD>
     *         </TR>
     *         <TR>
     *             <TD>cluster.5.&lt;anything&gt;</TD>
     *             <TD>cluster5</TD>
     *         </TR>
     *         <TR>
     *             <TD>cluster.retry</TD>
     *             <TD>clusterRetry</TD>
     *         </TR>
     *         <TR>
     *             <TD>compaction.time</TD>
     *             <TD>compaction</TD>
     *         </TR>
     *         <TR>
     *             <TD>heartBeatInterval</TD>
     *             <TD>heartBeat</TD>
     *         </TR>
     *         <TR>
     *             <TD>host</TD>
     *             <TD>hostName</TD>
     *         </TR>
     *         <TR>
     *             <TD>loggingLevel</TD>
     *             <TD>loggingLevel</TD>
     *         </TR>
     *         <TR>
     *             <TD>messageLog</TD>
     *             <TD>messageLogfile</TD>
     *         </TR>
     *         <TR>
     *             <TD>messagePort</TD>
     *             <TD>messagePort</TD>
     *         </TR>
     *         <TR>
     *             <TD>ownerFile</TD>
     *             <TD>ownerFile</TD>
     *         </TR>
     *         <TR>
     *             <TD>port</TD>
     *             <TD>clusterPort</TD>
     *         </TR>
     *         <TR>
     *             <TD>clusterPort</TD>
     *             <TD>clusterPort</TD>
     *         </TR>
     *         <TR>
     *             <TD>properties</TD>
     *             <TD>propertiesFile</TD>
     *         </TR>
     *         <TR>
     *             <TD>timeouts.deadNode</TD>
     *             <TD>deadNodeTimeout</TD>
     *         </TR>
     *         <TR>
     *             <TD>timeouts.heart_beat</TD>
     *             <TD>heartBeatTimeout</TD>
     *         </TR>
     *         <TR>
     *             <TD>timeouts.start</TD>
     *             <TD>startTimeout</TD>
     *         </TR>
     *     </TABLE>
     * </P>
     * @param propertyName The string (name) of the property.
     * @return The corresponding property value for the property.
     */
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

    /**
     * Send a propertyChanged message to the associated listener
     *
     * @param listener The listener for the property.
     * @param property The property that changed.
     * @throws Throwable If the listener throws an exception.
     */
    public void firePropertyChanged (PropertyListener listener, com.ltsllc.miranda.properties.Properties property)
            throws Throwable
    {
        PropertyChangedEvent propertyChangedEvent = new PropertyChangedEvent(this, property);
        listener.propertyChanged(propertyChangedEvent);
    }

    /**
     * Listen for changes to a given property
     *
     * @param listener The listener for the property changes.
     * @param property The property to listen to.
     */
    public void listen (PropertyListener listener, com.ltsllc.miranda.properties.Properties property) {
        List<PropertyListener> l = listeners.get(property);

        if (l == null) {
            l = new ArrayList<>();
            listeners.put(property, l);
        }

        l.add(listener);
    }

    /**
     * Stop listening for changes to a property
     *
     * <H>
     *     This method assumes that the listener is actually listening for changes to the property; if this is not the
     *     case then unpredictable results can occur.
     * </H>
     * @param propertyListener The listener.
     * @param property The property that the listener is listening to.
     */
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

    /**
     * Is a table different from the properties?
     *
     * @param table The table that the properties are compared to.  The table must consist of rows of the following
     *              form: &lt;name&gt;&lt;value&gt; to avoid unpredictable results.
     * @return true if the table contains values that are different from their associated property, false otherwise.
     */
    public boolean isDifferentFrom (String[][] table) {
        for (int i = 0; i < table.length; i++) {
            if (propertyIsDifferent(table[i])) {
                return true;
            }
        }

        return false;
    }

    /**
     * Is a row different from the associated property?
     *
     * @param row The row to compare with.  A row is expected to follow the form: &lt;property name&gt;&lt;value&gt;.
     *            If it does not then unpredictable results may occur.
     * @return True if the row has a different value from the row; false otherwise.
     */
    public boolean propertyIsDifferent (String[] row) {
        String name = row[0];
        String value = getProperty(row[0]);
        String newValue = row[1];
        return !value.equals(newValue);
    }

    public Set<Object> keySet () {
        return properties.keySet();
    }

    @Override
    public synchronized String toString() {
        return properties.toString();
    }

    public synchronized int size() {
        return properties.size();
    }


    public synchronized String[][] getTable () {

        String[][] table = new String[properties.size()][2];
        Enumeration enumeration = properties.keys();
        int i = 0;
        while (enumeration.hasMoreElements()) {
            String propertyName = (String) enumeration.nextElement();
            String value = properties.getProperty(propertyName);
            table[i][0] = propertyName;
            table[i][1] = value;
            i++;
        }
        return table;
    }


}
