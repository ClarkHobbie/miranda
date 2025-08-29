package com.ltsllc.miranda.logging;

import com.ltsllc.commons.LtsllcException;
import com.ltsllc.commons.io.ImprovedFile;
import com.ltsllc.miranda.Miranda;
import com.ltsllc.miranda.message.Message;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

public class MessageEventLogger {
    protected List<MessageEvent> log = new ArrayList<>();
    protected Map<UUID, List<MessageEvent>> map = new HashMap<>();
    protected ImprovedFile file = null;

    public MessageEventLogger() {
        String fileName = Miranda.getProperties().getProperty(Miranda.PROPERTY_EVENTS_FILE);
        file = new ImprovedFile(fileName);
    }

    public MessageEventLogger(ImprovedFile eventsFile) {
        this.file = eventsFile;
    }

    public void clear() {
        ImprovedFile backup = new ImprovedFile(file.getAbsolutePath() + ".backup");
        if (backup.exists()) {
            backup.delete();;
        }

        try {
            file.backup(backup);
        } catch (LtsllcException e) {
            throw new RuntimeException(e);
        }
    }

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

    public synchronized void store () {
        try {
            ImprovedFile backUp = new ImprovedFile(Miranda.getProperties().getProperty(Miranda.PROPERTY_EVENTS_FILE)
                    + ".backup");
            if (backUp.exists()) {
                backUp.delete();
            }

            if (file.exists()) {
                file.copyTo(backUp);
            }

            FileWriter fileWriter = null;
            BufferedWriter bufferedWriter = null;
            try {
                fileWriter = new FileWriter(file);
                bufferedWriter = new BufferedWriter(fileWriter);

                for (MessageEvent messageEvent : log) {
                    bufferedWriter.write(messageEvent.toStorageString());
                    bufferedWriter.newLine();
                }
            } finally {
                if (bufferedWriter != null) {
                    bufferedWriter.close();
                }

                if (fileWriter != null) {
                    fileWriter.close();
                }
            }
        } catch (IOException | LtsllcException e) {
            throw new RuntimeException(e);
        }
    }

    public synchronized void added(Message message) {
        MessageEvent messageEvent = new MessageEvent(message.getMessageID(), MessageEventType.added);
        messageEvent.setId(message.getMessageID());

        log.add(messageEvent);

        List<MessageEvent> messageEventList = getEventsFor(message.getMessageID());
        messageEventList.add(messageEvent);

        map.put(message.getMessageID(), messageEventList);

        store();
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

        store();
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

        store();
    }

    public synchronized void attemptFailed(Message message) {
        MessageEvent messageEvent = new MessageEvent(message.getMessageID(), MessageEventType.attemptFailed);
        messageEvent.setId(message.getMessageID());

        log.add(messageEvent);

        List<MessageEvent> messageEventList = getListFor(message.getMessageID());
        messageEventList.add(messageEvent);

        map.put(message.getMessageID(), messageEventList);

        store();
    }

    public synchronized void deleted (Message message) {
        MessageEvent messageEvent = new MessageEvent(message.getMessageID(), MessageEventType.deleted);

        log.add(messageEvent);

        List<MessageEvent> messageEventList = getListFor(message.getMessageID());
        messageEventList.add(messageEvent);

        map.put(message.getMessageID(), messageEventList);

        store();
    }

    public synchronized void removed (Message message) {
        MessageEvent messageEvent = new MessageEvent(message.getMessageID(), MessageEventType.removed);


    }
}