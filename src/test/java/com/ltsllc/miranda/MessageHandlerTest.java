package com.ltsllc.miranda;



import com.ltsllc.commons.LtsllcException;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.config.Configurator;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.util.MultiMap;
import org.junit.Test;
import org.junit.jupiter.api.BeforeAll;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class MessageHandlerTest {
    public static final Logger logger = LogManager.getLogger();
    @BeforeAll
    void setup () {
        Configurator.setRootLevel(Level.DEBUG);
    }

    @Test
    public void handle() throws LtsllcException, IOException {
        Configurator.setRootLevel(Level.DEBUG);
        logger.debug("handle");

        ServletInputStream mockInputStream = mock(ServletInputStream.class);
        when(mockInputStream.read()).thenReturn(15);

        HttpServletRequest mockServletRequest = mock(HttpServletRequest.class);
        when(mockServletRequest.getInputStream()).thenReturn(mockInputStream);

        HttpServletResponse mockServletResponse = mock(HttpServletResponse.class);

        Request jettyRequest = mock(Request.class);
        when(jettyRequest.getParameter(MessageHandler.PARAM_STATUS_URL)).thenReturn("HTTP://google.com");
        when(jettyRequest.getParameter(MessageHandler.PARAM_DESTINATION_URL)).thenReturn("http://google.com");
        when(jettyRequest.getInputStream()).thenReturn(mockInputStream);

        Miranda miranda = new Miranda();
        miranda.loadProperties();
        List<Message> list = new ArrayList<>();
        miranda.setNewMessageQueue(list);

        MessageHandler messageHandler = new MessageHandler();

        messageHandler.handle("hi there",jettyRequest, mockServletRequest, mockServletResponse);

        list = miranda.getNewMessageQueue();
        assert (list.size() > 0);
        logger.debug("The new message queue is not empty");
    }
}