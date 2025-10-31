package com.ltsllc.miranda.netty;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.ChannelPromise;

import java.nio.charset.Charset;

/**
 * Add a null byte to the end of outgoing messages.
 */
public class NullTerminatedOutboundFrame extends ChannelOutboundHandlerAdapter {
    public void write (ChannelHandlerContext ctx, Object msg, ChannelPromise promise) {
        String s = null;
        ByteBuf byteBuf = null;
        if (msg instanceof ByteBuf) {
            byteBuf = (ByteBuf) msg;
            s = byteBuf.toString(Charset.defaultCharset());
        } else if (msg instanceof String) {
            s = (String) msg;
        }

        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(s);
        stringBuilder.append('\u0000');
        msg = Unpooled.copiedBuffer(stringBuilder.toString().getBytes());
        byte[] bytes = ((ByteBuf) msg).array();
        s = ((ByteBuf) msg).toString(Charset.defaultCharset());
        ctx.write(msg, promise);
    }

    public void exceptionCaught (ChannelHandlerContext ctx, Throwable cause) {
        int i = 0;
        i++;
    }
}
