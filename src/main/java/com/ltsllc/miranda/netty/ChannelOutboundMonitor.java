package com.ltsllc.miranda.netty;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.buffer.UnpooledByteBufAllocator;
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

    public ChannelOutboundMonitor() {
        logger = logger;
    }

    public void write (ChannelHandlerContext ctx, Object msg, ChannelPromise promise) {
        try {
            String s = null;
            if (msg instanceof ByteBuf) {
                ByteBuf byteBuf = (ByteBuf) msg;
                s = byteBuf.toString(Charset.defaultCharset());
            } else if (msg instanceof String) {
                s = (String) msg;
            }
            logger.debug(s);
            ctx.write(msg);
            //ctx.fireChannelRead(msg);
        } finally {
            //ReferenceCountUtil.release(msg);
        }
    }

    public void exceptionCaught (ChannelHandlerContext ctx, Throwable cause) {
        logger.error (cause);
    }

}
