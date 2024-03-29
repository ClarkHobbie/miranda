package com.ltsllc.miranda.alarm;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import java.util.TimerTask;

/**
 * A TimerTask that notifies other classes
 */
public class AlarmTimerTask extends TimerTask {
    public static final Logger logger = LogManager.getLogger(AlarmTimerTask.class);

    protected Alarmable receiver;
    protected Alarms alarm;

    public AlarmTimerTask (Alarmable receiver, Alarms alarm) {
        this.receiver = receiver;
        this.alarm = alarm;
    }

    @Override
    public void run() {
        try {
            receiver.alarm(alarm);
        }
        catch (Throwable e) {
            logger.error("alarm threw exception", e);
        }
    }
}
