package com.ltsllc.miranda;


import com.ltsllc.commons.LtsllcException;
import com.ltsllc.commons.util.ImprovedProperties;
import com.ltsllc.commons.util.ImprovedRandom;
import com.ltsllc.miranda.cluster.Cluster;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.config.Configurator;
import org.apache.mina.core.session.IoSession;
import org.asynchttpclient.*;
import org.easymock.EasyMockExtension;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

@ExtendWith(EasyMockExtension.class)
class Whatever {

    @BeforeAll
    public static void setup () {
        Configurator.setRootLevel(Level.DEBUG);
    }

    @Test
    public void bid() throws LtsllcException {
        Cluster cluster = new Cluster();
        cluster.setNodes(new ArrayList<>());
        ImprovedRandom improvedRandom = new ImprovedRandom();
        Cluster.setRandomNumberGenerator(improvedRandom);

        Message message = new Message();
        message.setStatusURL("http://goggle.com");
        Date date1 = new Date();
        Date date2 = new Date();
        UUID uuid = new UUID(date1.getTime(), date2.getTime());
        message.setMessageID(uuid);

        cluster.bid(message);
    }

    @Test
    public void connect () throws LtsllcException {
        Cluster cluster = new Cluster();
        cluster.setNodes(new ArrayList<>());
        ImprovedRandom improvedRandom = new ImprovedRandom();
        Cluster.setRandomNumberGenerator(improvedRandom);

        Miranda.setProperties(new ImprovedProperties());
        Miranda miranda = new Miranda();
        miranda.loadProperties();

        cluster.connect();
    }

    @Test
    void informOfNewMessage () throws LtsllcException {
        Cluster cluster = new Cluster();
        cluster.setNodes(new ArrayList<>());
        ImprovedRandom improvedRandom = new ImprovedRandom();
        Cluster.setRandomNumberGenerator(improvedRandom);

        Message message = new Message();
        message.setStatusURL("http://goggle.com");
        Date date1 = new Date();
        Date date2 = new Date();
        UUID uuid = new UUID(date1.getTime(), date2.getTime());
        message.setMessageID(uuid);

        cluster.informOfNewMessage(message);
    }

    @Test
    void getBid () throws LtsllcException {
        Configurator.setRootLevel(Level.DEBUG);
        Cluster cluster = new Cluster();
        cluster.connect();

        Miranda miranda = new Miranda();
        miranda.loadProperties();

        Message message = new Message();
        Date date1 = new Date();
        Date date2 = new Date();
        UUID uuid = new UUID(date1.getTime(), date2.getTime());
        message.setMessageID(uuid);

        AsyncHttpClient client = Dsl.asyncHttpClient();
        Request postRequest = Dsl.post("http://localhost:" + miranda.getProperties().getProperty(Miranda.PROPERTY_CLUSTER_PORT))
                .build();
        client.executeRequest(postRequest, new AsyncCompletionHandler<Integer>() {
            @Override
            public Integer onCompleted(Response response) {
                int responseStatusCode = response.getStatusCode();
                assert(200 == responseStatusCode);
                return  responseStatusCode;
            }
        });

        String body = "BID " + message.getMessageID() + " 1234";

        String url = "http://localhost:" + miranda.getProperties().getProperty(Miranda.PROPERTY_CLUSTER_PORT);
        client = Dsl.asyncHttpClient();
        postRequest = Dsl.post(url)
                .setBody(body)
                .build();

        List<IoSession> list = cluster.getNodes();
        IoSession ioSession = list.get(0);

        client.executeRequest(postRequest, new AsyncCompletionHandler<Integer>() {
            @Override
            public Integer onCompleted(Response response) {
                int responseStatusCode = response.getStatusCode();
                assert(200 == responseStatusCode);
                return  responseStatusCode;
            }
        });


        cluster.getBid(ioSession, message);


    }


}