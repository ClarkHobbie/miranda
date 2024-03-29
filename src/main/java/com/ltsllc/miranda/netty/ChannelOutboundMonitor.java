package com.ltsllc.miranda.netty;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.ChannelPromise;
import io.netty.util.ReferenceCountUtil;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import java.nio.charset.Charset;

public class ChannelOutboundMonitor extends ChannelOutboundHandlerAdapter {
    private Logger logger = LogManager.getLogger(ChannelOutboundMonitor.class);

    public void write (ChannelHandlerContext ctx, Object msg, ChannelPromise promise) {
        try {
            if (msg instanceof ByteBuf) {
                ByteBuf byteBuf = (ByteBuf) msg;
                String s = byteBuf.toString(Charset.defaultCharset());
                logger.debug(s);
            }
        } finally {
            ReferenceCountUtil.release(msg);
        }
    }
}
