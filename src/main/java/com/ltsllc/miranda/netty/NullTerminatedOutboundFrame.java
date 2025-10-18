package com.ltsllc.miranda.netty;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.ChannelPromise;

/**
 * Add a null byte to the end of outgoing messages.
 */
public class NullTerminatedOutboundFrame extends ChannelOutboundHandlerAdapter {
    public void write (ChannelHandlerContext ctx, Object msg, ChannelPromise promise) {
        String s = null;

        if (msg instanceof ByteBuf) {
            ByteBuf byteBuf = (ByteBuf) msg;
            s = byteBuf.toString();
        } else if (msg instanceof String) {
            s = (String) msg;
        }

        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(s);
        stringBuilder.append('\u0000');
        msg = Unpooled.copiedBuffer(stringBuilder.toString().getBytes());
        ctx.write(msg, promise);
        ((ByteBuf) msg).release();
    }
}
