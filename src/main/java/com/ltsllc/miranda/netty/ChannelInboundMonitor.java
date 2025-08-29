package com.ltsllc.miranda.netty;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.ReferenceCountUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.nio.charset.Charset;

/**
 * A {@link ChannelInboundHandlerAdapter} that logs its messages and converts them to strings.
 */
public class ChannelInboundMonitor extends ChannelInboundHandlerAdapter {

    private static Logger logger = LogManager.getLogger(ChannelInboundMonitor.class);
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
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
