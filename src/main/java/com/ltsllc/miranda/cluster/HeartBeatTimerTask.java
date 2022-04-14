package com.ltsllc.miranda.cluster;

import com.ltsllc.miranda.Miranda;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.mina.core.future.WriteFuture;

import java.util.Timer;
import java.util.TimerTask;

public class HeartBeatTimerTask extends TimerTask {
    public static final Logger logger = LogManager.getLogger();

    protected Timer timer = null;
    protected Node node = null;


    public HeartBeatTimerTask (Node node, Timer timer) {
        this.node = node;
        this.timer = timer;
    }

    public Node getNode() {
        return node;
    }

    public void setNode(Node node) {
        this.node = node;
    }

    @Override
    public void run() {
        checkSendHeartBeat();
    }

    public void checkSendHeartBeat () {
        logger.debug("entering checkSendHeartBeat");
        long now = System.currentTimeMillis();
        long period = Miranda.getProperties().getLongProperty(Miranda.PROPERTY_HEART_BEAT_INTERVAL);

        if (now > node.getTimeOfLastActivity() + period) {
            if (node.isConnected()) {
                node.getIoSession().write(ClusterHandler.HEART_BEAT_START);
                logger.debug("wrote " + ClusterHandler.HEART_BEAT_START);
                node.setTimeOfLastActivity();
            } else {
                timer.cancel();
            }
        }

        logger.debug("leaving checkSendHeartBeat");
    }
}
