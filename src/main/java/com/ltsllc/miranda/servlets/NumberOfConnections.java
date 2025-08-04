package com.ltsllc.miranda.servlets;

import com.ltsllc.miranda.cluster.Cluster;
import jakarta.servlet.*;

import java.io.IOException;
import java.io.Writer;

/**
 * A servlet that returns the number of connections that the node currently has to other nodes.
 */
public class NumberOfConnections implements Servlet {
    public NumberOfConnections() {

    }


    @Override
    public void init(ServletConfig config) throws ServletException {

    }

    @Override
    public ServletConfig getServletConfig() {
        return null;
    }

    @Override
    public void service(ServletRequest servletRequest, ServletResponse servletResponse) throws ServletException, IOException {
        Writer writer = servletResponse.getWriter();
        servletResponse.setContentType("HTML");
        writer.write("" + Cluster.getInstance().getNumberOfConnections());
    }

    @Override
    public String getServletInfo() {
        return null;
    }

    @Override
    public void destroy() {

    }

}
