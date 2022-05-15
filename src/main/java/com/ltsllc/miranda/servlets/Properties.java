package com.ltsllc.miranda.servlets;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.ltsllc.miranda.Miranda;
import com.ltsllc.miranda.properties.PropertiesHolder;
import jakarta.servlet.*;

import java.io.IOException;

/**
 * A servlet that gathers up the system properties and ships them off as a JSON object.  The object takes the form of a
 * 2-dimensional table where the first column is always the property name and the second column of a row is always that
 * properties value.
 */
public class Properties implements Servlet {
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

        table = new String[p.keySet().size()][2];

        String[] names = new String[p.keySet().size()];
        p.keySet().toArray(names);


        for (int i = 0; i < names.length; i++) {
            table[i][0] =  names[i];
            table[i][1] = p.getProperty(names[i]);

        }

        GsonBuilder gsonBuilder = new GsonBuilder();
        gsonBuilder.setPrettyPrinting();
        Gson gson = gsonBuilder.create();
        String html = gson.toJson(table);
        res.setContentType("HIML");
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
