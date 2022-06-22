package com.ltsllc.miranda.codec;

import com.ltsllc.miranda.Miranda;
import com.ltsllc.miranda.alarm.Alarmable;
import com.ltsllc.miranda.alarm.Alarms;
import com.ltsllc.miranda.cluster.Cluster;
import com.ltsllc.miranda.cluster.Node;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;

import java.nio.charset.Charset;
import java.util.UUID;

public class HeartBeat extends ChannelDuplexHandler implements Alarmable {
    protected long timeOfLastActivity = -1;
    protected Channel channel;

    protected long start;

    protected UUID uuid;
    public static final ByteBuf HEARTBEAT = Unpooled.wrappedBuffer(Node.HEART_BEAT.getBytes());

    public HeartBeat (Channel channel, UUID uuid) {
        this.channel = channel;
        this.uuid = uuid;
        start = System.currentTimeMillis();
    }

    public void	flush(ChannelHandlerContext ctx) {
        updateTimeOfLastActivity();
        ctx.flush();
    }

    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        ByteBuf byteBuf = (ByteBuf) msg;
        String s = byteBuf.toString(Charset.defaultCharset());
        if (s.equals(Node.HEART_BEAT)) {
            if (System.currentTimeMillis()
                    > Miranda.getProperties().getLongProperty(Miranda.PROPERTY_HEART_BEAT_TIMEOUT) + start) {
                Cluster.getInstance().deadNode(uuid);
            }
        }
    }
    public void	read(ChannelHandlerContext ctx) {
        updateTimeOfLastActivity();
        ctx.read();
    }

    public void	write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) {
        updateTimeOfLastActivity();
        ctx.write(msg, promise);
    }

    public void updateTimeOfLastActivity () {
        timeOfLastActivity = System.currentTimeMillis();
    }

    @Override
    public void alarm(Alarms alarm) throws Throwable {
        if ((timeOfLastActivity < 0)
                || System.currentTimeMillis()
                > (timeOfLastActivity + Miranda.getProperties().getLongProperty(Miranda.PROPERTY_HEART_BEAT_INTERVAL)))
        {
            sendHeartBeat();
            start = System.currentTimeMillis();
        }
    }

    public void sendHeartBeat() {
        channel.writeAndFlush(HEARTBEAT);
    }
}