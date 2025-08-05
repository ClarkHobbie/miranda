package com.ltsllc.miranda.message;


import com.ltsllc.miranda.TestSuperclass;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.config.Configurator;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Scanner;
import java.util.UUID;

class MessageTest extends TestSuperclass {
    public static Logger logger = LogManager.getLogger();

    @BeforeAll
    public static void initializeCollectionClass() throws IOException {
        Configurator.setRootLevel(Level.DEBUG);
    }

    @Test
    public void contentsAreEquivalent () {
        byte[] array1 = "hi there".getBytes();
        byte[] array2 = null;

        boolean result = Message.contentsAreEquivalent(array1, array1);
        assert (result);

        result = Message.contentsAreEquivalent(array1,array2);
        assert (!result);

        result = Message.contentsAreEquivalent(array2, array1);
        assert (!result);

        byte[] array3 = "low there".getBytes();

        result = Message.contentsAreEquivalent(array1,array3);
        assert (!result);

        byte[] array4 = "hi there".getBytes();

        result = Message.contentsAreEquivalent(array1, array4);
        assert (result);

        byte[] array5 = "h1 there".getBytes();

        result = Message.contentsAreEquivalent(array1, array5);
        assert (!result);
    }

    @Test
    public void equals () {
        Message message1 = createTestMessage(UUID.randomUUID());
        Message message2 = createTestMessage(UUID.randomUUID());

        boolean result = message1.equals(message1);
        assert (result);

        result = message1.equals(message2);
        assert (!result);

        message2.setMessageID(message1.getMessageID());
        result = message1.equals(message2);
        assert (result);

        message2.setStatusURL("HTTP://GOOGLE.COM");
        result = message1.equals(message2);
        assert (!result);

        message2.setStatusURL(message1.getStatusURL());
        message2.setDeliveryURL("HTTP://GOOGLE.COM");
        result = message1.equals(message2);
        assert (!result);

        message2.setDeliveryURL(message1.getDeliveryURL());
        byte[] array = {1,2,4};
        message2.setContents(array);
        result = message1.equals(message2);
        assert (!result);
    }

    @Test
    public void everythingToString () {
        String strUuid = "12345678-9abc-def1-2345-6789abcdef12";
        UUID uuid = UUID.randomUUID();
        Message message1 = createTestMessage(UUID.fromString(strUuid));
        message1.setStatus(401);
        String s1 = message1.everythingToString();
        String s2 = "MESSAGE ID: 12345678-9abc-def1-2345-6789abcdef12 STATUS: HTTP://localhost:8080 DELIVERY: HTTP://localhost:8080 LAST STATUS: 401 CONTENTS: 010203";
        boolean result = s1.equals(s2);
        assert (result);
    }

    @Test
    public void internalsToString () {
        String  strUuid = "12345678-9abc-def1-2345-6789abcdef12";
        Message message = createTestMessage(UUID.fromString(strUuid));
        String s2 = "ID: 12345678-9abc-def1-2345-6789abcdef12 STATUS: HTTP://localhost:8080 DELIVERY: HTTP://localhost:8080 CONTENTS: 010203";
        assert (s2.equals(message.internalsToString()));
    }

    @Test
    public void longToString () {
        String  strUuid = "12345678-9abc-def1-2345-6789abcdef12";
        Message message = createTestMessage(UUID.fromString(strUuid));
        String s2 = "MESSAGE ID: 12345678-9abc-def1-2345-6789abcdef12 STATUS: HTTP://localhost:8080 DELIVERY: HTTP://localhost:8080 CONTENTS: 010203";
        assert (s2.equals(message.longToString()));
    }

    @Test
    public void readEverything () {
        String s = "MESSAGE ID: 12345678-9abc-def1-2345-6789abcdef12 STATUS: HTTP://localhost:8080 DELIVERY: HTTP://localhost:8080 CONTENTS: 010203";
        String  strUuid = "12345678-9abc-def1-2345-6789abcdef12";
        Message message = createTestMessage(UUID.fromString(strUuid));
        message.setStatus(401);
        Message message2 = Message.readEverything(s);
        assert (message2.equals(message));
    }

    @Test
    public void readLongFormat () {
        String  strUuid = "12345678-9abc-def1-2345-6789abcdef12";
        String s2 = "MESSAGE ID: 12345678-9abc-def1-2345-6789abcdef12 STATUS: HTTP://localhost:8080 DELIVERY: HTTP://localhost:8080 CONTENTS: 010203";
        Message message1 = createTestMessage(UUID.fromString(strUuid));
        assert (message1.equals(Message.readLongFormat(s2)));
    }

    @Test
    public void readLongFormatFromScanner () {
        String  strUuid = "12345678-9abc-def1-2345-6789abcdef12";
        String s = "MESSAGE ID: 12345678-9abc-def1-2345-6789abcdef12 STATUS: HTTP://localhost:8080 DELIVERY: HTTP://localhost:8080 CONTENTS: 010203";
        Message message = createTestMessage(UUID.fromString(strUuid));
        Scanner scanner = new Scanner(s);
        assert (message.equals(Message.readLongFormat(scanner)));
    }

    @Test
    public void testToString () {
        String  strUuid = "12345678-9abc-def1-2345-6789abcdef12";
        String s = "12345678-9abc-def1-2345-6789abcdef12";
        Message message = createTestMessage(UUID.fromString(strUuid));
        assert (s.equals(message.toString()));
    }
}