package com.ltsllc.miranda.servlets;

import com.ltsllc.miranda.MessageLog;
import jakarta.servlet.*;

import java.io.IOException;

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
