package com.ltsllc.miranda.servlets;

import java.io.IOException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class PostServlet extends HttpServlet {
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String data = request.getParameter("data"); // Get data from POST request
        System.out.println("Received POST data: " + data);

        response.setContentType("text/plain");
        response.getWriter().println("Data received successfully: " + data);
    }
}
