package com.ltsllc.miranda.servlets;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.ltsllc.miranda.Miranda;
import com.ltsllc.miranda.cluster.Cluster;
import com.ltsllc.miranda.message.MessageLog;
import com.ltsllc.miranda.properties.PropertiesHolder;
import jakarta.servlet.*;

import java.io.IOException;

/**
 * A servlet that returns all the info that the status page needs as one big table.
 */
public class Status implements Servlet {
    @Override
    public void init(ServletConfig config) throws ServletException {

    }

    @Override
    public ServletConfig getServletConfig() {
        return null;
    }

    @Override
    public void service(ServletRequest req, ServletResponse res) throws ServletException, IOException {
        String[][] table;
        PropertiesHolder p = Miranda.getProperties();

        table = new String[p.keySet().size() + 3][2];

        String[] names = new String[p.keySet().size() + 3];
        p.keySet().toArray(names);
        names[p.keySet().size()] = "numberOfConnections";
        names[p.keySet().size() + 1] = "numberOfMessages";
        names[p.keySet().size() + 2] = "numberOfNodes";


        for (int i = 0; i < names.length; i++) {
            table[i][0] = names[i];
            table[i][1] = p.getProperty(names[i]);
        }

        String name = "numberOfConnections";
        String value = "" + Cluster.getInstance().getNumberOfConnections();
        int numberOfRows = p.keySet().size() + 3;
        table[numberOfRows - 3][0] = name;
        table[numberOfRows - 3][1] = value;

        name = "numberOfMessages";
        value = "" + MessageLog.getInstance().getAllMessages().size();
        table[numberOfRows - 2][0] = name;
        table[numberOfRows - 2][1] = value;

        name = "numberOfNodes";
        value = "" + Cluster.getInstance().getNumberOfNodes();
        table[numberOfRows - 1][0] = name;
        table[numberOfRows - 1][1] = value;

        GsonBuilder gsonBuilder = new GsonBuilder();
        gsonBuilder.setPrettyPrinting();
        Gson gson = gsonBuilder.create();
        String html = gson.toJson(table);
        res.setContentType("application/json");
        res.getWriter().write(html);
    }

    @Override
    public String getServletInfo() {
        return null;
    }

    @Override
    public void destroy() {

    }
}
