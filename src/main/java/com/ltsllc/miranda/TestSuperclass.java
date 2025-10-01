package com.ltsllc.miranda;

import com.ltsllc.commons.io.ImprovedFile;
import com.ltsllc.miranda.cluster.Cluster;
import com.ltsllc.miranda.cluster.Node;
import com.ltsllc.miranda.message.Message;
import com.ltsllc.miranda.netty.HeartBeatHandler;
import com.ltsllc.miranda.properties.PropertiesHolder;
import io.netty.channel.Channel;
import io.netty.channel.embedded.EmbeddedChannel;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.config.Configurator;

import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * The superclass of all tests and a repository for useful methods
 */
public class TestSuperclass {
    public static Logger logger = LogManager.getLogger(TestSuperclass.class);

    public Message createTestMessage () {
        return createTestMessage(UUID.randomUUID(), UUID.randomUUID());
    }

    public Message createTestMessage(UUID uuid) {
        return createTestMessage(uuid, UUID.randomUUID());
    }

    public Message createTestMessage(UUID uuid, UUID owner) {
        Message message = new Message();
        message.setMessageID(uuid);
        message.setOwner(owner);
        message.setStatusURL("HTTP://localhost:3030/api/receiveStatus");
        message.setDeliveryURL("HTTP://localhost:3030/api/deliver");
        byte[] contents = {1, 2, 3};
        message.setContents(contents);

        return message;
    }

    public Node buildNode (UUID uuid) {
        EmbeddedChannel channel = new EmbeddedChannel();
        HeartBeatHandler handler = new HeartBeatHandler(channel, null);
        channel.pipeline().addLast(Node.HEART_BEAT, handler);

        Node node = new Node(uuid, "10.0.0.236", 2020, channel);
        handler.setNode(node);

        return node;
    }

    public InetSocketAddress parseAddr (int i) {
        PropertiesHolder p = Miranda.getProperties();
        String host = p.getProperty("cluster." + i + ".host");
        int port = p.getIntProperty("cluster." + i + ".port");

        InetSocketAddress addr = new InetSocketAddress(host, port);
        return addr;
    }

    public Thread startMiranda () {
        MirandaThread thread = new MirandaThread();
        thread.start();

        logger.debug("thread state: " + thread.getState());

        synchronized (this) {
            try {
                wait(4000);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
        logger.debug("thread state: " + thread.getState());
        return thread;
    }

    public void closePorts() {
        Miranda miranda = Miranda.getInstance();
        if (miranda == null) {
            return;
        }
        miranda.stopJetty();

        //
        // close the netty ports
        //
        Cluster cluster = Cluster.getInstance();
        if (cluster == null) {
            return;
        }

        Channel channel = cluster.getChannel();
        if (channel == null) {
            return;
        }

        try {
            channel.close().await();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public void setLoggingLevel (String component, Level level) {
        Map<String, Level> map = new HashMap<>();
        map.put(component, level);
        Configurator.setLevel(map);
    }

    public boolean isMirandaRunning () {
        Miranda miranda = Miranda.getInstance();
        return (miranda.getIterations() > 0);
    }

    public synchronized void pause (long time) {
        try {
            wait(time);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public void clearMessageLog () {
        ImprovedFile improvedFile = new ImprovedFile("messages.log");
        improvedFile.clear();
    }
}
