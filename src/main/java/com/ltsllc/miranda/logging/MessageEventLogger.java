package com.ltsllc.miranda.logging;

import com.ltsllc.miranda.message.Message;

import java.util.*;

public class MessageEventLogger {
    protected static List<MessageEvent> log = new ArrayList<>();
    protected static Map<UUID, List<MessageEvent>> map = new HashMap<>();

    public static List<MessageEvent> getLog() {
        return log;
    }

    public static void setLog(List<MessageEvent> log) {
        MessageEventLogger.log = log;
    }

    public static Map<UUID, List<MessageEvent>> getMap() {
        return map;
    }

    public static void setMap(Map<UUID, List<MessageEvent>> map) {
        MessageEventLogger.map = map;
    }

    public static synchronized void added(Message message) {
        MessageEvent messageEvent = new MessageEvent(message.getMessageID(), MessageEventType.added);

        log.add(messageEvent);

        List<MessageEvent> messageEventList = getEventsFor(message.getMessageID());
        messageEventList.add(messageEvent);

        map.put(message.getMessageID(), messageEventList);
    }

    public static synchronized List<MessageEvent> getEventsFor (UUID uuid) {
        List<MessageEvent> messageEventList = null;
        messageEventList = map.get(uuid);
        if (null == messageEventList) {
            messageEventList = new ArrayList<>();
            map.put(uuid, messageEventList);
        }

        return messageEventList;
    }

    public static synchronized void deliveryAttempted(Message message) {
        MessageEvent messageEvent = new MessageEvent(message.getMessageID(), MessageEventType.attempted);

        log.add(messageEvent);

        List<MessageEvent> messageEventList = getListFor(message.getMessageID());
        messageEventList.add(messageEvent);

        map.put(message.getMessageID(), messageEventList);
    }

    public static List<MessageEvent> getListFor (UUID uuid) {
        List<MessageEvent> messageEventList = map.get(uuid);
        if (messageEventList == null) {
            messageEventList = new ArrayList<>();
        }

        return messageEventList;
    }

    public static synchronized void delivered(Message message) {
        MessageEvent messageEvent = new MessageEvent(message.getMessageID(), MessageEventType.delivered);

        log.add(messageEvent);

        List<MessageEvent> messageEventList = getListFor(message.getMessageID());
        messageEventList.add(messageEvent);

        map.put(message.getMessageID(), messageEventList);
    }

    public static synchronized void attemptFailed(Message message) {
        MessageEvent messageEvent = new MessageEvent(message.getMessageID(), MessageEventType.attemptFailed);

        log.add(messageEvent);

        List<MessageEvent> messageEventList = getListFor(message.getMessageID());
        messageEventList.add(messageEvent);

        map.put(message.getMessageID(), messageEventList);
    }

    public static synchronized void deleted (Message message) {
        MessageEvent messageEvent = new MessageEvent(message.getMessageID(), MessageEventType.deleted);

        log.add(messageEvent);

        List<MessageEvent> messageEventList = getListFor(message.getMessageID());
        messageEventList.add(messageEvent);

        map.put(message.getMessageID(), messageEventList);
    }
}