package com.ltsllc.miranda.message;


import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.config.Configurator;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class MessageHandlerTest {
    public static final Logger logger = LogManager.getLogger();
    @BeforeAll
    static void setup () {
        Configurator.setRootLevel(Level.DEBUG);
    }

    @Test
    public void handle () {
        // because you can't simulate a ServletOutputWriter
    }
    /*
    @Test
    public void handle() throws LtsllcException, IOException {
        Miranda miranda = new Miranda();
        miranda.loadProperties();

        InputStream mockInputStream = IOUtils.toInputStream("hi there",     "UTF-8");

        PrintWriter mockPrintWriter = Mockito.mock(PrintWriter.class);
        HttpServletRequest mockServletRequest = mock(HttpServletRequest.class);

        byte[] myBinaryData = "hi there".getBytes();
        ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(myBinaryData);


        HttpServletResponse mockServletResponse = mock(HttpServletResponse.class);
        Mockito.when(mockServletResponse.getWriter()).thenReturn(byteArrayInputStream);

        Request mockJettyRequest = mock(Request.class);
        when(mockJettyRequest.getParameter(MessageHandler.PARAM_STATUS_URL)).thenReturn("HTTP://google.com");
        when(mockJettyRequest.getParameter(MessageHandler.PARAM_DESTINATION_URL)).thenReturn("http://google.com");
        when(mockJettyRequest.getInputStream()).thenReturn((ServletInputStream) byteArrayInputStream);
        when(mockJettyRequest.getContentLength()).thenReturn(8);
        MessageHandler messageHandler = new MessageHandler();

        messageHandler.handle("hi there", mockJettyRequest, mockServletRequest, mockServletResponse);

        List<Message> list = miranda.getNewMessageQueue();
        assert (list.size() > 0);
        logger.debug("The new message queue is not empty");
    }

     */
}