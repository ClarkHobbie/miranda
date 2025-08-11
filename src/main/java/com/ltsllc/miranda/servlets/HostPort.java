package com.ltsllc.miranda.servlets;

import com.ltsllc.miranda.Miranda;
import jakarta.servlet.*;

import java.io.IOException;

/**
 * A servlet that gets the host and port properties,
 */
public class HostPort implements Servlet {
    @Override
    public void init(ServletConfig config) throws ServletException {

    }

    @Override
    public ServletConfig getServletConfig() {
        return null;
    }

    @Override
    public void service(ServletRequest req, ServletResponse res) throws ServletException, IOException {
        String host = Miranda.getProperties().getProperty(Miranda.PROPERTY_THIS_HOST);
        String port = Miranda.getProperties().getProperty(Miranda.PROPERTY_THIS_PORT);
        res.setContentType("HTML");
        res.getWriter().write("" + host + ":" + port);
    }

    @Override
    public String getServletInfo() {
        return null;
    }

    @Override
    public void destroy() {

    }
}
