package com.ltsllc.miranda.netty;

import com.ltsllc.miranda.Miranda;
import com.ltsllc.miranda.cluster.Node;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.util.CharsetUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class HeartBeatHandlerTest {
    protected HeartBeatHandler handler = null;

    @BeforeEach
    public void beforeEach () {
        EmbeddedChannel channel = new EmbeddedChannel();
        handler = new HeartBeatHandler(channel, null);
    }

    @Test
    void encode() throws InterruptedException {
        Miranda miranda = new Miranda();
        EmbeddedChannel channel = new EmbeddedChannel();
        channel.pipeline().addLast(handler);
        long now = System.currentTimeMillis();
        synchronized (this) {
            wait(100);
        }

        String message = Node.HEART_BEAT;
        channel.writeOneOutbound(message);

        assert (handler.getTimeOfLastActivity() > now);
    }

    @Test
    void decode() {
        EmbeddedChannel channel = new EmbeddedChannel();
        channel.pipeline().addLast(handler);

        String message = Node.HEART_BEAT;
        ByteBuf byteBuf = Unpooled.copiedBuffer(message, CharsetUtil.UTF_8);
        channel.writeInbound(byteBuf);

        assert (!handler.isLoopback() && handler.isOnline());
    }

    @Test
    void heartBeatTimeout() {
    }

    @Test
    void sendHeartBeat() {
    }
}