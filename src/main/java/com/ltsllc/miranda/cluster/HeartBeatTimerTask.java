package com.ltsllc.miranda.cluster;

import com.ltsllc.miranda.Miranda;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.mina.core.future.WriteFuture;

import java.util.Timer;
import java.util.TimerTask;

/**
 * A TimerTask that sends a heart beat message according to the node's last activity and the setting for heart beat
 * interval.
 */
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

    public Timer getTimer() {
        return timer;
    }

    public void setTimer(Timer timer) {
        this.timer = timer;
    }

    @Override
    public void run() {
        checkSendHeartBeat();
    }

    /**
     * Send a heart beat message if the line has been idle for a heart beat interval
     */
    public void checkSendHeartBeat () {
        logger.debug("entering checkSendHeartBeat");
        long now = System.currentTimeMillis();
        long period = Miranda.getProperties().getLongProperty(Miranda.PROPERTY_HEART_BEAT_INTERVAL);

        if (now > node.getTimeOfLastActivity() + period) {
            if (node.getIoSession() != null) {
                node.sendHeartBeat();
            } else {

            }
        }

        logger.debug("leaving checkSendHeartBeat");
    }
}
