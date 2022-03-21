package com.ltsllc.miranda;

import io.netty.handler.codec.http.multipart.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.mina.core.service.IoHandler;
import org.apache.mina.core.session.IdleStatus;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.FilterEvent;

import java.net.URLDecoder;
import java.net.http.HttpRequest;
import java.nio.charset.StandardCharsets;

public class MirandaHandler implements IoHandler {
    public static final Logger logger = LogManager.getLogger();

    @Override
    public void sessionCreated(IoSession session) throws Exception {

    }

    @Override
    public void sessionOpened(IoSession session) throws Exception {

    }

    @Override
    public void sessionClosed(IoSession session) throws Exception {

    }

    @Override
    public void sessionIdle(IoSession session, IdleStatus status) throws Exception {

    }

    @Override
    public void exceptionCaught(IoSession session, Throwable cause) throws Exception {

    }

    @Override
    public void messageReceived(IoSession session, Object message) throws Exception {
        logger.debug("entering messageReceived with session = " + session + " and message = " + message);

        String s = (String) message;
        s = s.toUpperCase();
        String temp = URLDecoder.decode(s, StandardCharsets.UTF_8.toString());
        /*
        HttpRequest request = (HttpRequest) s;
        HttpPostRequestDecoder decoder = new HttpPostRequestDecoder(new DefaultHttpDataFactory(false), request);

        InterfaceHttpData data = decoder.getBodyHttpData("fromField1");
        if (data.getHttpDataType() == InterfaceHttpData.HttpDataType.Attribute) {
            Attribute attribute = (Attribute) data;
            String value = attribute.getValue();
            System.out.println("fromField1 :" + value);
        }

         */

        logger.debug("leaving messageReceived");
    }

    @Override
    public void messageSent(IoSession session, Object message) throws Exception {

    }

    @Override
    public void inputClosed(IoSession session) throws Exception {

    }

    @Override
    public void event(IoSession session, FilterEvent event) throws Exception {

    }
}
