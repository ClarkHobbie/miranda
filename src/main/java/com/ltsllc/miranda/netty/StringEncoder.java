package com.ltsllc.miranda.netty;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.FixedLengthFrameDecoder;
import io.netty.handler.codec.MessageToByteEncoder;
import io.netty.util.ReferenceCountUtil;

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
    /**
     * Create a new message
     * @param channelHandlerContext The context in which the message was created.
     * @param s The message.
     * @param byteBuf The new message.
     */
    @Override
    protected void encode(ChannelHandlerContext channelHandlerContext, String s, ByteBuf byteBuf) {
        try {
            ByteBuf byteBuf1 = Unpooled.buffer();
            byteBuf1.writeInt(s.length());
            byteBuf1.writeBytes(s.getBytes());
            channelHandlerContext.writeAndFlush(byteBuf1);
        } finally {
            ReferenceCountUtil.release(byteBuf);
        }
    }
}
