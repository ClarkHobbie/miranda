package com.ltsllc.miranda.message;

import java.util.*;

public class MessageEventLogger {
    protected static List<MessageEvent> log = new ArrayList<>();
    protected static Map<UUID, List<MessageEvent>> map = new HashMap<>();
    public static void added(Message message) {
        MessageEvent messageEvent = new MessageEvent(MessageEventType.added);

        log.add(messageEvent);

        List<MessageEvent> messageEventList = map.get(message.getMessageID());
        if (messageEventList == null) {
            messageEventList = new ArrayList<>();
        }

        messageEventList.add(messageEvent);

        map.put(message.getMessageID(), messageEventList);
    }

    public static List<MessageEvent> getEventsFor (UUID uuid) {
        List<MessageEvent> messageEventList = null;
        messageEventList = map.get(uuid);
        return messageEventList;
    }
}
