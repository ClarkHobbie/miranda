package com.ltsllc.miranda.netty;

import com.ltsllc.commons.LtsllcException;
import com.ltsllc.miranda.Miranda;
import com.ltsllc.miranda.alarm.AlarmClock;
import com.ltsllc.miranda.alarm.Alarmable;
import com.ltsllc.miranda.alarm.Alarms;
import com.ltsllc.miranda.cluster.Node;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageCodec;
import io.netty.util.CharsetUtil;
import io.netty.util.ReferenceCountUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

/**
 * A class that enforces the heart beat protocol
 */
public class HeartBeatHandler extends MessageToMessageCodec<ByteBuf, String> implements Alarmable {
    protected static final Logger logger = LogManager.getLogger(HeartBeatHandler.class);

    protected Channel channel;
    protected long iterations = 0;;
    protected boolean metTimeout = false;

    protected boolean isLoopback = false;

    public boolean isLoopback() {
        return isLoopback;
    }

    public void setLoopback(boolean loopback) {
        isLoopback = loopback;
    }

    protected long timeOfLastActivity = -1;

    public long getTimeOfLastActivity() {
        return timeOfLastActivity;
    }

    public void setTimeOfLastActivity(long timeOfLastActivity) {
        this.timeOfLastActivity = timeOfLastActivity;
    }

    protected UUID uuid = null;

    protected boolean online;

    public boolean isOnline() {
        return online;
    }

    public boolean isMetTimeout() {
        return metTimeout;
    }

    public void setMetTimeout(boolean metTimeout) {
        this.metTimeout = metTimeout;
    }

    public void setOnline(boolean online) {
        this.online = online;
    }

    public UUID getUuid() {
        return uuid;
    }

    public void setUuid(UUID uuid) {
        this.uuid = uuid;
    }


    public HeartBeatHandler (Channel channel) {
        this.channel = channel;
        AlarmClock.getInstance().scheduleOnce(this, Alarms.HEART_BEAT,
                Miranda.getProperties().getLongProperty(Miranda.PROPERTY_HEART_BEAT_INTERVAL));
    }


    @Override
    protected void encode(ChannelHandlerContext channelHandlerContext, String s, List<Object> list) {
        if (!isLoopback) {
            timeOfLastActivity = System.currentTimeMillis();
            list.add(s);
        }
   }

    /**
     * Decode a heart beat message.
     *
     * <P>
     *     This method translates a ByteBuf to a string.
     *     If the message is not heart beat related then just pass it on.  This method also updates the
     *     timeOfLastActivity.
     * </P>
     * @param channelHandlerContext The channel context in which the message took place
     * @param byteBuf The message
     * @param list The output messages.
     */
    @Override
    protected void decode(ChannelHandlerContext channelHandlerContext, ByteBuf byteBuf, List<Object> list) {
        try {
            if (!isLoopback) {
                String s = byteBuf.toString(CharsetUtil.UTF_8);
                timeOfLastActivity = System.currentTimeMillis();
                if (s.equals(Node.HEART_BEAT)) {
                    metTimeout = true;
                    online = true;
                } else if (s.equals(Node.HEART_BEAT_START)) {
                    channelHandlerContext.writeAndFlush(Node.HEART_BEAT);
                } else {
                    list.add(byteBuf);
                }
            }
        }
        finally {
            ReferenceCountUtil.release(byteBuf);
        }
    }

    /**
     * Handle an alarm event
     * @param alarm
     * @throws Throwable
     */
    @Override
    public void alarm(Alarms alarm) throws Throwable {
        switch (alarm) {
            case HEART_BEAT ->
                sendHeartBeat();

            case HEART_BEAT_TIMEOUT ->
                heartBeatTimeout();
        }

    }

    /**
     * The node has timed out, close the channel and declare the node dead.
     * @throws IOException
     * @throws LtsllcException
     */
    public void heartBeatTimeout() throws IOException, LtsllcException {
        if (!isLoopback) {
            if (!metTimeout) {
                logger.error("Node has gone offline.");
                channel.close();
            } else {
                metTimeout = false;
            }
        }
    }

    /**
     * Send a heart beat start message.
     * <P>
     *     This method is also responsible for setting an alarm for the next heart beat.
     * </P>
     */
    public void sendHeartBeat() {
        iterations++;
        if (iterations % 100 == 0) {
            System.gc();
        }

        if (!isLoopback) {
            AlarmClock.getInstance().scheduleOnce(this, Alarms.HEART_BEAT,
                    Miranda.getProperties().getLongProperty(Miranda.PROPERTY_HEART_BEAT_INTERVAL));

            if (Miranda.getProperties().getBooleanProperty(Miranda.PROPERTY_USE_HEARTBEATS)) {
                if (System.currentTimeMillis() >
                        timeOfLastActivity + Miranda.getProperties().getLongProperty(Miranda.PROPERTY_HEART_BEAT_INTERVAL)) {
                    channel.writeAndFlush(Node.HEART_BEAT_START);
                    long delay = Miranda.getProperties().getLongProperty(Miranda.PROPERTY_HEART_BEAT_TIMEOUT);
                    AlarmClock.getInstance().scheduleOnce(this, Alarms.HEART_BEAT_TIMEOUT, delay);
               }
            }
        }
    }

    /**
     * An exception occurred - close the channel and declare the node dead.
     *
     * @param ctx The context in which the exception occurred
     * @param cause The exception that triggered the exception
     * @throws Exception If the method has any exceptions
     */
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        if (cause.getMessage().endsWith("Connection reset")) {
            ctx.close();
        }
    }

}