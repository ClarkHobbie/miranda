package com.ltsllc.miranda.alarm;

/**
 * An object that listens for alarms.
 */
public interface Alarmable {
    public void alarm (Alarms alarm) throws Throwable;
}
