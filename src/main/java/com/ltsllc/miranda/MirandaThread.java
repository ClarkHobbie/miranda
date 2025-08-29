package com.ltsllc.miranda;

import com.ltsllc.commons.LtsllcException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;

public class MirandaThread extends Thread {
    public static Logger logger = LogManager.getLogger(Miranda.class);

    @Override
    public void run() {
        logger.debug("entering run");
        Miranda miranda = Miranda.getInstance();
        int exceptionCount = 0;
        boolean startupFailed = true;

        while (exceptionCount < 5 && startupFailed) {
            try {
                miranda.startUp(new String[0]);
                startupFailed = false;
            } catch (Exception e) {
                exceptionCount++;
                startupFailed = true;
            }
        }

        while (miranda.isKeepRunning()) {
            try {
                miranda.mainLoop();

                if (miranda.getIterations() % 1000 == 0) {
                    System.gc();
                }
            } catch (IOException | LtsllcException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
