package com.ltsllc.miranda.cluster;

import com.ltsllc.miranda.Miranda;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * A class that runs the cluster.
 */
public class ClusterThread extends Thread {
    public static Logger logger = LogManager.getLogger(ClusterThread.class);

    public void ClusterThread () {
        Cluster.defineStatics();
    }

    public void run () {
        synchronized (this) {
            try {
                wait(1000);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }

        Miranda.parseNodes();
        Cluster cluster = Cluster.getInstance();
        try {
            cluster.start(Miranda.getSpecNodes());
        } catch (Throwable t) {
            throw new RuntimeException(t);
        }
    }
}
