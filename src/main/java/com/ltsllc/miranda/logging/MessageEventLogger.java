package com.ltsllc.miranda.logging;

import com.ltsllc.miranda.message.Message;

import java.util.*;

public class MessageEventLogger {
    protected List<MessageEvent> log = new ArrayList<>();
    protected Map<UUID, List<MessageEvent>> map = new HashMap<>();
    protected static MessageEventLogger instance = null;

    public List<MessageEvent> getLog() {
        return log;
    }

    public void setLog(List<MessageEvent> log) {
        this.log = log;
    }

    public Map<UUID, List<MessageEvent>> getMap() {
        return map;
    }

    public void setMap(Map<UUID, List<MessageEvent>> map) {
        this.map = map;
    }

    public static MessageEventLogger getInstance() {
        return instance;
    }

    public synchronized void added(Message message) {
        MessageEvent messageEvent = new MessageEvent(message.getMessageID(), MessageEventType.added);

        log.add(messageEvent);

        List<MessageEvent> messageEventList = getEventsFor(message.getMessageID());
        messageEventList.add(messageEvent);

        map.put(message.getMessageID(), messageEventList);
    }

    public static void defineStatics () {
        instance = new MessageEventLogger();
    }

    public synchronized List<MessageEvent> getEventsFor (UUID uuid) {
        List<MessageEvent> messageEventList = null;
        messageEventList = map.get(uuid);
        if (null == messageEventList) {
            messageEventList = new ArrayList<>();
            map.put(uuid, messageEventList);
        }

        return messageEventList;
    }

    public synchronized void deliveryAttempted(Message message) {
        MessageEvent messageEvent = new MessageEvent(message.getMessageID(), MessageEventType.attempted);

        log.add(messageEvent);

        List<MessageEvent> messageEventList = getListFor(message.getMessageID());
        messageEventList.add(messageEvent);

        map.put(message.getMessageID(), messageEventList);
    }

    public List<MessageEvent> getListFor (UUID uuid) {
        List<MessageEvent> messageEventList = map.get(uuid);
        if (messageEventList == null) {
            messageEventList = new ArrayList<>();
        }

        return messageEventList;
    }

    public synchronized void delivered(Message message) {
        MessageEvent messageEvent = new MessageEvent(message.getMessageID(), MessageEventType.delivered);

        log.add(messageEvent);

        List<MessageEvent> messageEventList = getListFor(message.getMessageID());
        messageEventList.add(messageEvent);

        map.put(message.getMessageID(), messageEventList);
    }

    public synchronized void attemptFailed(Message message) {
        MessageEvent messageEvent = new MessageEvent(message.getMessageID(), MessageEventType.attemptFailed);

        log.add(messageEvent);

        List<MessageEvent> messageEventList = getListFor(message.getMessageID());
        messageEventList.add(messageEvent);

        map.put(message.getMessageID(), messageEventList);
    }

    public synchronized void deleted (Message message) {
        MessageEvent messageEvent = new MessageEvent(message.getMessageID(), MessageEventType.deleted);

        log.add(messageEvent);

        List<MessageEvent> messageEventList = getListFor(message.getMessageID());
        messageEventList.add(messageEvent);

        map.put(message.getMessageID(), messageEventList);
    }
}