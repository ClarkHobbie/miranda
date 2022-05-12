package com.ltsllc.miranda.alarm;

import java.util.Timer;

/**
 * A class that notifies other classes
 */
public class AlarmClock {
    protected static AlarmClock instance = new AlarmClock();

    protected AlarmClock () {}

    public static AlarmClock getInstance() {
        return instance;
    }

    public static void setInstance(AlarmClock instance) {
        AlarmClock.instance = instance;
    }

    protected Timer timer = new Timer();

    public Timer getTimer() {
        return timer;
    }

    public void setTimer(Timer timer) {
        this.timer = timer;
    }

    public void cancelAllAlarms () {
        this.timer.cancel();
        this.timer = new Timer();
    }


    public void schedule (Alarmable receiver, Alarms alarm, long period)
    {
        AlarmTimerTask alarmTimerTask = new AlarmTimerTask(receiver, alarm);
        timer.schedule(alarmTimerTask, period, period);
    }

    public void scheduleOnce (Alarmable receiver, Alarms alarm, long time) {
        AlarmTimerTask alarmTimerTask = new AlarmTimerTask(receiver, alarm);
        timer.schedule(alarmTimerTask, time);
    }

}
