package com.ltsllc.miranda.servlets;

import jakarta.servlet.ServletException;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Enumeration;
import java.util.Iterator;

public class PostReceiver extends HttpServlet {
    public static final String PARAM_DELIVERY_URL = "DELIVERY_URL";
    public static final String PARAM_STATUS_URL = "STATUS_URL";
    public static final String PARAM_CONTENT = "CONTENT";

    public static Logger logger = LogManager.getLogger(PostReceiver.class);

    @Override
    public void doPost (HttpServletRequest request, HttpServletResponse response)
        throws ServletException
    {
        String content = request.getParameter(PARAM_CONTENT);

        if (content == null) {
            ServletInputStream in = null;
            try {
                in = request.getInputStream();
            } catch (IOException e) {
                throw new ServletException(e);
            }
            byte[] buffer = null;
            try {
                buffer = in.readAllBytes();
            } catch (IOException e) {
                throw new ServletException(e);
            }

            content = (buffer == null) ? "mull" : new String(buffer);
        }

        StringBuilder builder = new StringBuilder();
        Enumeration<String> enumeration = request.getParameterNames();
        while (enumeration.hasMoreElements()) {
            String name = enumeration.nextElement();
            builder.append(name);
            builder.append(" = ");
            String value = request.getParameter(name);
            value = (value == null) ? "null" : value;
            builder.append(value);
            if (enumeration.hasMoreElements()) {
                builder.append(", ");
            }
        }

        logger.debug("Received POST with params: " + builder.toString());
    }
}
