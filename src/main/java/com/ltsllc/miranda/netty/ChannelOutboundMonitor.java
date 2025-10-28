package com.ltsllc.miranda.netty;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.buffer.UnpooledByteBufAllocator;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.ChannelPromise;
import io.netty.util.ReferenceCountUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.nio.charset.Charset;

/**
 * A {@link ChannelOutboundHandlerAdapter} that logs its messages.
 */
public class ChannelOutboundMonitor extends ChannelOutboundHandlerAdapter {
    private Logger logger = LogManager.getLogger(ChannelOutboundMonitor.class);
    public Channel channel;
    public String name;

    public ChannelOutboundMonitor(String name) {
        this.name = name;
    }

    public void write (ChannelHandlerContext ctx, Object msg, ChannelPromise promise) {
        if (channel == null) {
            channel = ctx.channel();
        }
        String s = null;
        ByteBuf byteBuf = null;
        if (msg instanceof ByteBuf) {
            byteBuf = (ByteBuf) msg;
            s = byteBuf.toString(Charset.defaultCharset());
            byteBuf.release();
        } else if (msg instanceof String) {
            s = (String) msg;
        }
        String message = s + channel.toString();

        logger.debug(message);
        byte[] bytes = byteBuf.array();
        byteBuf = Unpooled.copiedBuffer(s.getBytes());
        bytes = byteBuf.array();
        ctx.write(byteBuf, promise);
    }

    public void exceptionCaught (ChannelHandlerContext ctx, Throwable cause) {
        logger.error (cause);
    }

}
