package com.ltsllc.miranda.servlets;

import com.ltsllc.miranda.logging.MessageLog;
import jakarta.servlet.*;

import java.io.IOException;

/**
 * A servlet that returns the number of messages in the message log.
 */
public class NumberOfMessages implements Servlet {
    @Override
    public void init(ServletConfig config) throws ServletException {

    }

    @Override
    public ServletConfig getServletConfig() {
        return null;
    }

    @Override
    public void service(ServletRequest req, ServletResponse res) throws ServletException, IOException {
        res.setContentType("HTML");
        res.getWriter().write("" + MessageLog.getInstance().getAllMessages().size());
    }

    @Override
    public String getServletInfo() {
        return null;
    }

    @Override
    public void destroy() {

    }
}
