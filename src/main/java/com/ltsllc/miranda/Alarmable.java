package com.ltsllc.miranda;

import com.ltsllc.miranda.Alarms;

public interface Alarmable {
    public void alarm (Alarms alarm) throws Throwable;
}
