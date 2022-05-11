package com.ltsllc.miranda.servlets;

import com.ltsllc.miranda.cluster.Cluster;
import jakarta.servlet.*;


import java.io.IOException;
import java.io.Writer;

public class NumberOfConnectionsServlet implements Servlet {
    public NumberOfConnectionsServlet() {

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
        servletResponse.setContentType("html");
        Writer writer = servletResponse.getWriter();
        writer.write("<html>");
        writer.write(" " + Cluster.getInstance().getNumberOfConnections());
        writer.write("</html>");

    }

    @Override
    public String getServletInfo() {
        return null;
    }

    @Override
    public void destroy() {

    }

}
