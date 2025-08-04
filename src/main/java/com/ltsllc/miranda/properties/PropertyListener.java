package com.ltsllc.miranda.properties;

/**
 * A listener for property changes.
 */
public interface PropertyListener {
    public void propertyChanged(PropertyChangedEvent propertyChangedEvent) throws Throwable;
}
