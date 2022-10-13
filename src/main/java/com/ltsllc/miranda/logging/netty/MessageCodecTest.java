package com.ltsllc.miranda.logging.netty;

import com.ltsllc.miranda.TestSuperclass;
import com.ltsllc.miranda.netty.StringEncoder;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import org.junit.jupiter.api.Test;

import java.nio.charset.Charset;
import java.util.ArrayList;


import static org.mockito.Mockito.mock;

public class MessageCodecTest extends TestSuperclass {

    public void decode01 () throws Exception {
        String s = "Howdy";
        String newString = " " + s.length() + " " + s;
        s = "whatever";
        newString += " " + s.length() + " " + s;

        ByteBuf byteBuf = Unpooled.wrappedBuffer (newString.getBytes());
        // MessageCodec messageCodec = new MessageCodec();
        ArrayList<Object> al = new ArrayList<>();
        ChannelHandlerContext channelHandlerContext = mock(ChannelHandlerContext.class);
        // messageCodec.decode(channelHandlerContext, byteBuf, al);
        assert (al.size() == 2);

    }

    @Test
    public void decode02 () throws Exception {
        String s = "Howdy";
        String newString = " " + s.length() + " " + s;
        s = "whatever";
        newString += " " + s.length() + " " + s;
        s = "liwpi";
        newString += " " + s.length() + " " + "liw";

        ByteBuf byteBuf = Unpooled.wrappedBuffer (newString.getBytes());
        // MessageCodec messageCodec = new MessageCodec();
        ChannelHandlerContext channelHandlerContext = mock(ChannelHandlerContext.class);
        // messageCodec.decode(channelHandlerContext, byteBuf, al);

    }

    @Test
    public void decode03 () {
        // EmbeddedChannel channel = new EmbeddedChannel(new MessageCodec());
        String s = "Howdy";
        String newString = " " + s.length() + " " + s;
        ByteBuf byteBuf = Unpooled.wrappedBuffer(newString.getBytes());
        // channel.writeInbound(byteBuf);
        // ByteBuf message = channel.readInbound();
        // String outstr = message.toString(Charset.defaultCharset());
        // assert(outstr.equals("Howdy"));
    }
    @Test
    public void decode04() {
        String s = "Howdy";
        String newString = " " + s.length() + " " + s;
        s = "whatever";
        newString += " " + s.length() + " " + s;
        s = "liwpi";
        newString += " " + s.length() + " " + "liw";
        ByteBuf byteBuf = Unpooled.wrappedBuffer(newString.getBytes());
        // EmbeddedChannel channel = new EmbeddedChannel(new MessageCodec());
        // channel.writeInbound(byteBuf);
        // ByteBuf message = channel.readInbound();
        // String out = message.toString(Charset.defaultCharset());
        // assert(out.equals("Howdy"));
        // message = channel.readInbound();
        // out = new String(message.toString(Charset.defaultCharset()));
        // assert(out.equals("whatever"));
        // message = channel.readInbound();
        // assert(message == null);
        s = "ipi";
        byteBuf = Unpooled.wrappedBuffer(s.getBytes());
        // channel.writeInbound(byteBuf);
        // message = channel.readInbound();
        // out = message.toString(Charset.defaultCharset());
        // assert (out.equals("liwipi"));
    }


    public void decode05() {
        EmbeddedChannel channel = new EmbeddedChannel(new LengthFieldBasedFrameDecoder(1024,0,2,0,2));
        String s1 = "Howdy";
        int i = s1.length();

        byte[] buff1 = shortToBytes((short) i);

        String s2 = "liwipi";
        i = s2.length() - 2;
        byte[] buff2 = shortToBytes((short) i);

        ByteBuf byteBuf = Unpooled.wrappedBuffer(buff1, s1.getBytes(), buff2, s2.getBytes());

        channel.writeInbound(byteBuf);
        ByteBuf message = channel.readInbound();
        String out = message.toString(Charset.defaultCharset());
        assert (out.equals("Howdy"));
        message = channel.readInbound();
        assert (message == null);


    }

    @Test
    public void decode06 () {
        String s1 = "liwipi";
        byte[] buff = shortToBytes((short) s1.length());
        ByteBuf byteBuf = Unpooled.wrappedBuffer(buff,"liw".getBytes());
        EmbeddedChannel channel = new EmbeddedChannel(new LengthFieldBasedFrameDecoder(1024,0,2,0,2));

        channel.writeInbound(byteBuf);
        ByteBuf message = channel.readInbound();
        assert(message == null);

        byteBuf = Unpooled.wrappedBuffer("ipi".getBytes());
        channel.writeInbound(byteBuf);
        message = channel.readInbound();
        String s = message.toString(Charset.defaultCharset());
        assert (s.equals("liwipi"));
    }

    @Test
    public void decode07 () {
        String s = "whatever";
        ByteBuf byteBuf = Unpooled.buffer();
        byteBuf.writeShort(s.length());
        byteBuf.writeBytes(s.getBytes());

        EmbeddedChannel channel = new EmbeddedChannel(new LengthFieldBasedFrameDecoder(1024,0,2,0,2));
        channel.writeInbound(byteBuf);
        ByteBuf message = channel.readInbound();
        String s2 = message.toString(Charset.defaultCharset());
        assert (s2.equals("whatever"));
    }

    public byte[] shortToBytes (short s) {

        byte[] temp = new byte[2];

        temp[0] = (byte)(s >> 8);
        temp[1] = (byte)(s & 0xFF);
        return temp;
    }

    @Test
    public void decode08 () {
        //EmbeddedChannel channel = new EmbeddedChannel(new MessageCodec());
        String s1 = "Howdy";
        // channel.writeInbound(s1);
        // String s2 = channel.readInbound();
        // assert (s2.equals("Howdy"));
    }


    public void encode01 (){
        // EmbeddedChannel channel = new EmbeddedChannel(new MessageCodec());
        // channel.writeOneOutbound("Howdy");
        // ByteBuf byteBuf = channel.readOutbound();
        // int length = byteBuf.readShort();
        // assert (length == "Howdy".length());
        // String s = byteBuf.toString(Charset.defaultCharset());
        // assert (s.equals("Howdy"));
    }

    @Test
    public void encode02 () {
        // EmbeddedChannel channel = new EmbeddedChannel(new MessageCodec());
        ByteBuf byteBuf = Unpooled.buffer();
        String s = "liwipi";
        byteBuf.writeShort(s.length());
        byteBuf.writeBytes("liw".getBytes());
        // channel.writeInbound(byteBuf);
        // Object o = channel.readInbound();
        // assert (o == null);
    }


    public void encode03 () {
        EmbeddedChannel channel = new EmbeddedChannel(new StringEncoder());
        String s = "Howdy";
        channel.writeOneOutbound(s);
        ByteBuf byteBuf = channel.readOutbound();
        short temp = byteBuf.readShort();
        assert (temp == s.length());
        String s2 = byteBuf.toString(Charset.defaultCharset());
        assert(s2.equals("Howdy"));
    }
}
