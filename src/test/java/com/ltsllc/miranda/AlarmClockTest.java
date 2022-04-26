package com.ltsllc.miranda;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.config.Configurator;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class AlarmClockTest extends TestSuperclass implements Alarmable {
    int timesCalled = 0;
    @BeforeAll
    public static void setup () {
        Configurator.setRootLevel(Level.DEBUG);
    }

    @Test
    public void schedule () throws InterruptedException {
        timesCalled = 0;
        AlarmClock.getInstance().cancelAllAlarms();

        AlarmClock.getInstance().schedule(this, Alarms.TEST, 250);

        synchronized (this) {
            wait(1000);
        }

        assert(timesCalled > 1);
    }

    @Test
    public void scheduleOnce () throws InterruptedException {
        timesCalled = 0;
        AlarmClock.getInstance().cancelAllAlarms();

        AlarmClock.getInstance().scheduleOnce(this, Alarms.TEST, 250);

        synchronized (this) {
            wait(1000);
        }

        assert (timesCalled == 1);
    }

    @Override
    public void alarm(Alarms alarm) {
        timesCalled++;
    }
}
