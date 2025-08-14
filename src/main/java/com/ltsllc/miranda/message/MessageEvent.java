package com.ltsllc.miranda.message;

import jdk.jfr.StackTrace;

import java.time.LocalTime;

public class MessageEvent {
    protected MessageEventType type = MessageEventType.unknown;
    protected long time = System.currentTimeMillis();
    protected Exception where = new Exception();

    public MessageEvent(MessageEventType newType) {
        this.type = newType;
    }

    public Exception getWhere() {
        return where;
    }

    public long getTime() {
        return time;
    }

    public MessageEventType getType() {
        return type;
    }
}
