package com.ltsllc.miranda.codec;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.FixedLengthFrameDecoder;
import io.netty.handler.codec.MessageToByteEncoder;

/**
 * A class that views messages as a 4 byte number that indicates the length of the string and then the string.
 *
 * <P>
 *     This class is intended to be used with FixedLengthFrameDecoder to decode messages.
 * </P>
 *
 * @see FixedLengthFrameDecoder
 */
public class StringEncoder extends MessageToByteEncoder<String> {
    @Override
    protected void encode(ChannelHandlerContext channelHandlerContext, String s, ByteBuf byteBuf) throws Exception {
        byteBuf.writeInt(s.length());
        byteBuf.writeBytes(s.getBytes());
        channelHandlerContext.writeAndFlush(byteBuf);
    }
}
