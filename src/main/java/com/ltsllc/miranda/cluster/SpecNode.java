package com.ltsllc.miranda.cluster;

/**
 * A simple class that contains the specification for a node
 */
public class SpecNode {
    protected String host;
    protected int port;

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }
}
