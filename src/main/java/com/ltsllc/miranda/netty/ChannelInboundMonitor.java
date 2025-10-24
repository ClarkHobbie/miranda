package com.ltsllc.miranda.netty;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.ReferenceCountUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.nio.charset.Charset;

/**
 * A {@link ChannelInboundHandlerAdapter} that logs its messages.
 */
public class ChannelInboundMonitor extends ChannelInboundHandlerAdapter {

    private static Logger logger = LogManager.getLogger(ChannelInboundMonitor.class);

    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        try {
            String s = null;
            if (msg instanceof ByteBuf) {
                ByteBuf byteBuf = (ByteBuf) msg;
                s = byteBuf.toString(Charset.defaultCharset());
                byteBuf.release();
            }
            else if (msg instanceof String){
                String temp = (String) msg;
                s = temp;
            } else {
                logger.error("unknown message: " + msg.toString());
                return;
            }
            logger.debug(s);
            ctx.fireChannelRead(msg);
        } finally {
            //ReferenceCountUtil.release(msg);
        }
    }

    public void exceptionCaught (ChannelHandlerContext ctx, Throwable cause) {
        logger.error (cause);
    }
}
