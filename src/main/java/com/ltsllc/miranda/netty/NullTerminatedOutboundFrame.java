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
        MessageChanelFutureListener listener = new MessageChanelFutureListener(s);
        channelFuture.addListener(listener);
    }

    public void exceptionCaught (ChannelHandlerContext ctx, Throwable cause) {
        logger.error ("caught " + cause);
        logger.error("on channel " + ctx.channel());
        cause.printStackTrace();
    }
}
