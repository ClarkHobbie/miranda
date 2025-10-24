package com.ltsllc.miranda.netty;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

import java.nio.charset.Charset;

/**
 * Break a string into several substrings each of which is terminated with a null
 */
public class NullTerminatedInboundFrame extends ChannelInboundHandlerAdapter {
    protected StringBuilder stringBuilder = new StringBuilder();

    public void channelRead (ChannelHandlerContext ctx, Object msg) {
        String s = null;

        if (msg instanceof ByteBuf) {
            ByteBuf byteBuf = (ByteBuf) msg;
            s = byteBuf.toString(Charset.defaultCharset());
        } else if (msg instanceof String) {
            s = (String) msg;
        }

        int index = s.indexOf('\u0000');
        while (index != -1) {
            String frame = s.substring(0, index);
            ctx.fireChannelRead(frame);
            s = s.substring(index + 1, s.length());
            index = s.indexOf('\u0000');
        }

        if (index == -1 && s.length() > 0) {
            stringBuilder.append(s);
        }

        ctx.fireChannelRead(s);
    }
}
