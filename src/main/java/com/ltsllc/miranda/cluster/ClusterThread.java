package com.ltsllc.miranda.cluster;

import com.ltsllc.miranda.Miranda;

/**
 * A class that runs the cluster.
 */
public class ClusterThread extends Thread {

    public void run () {
        Cluster.defineStatics();
        Miranda.parseNodes();
        Cluster cluster = Cluster.getInstance();
        try {
            cluster.start(Miranda.getSpecNodes());
        } catch (Throwable t) {
            throw new RuntimeException(t);
        }
    }
}
