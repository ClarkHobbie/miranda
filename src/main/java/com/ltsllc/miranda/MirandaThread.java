package com.ltsllc.miranda;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class MirandaThread extends Thread {
    public static final Logger logger = LogManager.getLogger();

    protected Miranda miranda;
    protected volatile boolean keepRunning = true;

    protected volatile long sleepTime = 1000;

    public MirandaThread () {
        super();
    }
    public long getSleepTime() {
        return sleepTime;
    }

    public void setSleepTime(long sleepTime) {
        this.sleepTime = sleepTime;
    }

    public Miranda getMiranda() {
        return miranda;
    }

    public void setMiranda(Miranda miranda) {
        this.miranda = miranda;
    }

    public boolean isKeepRunning() {
        return keepRunning;
    }

    public void setKeepRunning(boolean keepRunning) {
        this.keepRunning = keepRunning;
    }

    @Override
    public void run () {
        logger.debug("starting run");
        while (keepRunning) {
            logger.debug("entering iteration");
            miranda.mainLoop();
            if (sleepTime > 0) {
                synchronized (this) {
                    try {
                        logger.debug("sleeping for " + sleepTime + " milliseconds");
                        wait(sleepTime);
                    } catch (InterruptedException e) {

                    }

                }
            }
            logger.debug("leaving iteration");
        }
        logger.debug("leaving run");
    }
}
