package com.ltsllc.commons.util;

import java.util.Properties;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/*
 * An improved version of the Properties class
 *
 * This class improves on its ancestor by offering
 * <UL>
 * <LI>getting an INTEGER property</LI>
 * <LI>setting properties if they are null</LI>
 * </UL>
 */
public class ImprovedProperties extends Properties {
    public static final Logger logger = LogManager.getLogger();

    /*
     * create an empty properties
     */
    public ImprovedProperties () {
        super();
    }


    public void doNothing () {}

    /*
     * get an integer property
     *
     * This method translates a String property to an integer property.  In the case where the named property
     * is null, the method returns 0.
     */
    public int getIntProperty (String name) {
        String value = getProperty(name);

        if (null == value) {
            value = "0";
        }
        
        return Integer.parseInt(value);
    }

    /*
     * get an integer property, with a default value.
     *
     * If the named property is null, then the method returns the default value.
     */
    public int getIntProperty(String name, String defaultValue) {
        String propertyValue = getProperty(name);
        if (null == propertyValue) {
            propertyValue = defaultValue;
        }

        return Integer.parseInt(propertyValue);
    }

    public String getStringProperty (String name) {
        return getStringProperty(name, null);
    }

    
    /**
     * Get a string property, with a default value
     *
     * If the named property is null, then the method returns the default value.
     */

    public String getStringProperty (String name, String defaultValue) {
        String propertyValue = getProperty(name);
        if (null == propertyValue) {
            propertyValue = defaultValue;
        }

        return propertyValue;
    }

    /*
     * set a bunch of properties if they are undefined (null)
     *
     * This method sets properties if they are null.
     *
     * @param properties The properties to define if they are currently null
     */
    public void setIfNull (Properties properties){
        for (Object keyObject : properties.keySet()) {
            String key = keyObject.toString();
            String value = getProperty(key);

            if (null == value) {
                value = properties.getProperty(key);
            }

            setProperty(key, value);
        }
    }

    /*
     * set a property if it is currently null
     *
     * This method set a property only if it is currently null, otherwise, it does nothing
     */
    public void setIfNull (String key, String value) {
        if (getProperty(key) == null) {
            setProperty(key, value);
        }
    }

    /**
     * Get a property as a long value or take the default value if it is not defined.
     *
     * Note that the if the default value is null, then the method returns 0.
     *
     * @param property The property to get.
     * @param defaultValue The default value if the property is not defined
     * @return The properties value or the default value if the property is not defined
     */
    public long getLongProperty (String property, String defaultValue) {
        long returnValue = 0;
        if (getProperty(property) == null) {
            if (null == defaultValue) {
            } else {
                try {
                    returnValue = Long.parseLong(defaultValue);
                } catch (Exception e) {
                    returnValue = 0;
                }
            }
        } else {
            try {
                returnValue = Long.parseLong(getProperty(property));
            } catch (Exception e) {
                returnValue = 0;
            }
        }
        return returnValue;
    }

    /**
     * Get a property value as a long
     *
     *
     * @param property The property to get.
     * @return The properties value, as a long or 0 if an exception is thrown while parsing the property or the property
     * is null
     */
    public long getLongProperty (String property) {

        long returnValue = 0;
        if (null == getProperty(property)) {
        } else {
            try {
                returnValue = Long.parseLong(getProperty(property));
            } catch (Exception e) {
                logger.error ("exception parsing long, returning 0",e);
                returnValue = 0;
            }
        }
        return returnValue;
    }

    /**
     * Get a boolean property
     */
    public boolean getBooleanProperty (String property) {
        boolean returnValue = false;
        if (null == getProperty(property)) {
        } else {
            try {
                returnValue = Boolean.parseBoolean(getProperty(property));
            } catch (Exception e) {
                logger.error ("exception parsing boolean, returning false",e);
                returnValue = false;
            }
        }
        return returnValue;
    }
}
