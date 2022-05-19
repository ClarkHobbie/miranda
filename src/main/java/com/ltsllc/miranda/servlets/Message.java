package com.ltsllc.miranda.servlets;

import com.ltsllc.miranda.Miranda;
import com.ltsllc.miranda.message.MessageLog;
import jakarta.servlet.*;

import java.io.IOException;
import java.util.UUID;

public class Message implements Servlet {
    public static final String PARAM_DESTINATION_URL = "DELIVER_URL";
    public static final String PARAM_STATUS_URL = "STATUS_URL";

    @Override
    public void init(ServletConfig config) throws ServletException {

    }

    @Override
    public ServletConfig getServletConfig() {
        return null;
    }

    @Override
    public void service(ServletRequest req, ServletResponse res) throws ServletException, IOException {
        String statusURL = req.getParameter(PARAM_STATUS_URL);
        String deliveryURL = req.getParameter(PARAM_DESTINATION_URL);
        byte[] contents = req.getInputStream().readAllBytes();

        com.ltsllc.miranda.message.Message message = new com.ltsllc.miranda.message.Message();
        message.setStatusURL(statusURL);
        message.setDeliveryURL(deliveryURL);
        message.setContents(contents);
        message.setMessageID(UUID.randomUUID());

        MessageLog.getInstance().add(message, Miranda.getInstance().getMyUuid());

        res.getWriter().write(message.getMessageID().toString());
    }

    @Override
    public String getServletInfo() {
        return null;
    }

    @Override
    public void destroy() {

    }
}
