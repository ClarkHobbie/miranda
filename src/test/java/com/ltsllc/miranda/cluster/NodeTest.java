package com.ltsllc.miranda.cluster;

import com.ltsllc.miranda.Message;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.config.Configurator;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.UUID;

public class NodeTest {
    @BeforeAll
    public void setup () {
        Configurator.setRootLevel(Level.DEBUG);
    }

    @Test
    public void auctionMessage () throws InterruptedException {
        Node node = new Node();

        Message message = createTestMessage(UUID.randomUUID());

        node.auctionMessage(message);
    }

    public Message createTestMessage (UUID uuid) {
        Message message = new Message();
        message.setMessageID(uuid);
        message.setStatusURL("HTTP://GOOGLE.COM");
        message.setDeliveryURL("HTTP://GOOGLE.COM");
        byte[] contents = {1, 2, 3};
        message.setContents(contents);

        return message;
    }


}
