package com.ltsllc.miranda.properties;

import java.util.Set;

public interface PropertyListener {
    public void propertyChanged(PropertyChangedEvent propertyChangedEvent) throws Throwable;
    public Set<Properties> listeningTo();
}
