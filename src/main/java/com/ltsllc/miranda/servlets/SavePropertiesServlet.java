package com.ltsllc.miranda.servlets;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.ltsllc.miranda.Miranda;
import jakarta.servlet.*;

import java.io.IOException;
import java.util.stream.Collectors;

/**
 * A servlet that saves any new values from the properties page
 *
 * <H>
 *     To avoid unpredictable results, the request is assumed to contain a table whose row adhere to a certain form:
 *     that is &lt;property name&gt;&lt;property value&gt; where the property name must be identical to those used by
 *     the properties page.
 * </H>
 */
public class SavePropertiesServlet implements Servlet {
    @Override
    public void init(ServletConfig config) throws ServletException {

    }

    @Override
    public ServletConfig getServletConfig() {
        return null;
    }

    /**
     * If a property is value in the table is different from it's associated property, then change the property so that
     * its value is the one from the table
     *
     * @param req The ServletRequest for the service.
     * @param res The ServletResponse from the service.
     * @throws ServletException The servlet does not throw this exception and it is here by requirement.
     * @throws IOException This exception is thrown by the request if there is a problem getting a reader.
     */
    @Override
    public void service(ServletRequest req, ServletResponse res) throws ServletException, IOException {
        String json = req.getReader().lines().collect(Collectors.joining(System.lineSeparator()));
        GsonBuilder gsonBuilder = new GsonBuilder();
        gsonBuilder.setPrettyPrinting();
        Gson gson = gsonBuilder.create();

        String[][] table = gson.fromJson(json, String[][].class);
        if (Miranda.getProperties().isDifferentFrom(table)) {
            for (int i = 0; i < table.length; i++) {
                if (Miranda.getProperties().propertyIsDifferent(table[i])) {
                    Miranda.getProperties().setProperty(table[i][0], table[i][1]);
                }
            }
        }

    }

    @Override
    public String getServletInfo() {
        return null;
    }

    @Override
    public void destroy() {

    }
}
