package com.ltsllc.miranda.netty;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.handler.codec.MessageToMessageCodec;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import java.nio.charset.Charset;
import java.util.List;

/**
 * A {@link MessageToMessageCodec} that logs its messages.
 */
public class ChannelMonitor extends MessageToMessageCodec<ByteBuf,ByteBuf> {
    protected Logger logger = LogManager.getLogger(ChannelMonitor.class);
    @Override
    protected void encode(ChannelHandlerContext channelHandlerContext, ByteBuf byteBuf, List<Object> list) {
        list.add(byteBuf);
        String string = byteBuf.toString(Charset.defaultCharset());
        logger.debug(string);
    }

    @Override
    protected void decode(ChannelHandlerContext channelHandlerContext, ByteBuf byteBuf, List<Object> list) throws Exception {
        list.add(byteBuf);
    }

    @Override
    public void close(ChannelHandlerContext ctx, ChannelPromise promise) {
        int i = 5;
        i++;
    }
}
