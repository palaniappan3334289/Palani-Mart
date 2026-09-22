package com.palani.palanimart.controller;
 
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Controller handling user-facing error views (/error/400, /error/403, /error/404, /error/500).
 */
@WebServlet(name = "ErrorServlet", urlPatterns = {"/error/*"})
public class ErrorServlet extends BaseServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String pathInfo = req.getPathInfo();
        if (pathInfo != null && pathInfo.contains("403")) {
            resp.setStatus(HttpServletResponse.SC_FORBIDDEN);
            forwardToJsp(req, resp, "error/403.jsp");
        } else if (pathInfo != null && pathInfo.contains("400")) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            forwardToJsp(req, resp, "error/400.jsp");
        } else if (pathInfo != null && pathInfo.contains("500")) {
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            forwardToJsp(req, resp, "error/500.jsp");
        } else {
            resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
            forwardToJsp(req, resp, "error/404.jsp");
        }
    }
}
