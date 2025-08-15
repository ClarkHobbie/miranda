package com.ltsllc.miranda.servlets;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class PostReceiver extends HttpServlet {
    public static final String PARAM_DELIVERY_URL = "DELIVERY_URL";
    public static final String PARAM_STATUS_URL = "STATUS_URL";
    public static final String PARAM_CONTENT = "CONTENT";

    public static Logger logger = LogManager.getLogger(PostReceiver.class);

    @Override
    public void doPost (HttpServletRequest request, HttpServletResponse response)
        throws ServletException
    {
        String deliveryURL = request.getParameter(PARAM_DELIVERY_URL);
        String statusURL = request.getParameter(PARAM_STATUS_URL);
        String content = new String(request.getParameter(PARAM_CONTENT));

        logger.debug("Received POST with deliver URL: " + deliveryURL +
                " and status URL: " + statusURL +
                " and content: " + content
        );
    }
}
