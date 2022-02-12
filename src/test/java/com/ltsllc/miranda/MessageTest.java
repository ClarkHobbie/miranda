package com.ltsllc.miranda;


import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.config.Configurator;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Date;
import java.util.UUID;

class MessageTest {
    public static Logger logger = LogManager.getLogger();

    // Initialization of CollectionClass moved here (instead of a static block) for two
    // reasons:
    // 1. If the initialization fails, you can't run the test anyway - better fail
    //    right here that print an error and continue to the test which we
    //    know won't work
    // 2. It just looks neater
    @BeforeAll
    public static void initializeCollectionClass() throws IOException {
        Configurator.setRootLevel(Level.DEBUG);
    }



    @Test
    void informOfCreated() {
        Message message = new Message();
        message.setStatusURL("http://goggle.com");
        Date date1 = new Date();
        Date date2 = new Date();
        UUID uuid = new UUID(date1.getTime(), date2.getTime());
        message.setMessageID(uuid);

        message.setDeliveryURL("http://google.com");
        message.informOfCreated();
    }

    @Test
    public void deliver() {
        Message message = new Message();
        message.setStatusURL("http://goggle.com");
        Date date1 = new Date();
        Date date2 = new Date();
        UUID uuid = new UUID(date1.getTime(), date2.getTime());
        message.setMessageID(uuid);

        message.setDeliveryURL("http://google.com");
        message.deliver();
    }

    @Test
    public void informOfDelivery () {
        Message message = new Message();
        message.setStatusURL("http://goggle.com");
        Date date1 = new Date();
        Date date2 = new Date();
        UUID uuid = new UUID(date1.getTime(), date2.getTime());
        message.setMessageID(uuid);

        message.setDeliveryURL("http://google.com");
        message.informOfDelivery();

    }


}