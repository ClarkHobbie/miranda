package com.ltsllc.miranda.servlets;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.ltsllc.miranda.cluster.Cluster;
import com.ltsllc.miranda.cluster.Node;
import jakarta.servlet.*;

import java.io.IOException;
import java.util.List;

/**
 * A servlet that retrieves connection data.
 */
public class ConnectionDetails implements Servlet {
    @Override
    public void init(ServletConfig config) throws ServletException {

    }

    @Override
    public ServletConfig getServletConfig() {
        return null;
    }

    @Override
    public void service(ServletRequest req, ServletResponse res) throws ServletException, IOException {
        List<Node> data = Cluster.getInstance().getNodes();
        String[][] table = new String[data.size()][];
        for (int i = 0; i < data.size(); i++) {
            Node node = data.get(i);
            table[i] = new String[5];
            String[] row = table[i];
            row[0] = "" + i;
            row[1] = node.getChannel().toString();
            row[2] = node.getHost();
            row[3] = node.getUuid().toString();
            row[4] = node.getState().toString();
        }

        GsonBuilder gsonBuilder = new GsonBuilder();
        gsonBuilder.setPrettyPrinting();
        Gson gson = gsonBuilder.create();
        String json = gson.toJson(table);

        res.setContentType("application/json");
        res.getWriter().write(json);
    }

    @Override
    public String getServletInfo() {
        return null;
    }

    @Override
    public void destroy() {

    }
}
