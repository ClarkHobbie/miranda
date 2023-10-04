package com.ltsllc.miranda.cluster;

import com.ltsllc.miranda.Miranda;

public class ClusterThread extends Thread {

    public void run () {
        Cluster.defineStatics();
        Miranda.parseNodes();
        try {
            Cluster.getInstance().start(Miranda.getSpecNodes());
        } catch (Throwable t) {
            throw new RuntimeException(t);
        }
        while (true) {
        }
    }
}
