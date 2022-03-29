package com.ltsllc.miranda;

import com.ltsllc.commons.util.ImprovedProperties;
import com.ltsllc.miranda.Message;
import com.ltsllc.miranda.Miranda;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.config.Configurator;
import org.bouncycastle.util.IPAddress;
import org.junit.jupiter.api.BeforeAll;

import java.net.InetSocketAddress;
import java.util.UUID;

/**
 * The superclass of all tests and a repository for useful methods
 */
public class TestSuperclass {
    public Message createTestMessage(UUID uuid) {
        Message message = new Message();
        message.setMessageID(uuid);
        message.setStatusURL("HTTP://localhost:8080");
        message.setDeliveryURL("HTTP://localhost:8080");
        byte[] contents = {1, 2, 3};
        message.setContents(contents);

        return message;
    }

    public InetSocketAddress parseAddr (int i) {
        ImprovedProperties p = Miranda.getProperties();
        String host = p.getProperty("cluster." + i + ".host");
        int port = p.getIntProperty("cluster." + i + ".port");

        InetSocketAddress addr = new InetSocketAddress(host, port);
        return addr;
    }

    @BeforeAll
    public static void setup () {
        Configurator.setRootLevel(Level.DEBUG);

    }
}
