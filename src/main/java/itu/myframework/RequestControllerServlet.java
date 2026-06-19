package itu.myframework;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;

import itu.myframework.util.Scanner;
import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class RequestControllerServlet extends HttpServlet {
    private List<Class<?>> controllers;

    public void init(ServletConfig config) throws ServletException {
        super.init(config);

        String packageName = config.getInitParameter("package-name");

        if (packageName == null || packageName.isEmpty()) {
            packageName = "";
        }

        try {
            controllers = Scanner.getClasses(packageName);
        } catch (Exception e) {
            throw new ServletException("Impossible de scanner le package");
        }
    }

    public void doGet(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        processRequest(req, res);
    }

    public void doPost(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        processRequest(req, res);
    }

    private void processRequest(HttpServletRequest req, HttpServletResponse res) throws IOException {
        res.setContentType("text/plain");
        PrintWriter printer = res.getWriter();

        for (Class<?> ctrl : controllers) {
            printer.println(ctrl.getName());
        }
    }

}
