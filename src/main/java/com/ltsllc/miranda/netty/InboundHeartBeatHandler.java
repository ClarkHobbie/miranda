package com.ltsllc.miranda.netty;

import com.ltsllc.commons.LtsllcException;
import com.ltsllc.miranda.Miranda;
import com.ltsllc.miranda.alarm.AlarmClock;
import com.ltsllc.miranda.alarm.Alarmable;
import com.ltsllc.miranda.alarm.Alarms;
import com.ltsllc.miranda.cluster.Node;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.List;

public class InboundHeartBeatHandler extends ChannelInboundHandlerAdapter implements Alarmable {

    public static Logger logger = LogManager.getLogger(InboundHeartBeatHandler.class);

    public Channel channel = null;
    public volatile Boolean metTimeout = false;
    public volatile Long timeOfLastActivity;
    public int iterations = 0;

    public InboundHeartBeatHandler (Channel channel, Long timeOfLastActivity, Boolean metTimeout) {
        this.channel = channel;
        this.timeOfLastActivity = timeOfLastActivity;
        this.metTimeout = metTimeout;
        AlarmClock.getInstance().scheduleOnce(this, Alarms.HEART_BEAT,
                Miranda.getProperties().getLongProperty(Miranda.PROPERTY_HEART_BEAT_INTERVAL));
    }

    @Override
    public void alarm(Alarms alarm) throws Throwable {
        switch (alarm) {
            case HEART_BEAT ->
                    sendHeartBeat();

            case HEART_BEAT_TIMEOUT ->
                    heartBeatTimeout();
        }

    }

    public void heartBeatTimeout() throws IOException, LtsllcException {
        if (!metTimeout) {
            logger.error("Node has gone offline.");
            channel.close();
        } else {
            metTimeout = false;
        }
    }

    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        timeOfLastActivity = System.currentTimeMillis();
        String s = null;
        if (msg instanceof ByteBuf) {
            ByteBuf byteBuf = (ByteBuf) msg;
            s = byteBuf.toString(Charset.defaultCharset());
        } else if (msg instanceof String) {
            s = (String) msg;
        } else {
            s = "unknown";
        }

        if (s.equalsIgnoreCase(Node.HEART_BEAT)) {
            metTimeout = true;
        } else if (s.equalsIgnoreCase(Node.HEART_BEAT_START)) {
            sendHeartBeat();
        } else {
            ctx.fireChannelRead(msg);
        }
    }

    public synchronized void sendHeartBeat() {
        iterations++;

        if (iterations % 100 == 0) {
            System.gc();
        }

        if (!channel.isOpen()) {
            logger.error("closed channel");
        }

        AlarmClock.getInstance().scheduleOnce(this, Alarms.HEART_BEAT,
                Miranda.getProperties().getLongProperty(Miranda.PROPERTY_HEART_BEAT_INTERVAL));
        if (Miranda.getProperties().getBooleanProperty(Miranda.PROPERTY_USE_HEARTBEATS)) {
            if (System.currentTimeMillis() >
                    timeOfLastActivity + Miranda.getProperties().getLongProperty(Miranda.PROPERTY_HEART_BEAT_INTERVAL)) {
                String message = Node.HEART_BEAT_START;
                ByteBuf byteBuf = Unpooled.copiedBuffer(message.getBytes());
                ChannelFuture future = channel.writeAndFlush(byteBuf);
                long delay = Miranda.getProperties().getLongProperty(Miranda.PROPERTY_HEART_BEAT_TIMEOUT);
                AlarmClock.getInstance().scheduleOnce(this, Alarms.HEART_BEAT_TIMEOUT, delay);
                try {
                    future.await();
                    if (!future.isSuccess()) {
                        logger.error("heart beat start was a failure");
                    }
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }

            }
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause)
            throws Exception {
        logger.error(cause);
        ctx.fireExceptionCaught(cause);
    }
}
