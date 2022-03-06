package com.ltsllc.miranda;

import com.ltsllc.commons.LtsllcException;
import com.ltsllc.commons.io.ImprovedFile;
import com.ltsllc.util.Grep;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.config.Configurator;
import org.junit.Before;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.FileReader;
import java.io.IOException;
import java.util.UUID;
import java.util.regex.Pattern;

public class SendQueueFileTest {
    @BeforeAll
    public static void beforeAll () {
        Configurator.setRootLevel(Level.DEBUG);
    }

    @Before
    public void setup () throws LtsllcException {
        Miranda miranda = new Miranda();
        miranda.loadProperties();
    }

    public static Message createTestMessage (UUID uuid) {
        Message message = new Message();
        message.setMessageID(uuid);
        message.setStatusURL("HTTP://GOOGLE.COM");
        message.setDeliveryURL("HTTP://GOOGLE.COM");
        byte[] contents = {1, 2, 3};
        message.setContents(contents);

        return message;

    }

    @Test
    public void record () throws IOException, LtsllcException {
        Miranda miranda = new Miranda();
        miranda.loadProperties();

        ImprovedFile file = new ImprovedFile(Miranda.getProperties().getProperty(Miranda.PROPERTY_SEND_FILE));

        try {
            Message message = SendQueueFileTest.createTestMessage(UUID.randomUUID());

            SendQueueFile.defineStatics();
            SendQueueFile.getInstance().record(message);

            Grep grep = new Grep();

            assert (file.exists());
            assert (grep.matches(message.getMessageID().toString(), file) > 0);
        } finally {
            if (file.exists()) {
                file.delete();
            }
        }
    }

    @Test
    public void shouldRecover () throws LtsllcException, IOException {
        Miranda miranda = new Miranda();
        miranda.loadProperties();

        ImprovedFile file = new ImprovedFile(Miranda.getProperties().getProperty(Miranda.PROPERTY_SEND_FILE));
        try {
            file.touch();
            assert(SendQueueFile.shouldRecover());
        } finally {
            if (file.exists()) {
                file.delete();
            }
        }

    }
}
