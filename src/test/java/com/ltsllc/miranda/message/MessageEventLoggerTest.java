package com.ltsllc.miranda.message;

import com.ltsllc.miranda.logging.MessageEvent;
import com.ltsllc.miranda.logging.MessageEventLogger;
import com.ltsllc.miranda.logging.MessageEventType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.UUID;

class MessageEventLoggerTest {
    @BeforeEach
    void setUp () {
        MessageEventLogger.defineStatics();
    }

    @Test
    void added() throws IOException {
        Message message = buildMessage();
        List<MessageEvent> messageEventList = MessageEventLogger.getInstance().getEventsFor(message.getMessageID());

        assert (messageEventList.size() <= 0);

        MessageEventLogger.getInstance().added(message);

        messageEventList = MessageEventLogger.getInstance().getListFor(message.getMessageID());

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
        MessageEventLogger.defineStatics();
        List<MessageEvent> messageEventList = MessageEventLogger.getInstance().getEventsFor(message.getMessageID());

        assert (messageEventList.size() <= 0);

        MessageEventLogger.getInstance().deliveryAttempted(message);

        messageEventList = MessageEventLogger.getInstance().getEventsFor(message.getMessageID());

        assert (messageEventList.get(0).getType() == MessageEventType.attempted);
    }

    @Test
    void getListFor() throws IOException {
        MessageEventLogger.defineStatics();
        Message message = buildMessage();
        List<MessageEvent> messageEventList = MessageEventLogger.getInstance().getLog();
        Map<UUID, List<MessageEvent>> eventsMap = MessageEventLogger.getInstance().getMap();

        assert (!messageEventList.contains(message));
        assert (eventsMap.get(message.getMessageID()) == null);

        MessageEventLogger.getInstance().added(message);

        messageEventList = MessageEventLogger.getInstance().getEventsFor(message.getMessageID());

        assert (messageEventList.getFirst().getType() == MessageEventType.added);
    }

    @Test
    void delivered() {
        Message message = buildMessage();
        List<MessageEvent> messageEventList = MessageEventLogger.getInstance().getLog();
        Map<UUID, List<MessageEvent>> eventsMap = MessageEventLogger.getInstance().getMap();

        assert (!messageEventList.contains(message));
        assert (eventsMap.get(message.getMessageID()) == null);

        MessageEventLogger.getInstance().delivered(message);

        messageEventList = MessageEventLogger.getInstance().getEventsFor(message.getMessageID());

        assert (messageEventList.getFirst().getType() == MessageEventType.delivered);
    }

    @Test
    void attemptFailed() {
        Message message = buildMessage();
        MessageEventLogger.defineStatics();
        List<MessageEvent> messageEventList = MessageEventLogger.getInstance().getLog();
        Map<UUID, List<MessageEvent>> eventsMap = MessageEventLogger.getInstance().getMap();

        assert (!messageEventList.contains(message));
        assert (eventsMap.get(message.getMessageID()) == null);

        MessageEventLogger.getInstance().attemptFailed(message);

        messageEventList = MessageEventLogger.getInstance().getEventsFor(message.getMessageID());

        assert (messageEventList.getFirst().getType() == MessageEventType.attemptFailed);
    }

    @Test
    void deleted() {
        Message message = buildMessage();
        List<MessageEvent> messageEventList = MessageEventLogger.getInstance().getLog();
        Map<UUID, List<MessageEvent>> eventsMap = MessageEventLogger.getInstance().getMap();

        assert (!messageEventList.contains(message));
        assert (eventsMap.get(message.getMessageID()) == null);

        MessageEventLogger.getInstance().deleted(message);

        messageEventList = MessageEventLogger.getInstance().getEventsFor(message.getMessageID());

        assert (messageEventList.getFirst().getType() == MessageEventType.deleted);
    }
}