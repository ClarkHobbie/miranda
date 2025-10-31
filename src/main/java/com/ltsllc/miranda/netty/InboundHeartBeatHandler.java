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

    public String direction;
    public Channel channel = null;
    public volatile Boolean metTimeout = false;
    public volatile Long timeOfLastActivity;
    public int iterations = 0;
    public volatile Boolean channelIsLocked;

    public InboundHeartBeatHandler (Boolean channelIsLocked, String direction, Channel channel, Long timeOfLastActivity, Boolean metTimeout) {
        this.channelIsLocked = channelIsLocked;
        this.direction = direction;
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
            logger.debug("met timeout");
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

        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(Node.HEART_BEAT);
        if (!channel.isWritable()) {
            logger.error("channel is not writeable");
            return;
        }
        ByteBuf byteBuf = Unpooled.copiedBuffer(stringBuilder.toString().getBytes());
        logger.debug("reference count after allocation: " + byteBuf.refCnt());
        while (channelIsLocked) {
            try {
                wait(1000);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
        channelIsLocked = true;

        ChannelFuture future = channel.writeAndFlush(byteBuf);
        try {
            future.await();
            channelIsLocked = false;
            byteBuf.retain();
            logger.debug("reference count after await: " + byteBuf.refCnt());
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        if (!future.isSuccess()) {
            logger.error(future.exceptionNow());
            future.cause().printStackTrace();;
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause)
            throws Exception {
        logger.error(cause);
        cause.printStackTrace();
        ctx.fireExceptionCaught(cause);
    }
}
