package com.ltsllc.miranda.message;

import com.ltsllc.miranda.logging.MessageEvent;
import com.ltsllc.miranda.logging.MessageEventLogger;
import com.ltsllc.miranda.logging.MessageEventType;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.UUID;

class MessageEventLoggerTest {

    @Test
    void added() {
        Message message = buildMessage();
        List<MessageEvent> messageEventList = MessageEventLogger.getEventsFor(message.getMessageID());

        assert (messageEventList.size() <= 0);

        MessageEventLogger.added(message);

        messageEventList = MessageEventLogger.getListFor(message.getMessageID());

        assert (messageEventList.size() >= 1);
    }

    public Message buildMessage() {
        Message message = new Message();
        message.setMessageID(UUID.randomUUID());
        message.setStatusURL("http://localhost:2021");
        message.setDeliveryURL("http://localhost:2021");
        message.setContents("hi there".getBytes());

        return message;
    }

    @Test
    void deliveryAttempted() {
        Message message = buildMessage();
        List<MessageEvent> messageEventList = MessageEventLogger.getEventsFor(message.getMessageID());

        assert (messageEventList.size() <= 0);

        MessageEventLogger.deliveryAttempted(message);

        messageEventList = MessageEventLogger.getEventsFor(message.getMessageID());

        assert (messageEventList.get(0).getType() == MessageEventType.attempted);
    }

    @Test
    void getListFor() {
        Message message = buildMessage();
        List<MessageEvent> messageEventList = MessageEventLogger.getLog();
        Map<UUID, List<MessageEvent>> eventsMap = MessageEventLogger.getMap();

        assert (!messageEventList.contains(message));
        assert (eventsMap.get(message.getMessageID()) == null);

        MessageEventLogger.added(message);

        messageEventList = MessageEventLogger.getEventsFor(message.getMessageID());

        assert (messageEventList.getFirst().getType() == MessageEventType.added);
    }

    @Test
    void delivered() {
        Message message = buildMessage();
        List<MessageEvent> messageEventList = MessageEventLogger.getLog();
        Map<UUID, List<MessageEvent>> eventsMap = MessageEventLogger.getMap();

        assert (!messageEventList.contains(message));
        assert (eventsMap.get(message.getMessageID()) == null);

        MessageEventLogger.delivered(message);

        messageEventList = MessageEventLogger.getEventsFor(message.getMessageID());

        assert (messageEventList.getFirst().getType() == MessageEventType.delivered);
    }

    @Test
    void attemptFailed() {
        Message message = buildMessage();
        List<MessageEvent> messageEventList = MessageEventLogger.getLog();
        Map<UUID, List<MessageEvent>> eventsMap = MessageEventLogger.getMap();

        assert (!messageEventList.contains(message));
        assert (eventsMap.get(message.getMessageID()) == null);

        MessageEventLogger.attemptFailed(message);

        messageEventList = MessageEventLogger.getEventsFor(message.getMessageID());

        assert (messageEventList.getFirst().getType() == MessageEventType.attemptFailed);
    }

    @Test
    void deleted() {
        Message message = buildMessage();
        List<MessageEvent> messageEventList = MessageEventLogger.getLog();
        Map<UUID, List<MessageEvent>> eventsMap = MessageEventLogger.getMap();

        assert (!messageEventList.contains(message));
        assert (eventsMap.get(message.getMessageID()) == null);

        MessageEventLogger.deleted(message);

        messageEventList = MessageEventLogger.getEventsFor(message.getMessageID());

        assert (messageEventList.getFirst().getType() == MessageEventType.deleted);
    }
}