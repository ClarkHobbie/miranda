package com.ltsllc.miranda.cluster;

import com.ltsllc.miranda.Miranda;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.mina.core.future.WriteFuture;

import java.util.Timer;
import java.util.TimerTask;

public class HeartBeatTimerTask extends TimerTask {
    public static final Logger logger = LogManager.getLogger();
    protected ClusterHandler clusterHandler = null;
    protected Timer timer = null;


    public HeartBeatTimerTask (ClusterHandler clusterHandler, Timer timer) {
        this.clusterHandler = clusterHandler;
        this.timer = timer;
    }

    public ClusterHandler getClusterHandler() {
        return clusterHandler;
    }

    public void setClusterHandler(ClusterHandler clusterHandler) {
        this.clusterHandler = clusterHandler;
    }

    @Override
    public void run() {
        checkSendHeartBeat();
    }

    public void checkSendHeartBeat () {
        logger.debug("entering checkSendHeartBeat");
        long now = System.currentTimeMillis();
        long period = Miranda.getProperties().getLongProperty(Miranda.PROPERTY_HEART_BEAT_INTERVAL);

        if (now > clusterHandler.getTimeOfLastActivity() + period) {
            if (clusterHandler.getNode().isConnected()) {
                clusterHandler.getNode().getIoSession().write(ClusterHandler.HEART_BEAT_START);
                logger.debug("wrote " + ClusterHandler.HEART_BEAT_START);
                clusterHandler.setTimeOfLastActivity();
            } else {
                timer.cancel();
            }
        }

        logger.debug("leaving checkSendHeartBeat");
    }
}
