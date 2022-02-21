package com.ltsllc.miranda;


import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.ltsllc.commons.LtsllcException;
import net.bytebuddy.description.method.MethodDescription;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.config.Configurator;
import org.easymock.EasyMockExtension;
import org.junit.Test;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.extension.ExtendWith;

import java.io.*;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;


import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class DoNothingTest {
    protected static final Logger logger = LogManager.getLogger();

    @BeforeAll
    public void setup () {
    }

    @Test
    public void doNothing () throws LtsllcException, IOException {
        Configurator.setRootLevel(Level.DEBUG);
        Miranda miranda = new Miranda();
        miranda.loadProperties();


        miranda = new Miranda();
        miranda.loadProperties();
        File sendFile = new File(Miranda.PROPERTY_DEFAULT_SEND_FILE);
        FileOutputStream fileOutputStream = new FileOutputStream(sendFile);
        Writer writer = new OutputStreamWriter(fileOutputStream);

        Date d1 = new Date();
        Date d2 = new Date();
        UUID uuid = new UUID(d1.getTime(), d2.getTime());

        Message message = new Message();
        message.setMessageID(uuid);
        message.setStatusURL("http://google.com");
        message.setContents("Hi there!".getBytes());
        message.setDeliveryURL("http://google.com");

        List<Message> list = new ArrayList<>();
        list.add(message);

        miranda.writeJson(list,writer);
        fileOutputStream.close();


        Miranda.setSendQueue(new ArrayList<>());

        miranda.loadSendFile();

        list = Miranda.getSendQueue();
        if (!(message.equals(list.get(0)))) {
            logger.debug(list.get(0) + " is not equal to " + message);
        }
        assert (list.get(0).equals(message));

    }
}
