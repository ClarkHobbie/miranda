package com.ltsllc.miranda.logging;

import com.ltsllc.commons.LtsllcException;
import com.ltsllc.miranda.message.Message;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.UUID;

class MessageEventLoggerTest {
    @BeforeEach
    void setUp () {
        MessageLog.defineStatics();
    }

    @Test
    void added() throws IOException, LtsllcException {
        MessageLog.defineStatics();
        Message message = buildMessage();
        List<MessageEvent> messageEventList = MessageLog.getInstance().getMessageEventLogger().getEventsFor(message.getMessageID());

        assert (messageEventList.size() <= 0);

        MessageLog.getInstance().getMessageEventLogger().added(message);

        messageEventList = MessageLog.getInstance().getMessageEventLogger().getListFor(message.getMessageID());

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
        List<MessageEvent> messageEventList = MessageLog.getInstance().getMessageEventLogger().getEventsFor(message.getMessageID());

        assert (messageEventList.size() <= 0);

        MessageLog.getInstance().getMessageEventLogger().deliveryAttempted(message);

        messageEventList = MessageLog.getInstance().getMessageEventLogger().getEventsFor(message.getMessageID());

        assert (messageEventList.get(0).getType() == MessageEventType.attempted);
    }

    @Test
    void getListFor() throws IOException, LtsllcException {
        Message message = buildMessage();
        List<MessageEvent> messageEventList = MessageLog.getInstance().getMessageEventLogger().getLog();
        Map<UUID, List<MessageEvent>> eventsMap = MessageLog.getInstance().getMessageEventLogger().getMap();

        assert (!messageEventList.contains(message));
        assert (eventsMap.get(message.getMessageID()) == null);

        MessageLog.getInstance().getMessageEventLogger().added(message);

        messageEventList = MessageLog.getInstance().getMessageEventLogger().getEventsFor(message.getMessageID());

        assert (messageEventList.getFirst().getType() == MessageEventType.added);
    }

    @Test
    void delivered() {
        Message message = buildMessage();
        List<MessageEvent> messageEventList = MessageLog.getInstance().getMessageEventLogger().getLog();
        Map<UUID, List<MessageEvent>> eventsMap = MessageLog.getInstance().getMessageEventLogger().getMap();

        assert (!messageEventList.contains(message));
        assert (eventsMap.get(message.getMessageID()) == null);

        MessageLog.getInstance().getMessageEventLogger().delivered(message);

        messageEventList = MessageLog.getInstance().getMessageEventLogger().getEventsFor(message.getMessageID());

        assert (messageEventList.getFirst().getType() == MessageEventType.delivered);
    }

    @Test
    void attemptFailed() {
        Message message = buildMessage();
        List<MessageEvent> messageEventList = MessageLog.getInstance().getMessageEventLogger().getLog();
        Map<UUID, List<MessageEvent>> eventsMap = MessageLog.getInstance().getMessageEventLogger().getMap();

        assert (!messageEventList.contains(message));
        assert (eventsMap.get(message.getMessageID()) == null);

        MessageLog.getInstance().getMessageEventLogger().attemptFailed(message);

        messageEventList = MessageLog.getInstance().getMessageEventLogger().getEventsFor(message.getMessageID());

        assert (messageEventList.getFirst().getType() == MessageEventType.attemptFailed);
    }

    @Test
    void deleted() {
        Message message = buildMessage();
        List<MessageEvent> messageEventList = MessageLog.getInstance().getMessageEventLogger().getLog();
        Map<UUID, List<MessageEvent>> eventsMap = MessageLog.getInstance().getMessageEventLogger().getMap();

        assert (!messageEventList.contains(message));
        assert (eventsMap.get(message.getMessageID()) == null);

        MessageLog.getInstance().getMessageEventLogger().deleted(message);

        messageEventList = MessageLog.getInstance().getMessageEventLogger().getEventsFor(message.getMessageID());

        assert (messageEventList.getFirst().getType() == MessageEventType.deleted);
    }
}