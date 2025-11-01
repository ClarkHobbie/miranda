package com.ltsllc.miranda.netty;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.nio.charset.Charset;

/**
 * Add a null byte to the end of outgoing messages.
 */
public class NullTerminatedOutboundFrame extends ChannelOutboundHandlerAdapter {
    public static Logger logger = LogManager.getLogger(NullTerminatedOutboundFrame.class);

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
        ChannelFuture channelFuture = ctx.writeAndFlush(msg, promise);
        ChannelFutureListener channelFutureListener = new ChannelFutureListener() {
            @Override
            public void operationComplete(ChannelFuture channelFuture) throws Exception {
                if (channelFuture.isSuccess()) {
                    logger.debug ("sent successfully");
                } else {
                    logger.error("send failed");
                    channelFuture.cause().printStackTrace();
                }
            }
        };
        channelFuture.addListener(channelFutureListener);
    }

    public void exceptionCaught (ChannelHandlerContext ctx, Throwable cause) {
        int i = 0;
        i++;
    }
}
