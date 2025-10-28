package com.ltsllc.miranda.netty;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.ChannelPromise;

import java.io.ByteArrayOutputStream;
import java.nio.charset.Charset;

public class DoNothingOutboundChannelHandler extends ChannelOutboundHandlerAdapter {
    protected String name;

    public DoNothingOutboundChannelHandler(String newName) {
        name = newName;
    }

    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
        String s = null;
        ByteBuf byteBuf = null;
        if (msg instanceof ByteBuf) {
            byteBuf = (ByteBuf) msg;
            s = byteBuf.toString(Charset.defaultCharset());
        } else if (msg instanceof String) {
            s = (String) msg;
        }
        byte[] bytes = byteBuf.array();
        int i = 0;
        i++;
        ctx.writeAndFlush(msg);
    }

    public void exceptionCaught (ChannelHandlerContext ctx, Throwable cause) {
        cause =  cause;
    }
}
