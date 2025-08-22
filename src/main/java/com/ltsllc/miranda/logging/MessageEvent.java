package com.ltsllc.miranda.logging;

import com.ltsllc.commons.io.ScannerWithUnget;

import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.UUID;

public class MessageEvent {
    protected MessageEventType type = MessageEventType.unknown;
    protected long time = System.currentTimeMillis();
    protected Exception where = new Exception();
    protected UUID id;

    public MessageEvent () {}

    public MessageEvent(UUID id, MessageEventType newType) {
        this.id = id;
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

    public UUID getId() {
        return id;
    }

    public void setType(MessageEventType type) {
        this.type = type;
    }

    public void setTime(long time) {
        this.time = time;
    }

    public void setWhere(Exception where) {
        this.where = where;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String toStorageString ()
    {
        StringBuilder stringBuilder = new StringBuilder();

        stringBuilder.append("ID: ");
        stringBuilder.append(id.toString());
        stringBuilder.append(" TIME: ");
        stringBuilder.append(time);
        stringBuilder.append(" TYPE: ");
        stringBuilder.append(type.toString());
        stringBuilder.append(" WHERE: ");

        for (StackTraceElement stackTraceElement : where.getStackTrace()) {
            stringBuilder.append (stackTraceElement.getClassName());
            stringBuilder.append(' ');
            stringBuilder.append(stackTraceElement.getMethodName());
            stringBuilder.append(' ');
            stringBuilder.append(stackTraceElement.getFileName());
            stringBuilder.append(' ');
            stringBuilder.append(stackTraceElement.getLineNumber());
            stringBuilder.append(' ');
        }

        return stringBuilder.toString();
    }

    public static MessageEvent readMessageEvent (String line) {
        Scanner scanner = new Scanner(line);
        ScannerWithUnget scannerWithUnget = new ScannerWithUnget(scanner);
        MessageEvent messageEvent = new MessageEvent();
        readInternals(scannerWithUnget, messageEvent);

        return messageEvent;
    }

    public static void readInternals(ScannerWithUnget scanner, MessageEvent messageEvent) {
        scanner.next(); // ID:
        messageEvent.setId(UUID.fromString(scanner.next()));
        scanner.next(); // TIME:
        messageEvent.setTime(Long.parseLong(scanner.next()));
        scanner.next(); // TYPE:
        messageEvent.setType(MessageEventType.valueOf(scanner.next()));
        scanner.next(); // WHERE:

        List<StackTraceElement> stackTraceElements = new ArrayList<>();
        for (String token = scanner.next(); token != null; token = scanner.next()) {
            String className;
            String methodName;
            String lineNumber;
            String fileName;

            className = token;
            methodName = scanner.next();
            fileName = scanner.next();
            lineNumber = scanner.next();

            StackTraceElement stackTraceElement = new StackTraceElement(className, methodName, fileName,
                    Integer.parseInt(lineNumber));
            stackTraceElements.add(stackTraceElement);
        }
        StackTraceElement[] buffer = new StackTraceElement[stackTraceElements.size()];
        StackTraceElement[] stackTrace = stackTraceElements.toArray(buffer);

        Exception exception = new Exception();
        exception.setStackTrace(stackTrace);

        messageEvent.setWhere(exception);
    }
}
