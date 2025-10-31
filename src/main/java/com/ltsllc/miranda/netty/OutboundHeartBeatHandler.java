package com.ltsllc.miranda.netty;

import com.ltsllc.miranda.Miranda;
import com.ltsllc.miranda.alarm.AlarmClock;
import com.ltsllc.miranda.alarm.Alarmable;
import com.ltsllc.miranda.alarm.Alarms;
import com.ltsllc.miranda.cluster.Node;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.nio.charset.Charset;

public class OutboundHeartBeatHandler extends ChannelOutboundHandlerAdapter implements Alarmable {
    public static Logger logger = LogManager.getLogger(OutboundHeartBeatHandler.class);

    public String direction;
    public Channel channel;
    public volatile Long timeOfLastActivity;
    public volatile Boolean metTimeout = false;
    public int iterations = 0;
    public volatile Boolean channelIsLocked;

    public OutboundHeartBeatHandler(Boolean channelIsLocked, String direction, Channel channel, Long timeOfLastActivity, Boolean metTimeout) {
        this.channelIsLocked = channelIsLocked;
        this.direction = direction;
        this.channel = channel;
        this.timeOfLastActivity = timeOfLastActivity;
        this.metTimeout = metTimeout;
        AlarmClock.getInstance().scheduleOnce(this, Alarms.HEART_BEAT, Miranda.getProperties().getLongProperty(Miranda.PROPERTY_HEART_BEAT_INTERVAL));
    }

    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
        timeOfLastActivity = System.currentTimeMillis();
        String s = null;
        if (msg instanceof ByteBuf) {
            ByteBuf byteBuf = (ByteBuf) msg;
            s = byteBuf.toString(Charset.defaultCharset());
        } else if (msg instanceof String) {
            s = (String) msg;
        }

        if (s.equalsIgnoreCase(Node.HEART_BEAT_START)) {
            sendHeartBeat();
            return;
        }

        if (msg instanceof String) {
            ByteBuf byteBuf = Unpooled.copiedBuffer(s.getBytes());
            msg = byteBuf;
        }

        ChannelFuture future = ctx.writeAndFlush(msg, promise);
        ChannelFutureListener listener = new ChannelFutureListener(){
            @Override
            public void operationComplete(ChannelFuture future) {
                if (future.isSuccess()) {
                    logger.debug(direction + " message sent successfully!");
                } else {
                    logger.error(direction + " Failed to send message: " + future.cause());
                    // Optionally, close the channel on failure
                    future.channel().close();
                }
            }
        };
        future.addListener(listener);
    }

    public void sendHeartBeat() {
        iterations++;

        if (iterations % 100 == 0) {
            System.gc();
        }

        if (channel == null || !channel.isOpen()) {
            logger.error("null or closed channel");
            return;
        }

        AlarmClock.getInstance().scheduleOnce(this, Alarms.HEART_BEAT,
                Miranda.getProperties().getLongProperty(Miranda.PROPERTY_HEART_BEAT_INTERVAL));
        if (Miranda.getProperties().getBooleanProperty(Miranda.PROPERTY_USE_HEARTBEATS)) {
            if (System.currentTimeMillis() >
                    timeOfLastActivity + Miranda.getProperties().getLongProperty(Miranda.PROPERTY_HEART_BEAT_INTERVAL)) {
                String message = Node.HEART_BEAT_START;
                ByteBuf byteBuf = Unpooled.copiedBuffer(message.getBytes());

                while (channelIsLocked) {
                    try {
                        synchronized (this) {
                            wait(1000);
                        }
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                }
                channelIsLocked = true;
                ChannelFuture future = channel.writeAndFlush(byteBuf);
                long delay = Miranda.getProperties().getLongProperty(Miranda.PROPERTY_HEART_BEAT_TIMEOUT);
                AlarmClock.getInstance().scheduleOnce(this, Alarms.HEART_BEAT_TIMEOUT, delay);
                metTimeout = false;
                try {
                    future.await();
                    channelIsLocked = false;
                    if (!future.isSuccess()) {
                        logger.error("heartbeat start failed");
                        future.cause().printStackTrace();
                    }
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }

            }
        }
    }

    public void alarm(Alarms alarm) throws Throwable {
        switch (alarm) {
            case HEART_BEAT -> sendHeartBeat();

            case HEART_BEAT_TIMEOUT -> heartBeatTimeout();

        }
    }

    public void heartBeatTimeout() throws IOException {
        if (!metTimeout) {
            logger.error("Node has gone offline.");
            channel.close();
        } else {
            metTimeout = false;
        }
    }

    public void exceptionCaught (ChannelHandlerContext ctx, Throwable cause) {
        logger.error ("caught exception: " + cause);
        logger.error(cause);
    }

}