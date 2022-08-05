package com.ltsllc.miranda.netty;

import com.ltsllc.commons.LtsllcException;
import com.ltsllc.miranda.Miranda;
import com.ltsllc.miranda.alarm.AlarmClock;
import com.ltsllc.miranda.alarm.Alarmable;
import com.ltsllc.miranda.alarm.Alarms;
import com.ltsllc.miranda.cluster.Cluster;
import com.ltsllc.miranda.cluster.Node;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageCodec;
import io.netty.util.ReferenceCountUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.List;
import java.util.UUID;

/**
 * A class that enforces the heart beat protocol
 */
public class HeartBeatHandler extends MessageToMessageCodec<ByteBuf, String> implements Alarmable {
    protected static final Logger logger = LogManager.getLogger();

    protected Channel channel;
    protected boolean metTimeout = false;

    protected long timeOfLastActivity = -1;

    protected UUID uuid = null;

    public UUID getUuid() {
        return uuid;
    }

    public void setUuid(UUID uuid) {
        this.uuid = uuid;
    }

    protected Node node;

    public Node getNode() {
        return node;
    }

    public void setNode(Node node) {
        this.node = node;
    }

    public HeartBeatHandler (Channel channel) {
        this.channel = channel;
        AlarmClock.getInstance().scheduleOnce(this, Alarms.HEART_BEAT,
                Miranda.getProperties().getLongProperty(Miranda.PROPERTY_HEART_BEAT_INTERVAL));
    }


    @Override
    protected void encode(ChannelHandlerContext channelHandlerContext, String s, List<Object> list) {
        timeOfLastActivity = System.currentTimeMillis();
        list.add(s);
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
            String s = byteBuf.toString(Charset.defaultCharset());
            timeOfLastActivity = System.currentTimeMillis();
            if (s.equals(Node.HEART_BEAT)) {
                metTimeout = true;
            } else if (s.equals(Node.HEART_BEAT_START)) {
                channelHandlerContext.writeAndFlush(Node.HEART_BEAT);
            } else {
                list.add(s);
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
        if (!metTimeout) {
            logger.error("Node has gone offline.");
            channel.close();
            Cluster.getInstance().removeNode(node);
            Cluster.getInstance().deadNode(uuid,node);
        } else {
            metTimeout = false;
        }
    }

    /**
     * Send a heart beat start message.
     * <P>
     *     This method is also responsible for setting an alarm for the next heart beat.
     * </P>
     */
    public void sendHeartBeat() {
        AlarmClock.getInstance().scheduleOnce(this, Alarms.HEART_BEAT,
                Miranda.getProperties().getLongProperty(Miranda.PROPERTY_HEART_BEAT_INTERVAL));

        if (System.currentTimeMillis() >
                timeOfLastActivity + Miranda.getProperties().getLongProperty(Miranda.PROPERTY_HEART_BEAT_INTERVAL)) {
            channel.writeAndFlush(Node.HEART_BEAT_START);
            AlarmClock.getInstance().scheduleOnce(this, Alarms.HEART_BEAT,
                    Miranda.getProperties().getLongProperty(Miranda.PROPERTY_HEART_BEAT_TIMEOUT));

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
            Cluster.getInstance().removeNode(node);
            Cluster.getInstance().deadNode(uuid,node);
        }
    }
}