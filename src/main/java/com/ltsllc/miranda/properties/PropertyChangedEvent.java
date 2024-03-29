package com.ltsllc.miranda.properties;

/**
 * A class that is past to a PropertyListen
 *
 * @see PropertyListener
 */
public class PropertyChangedEvent {
    protected Object source;
    protected Properties property;

    public PropertyChangedEvent(Object source, Properties property) {
        setSource(source);
        setProperty(property);
    }

    public Object getSource() {
        return source;
    }

    public void setSource(Object source) {
        this.source = source;
    }

    public Properties getProperty() {
        return property;
    }

    public void setProperty(Properties property) {
        this.property = property;
    }
}
