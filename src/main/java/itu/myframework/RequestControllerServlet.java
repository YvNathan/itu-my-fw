package itu.myframework;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;

import itu.myframework.routing.MethodMapping;
import itu.myframework.routing.RequestMethod;
import itu.myframework.routing.URLMethod;
import itu.myframework.scanner.Scanner;
import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class RequestControllerServlet extends HttpServlet {
    private HashMap<URLMethod, MethodMapping> mappings;

    public void init(ServletConfig config) throws ServletException {
        super.init(config);

        String packageName = config.getInitParameter("package-name");

        if (packageName == null || packageName.isEmpty()) {
            packageName = "";
        }

        try {
            mappings = Scanner.getMappings(packageName);
        } catch (Exception e) {
            throw new ServletException("Impossible de scanner le package", e);
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
        
        String contextPath = req.getContextPath();

        String url = req.getRequestURI().substring(contextPath.length());
        RequestMethod requestMethod = RequestMethod.valueOf(req.getMethod());

        MethodMapping urlmap = mappings.get(new URLMethod(url, requestMethod));

        if (urlmap != null) {
            printer.println("\"" + url + "\"" + " : url associé à la méthode " + urlmap.getAnnotatedMethod().getName() + " de la classe " + urlmap.getTargetClass().getSimpleName());
        } else {
            printer.println("Voici les urls qui sont gérés :");
            for (URLMethod urlMethod : mappings.keySet()) {
                MethodMapping map = mappings.get(urlMethod);

                String urlValue = urlMethod.getUrl();
                RequestMethod rm = urlMethod.getRequestMethod();
                
                printer.println(rm + ": \"" + urlValue + "\"" + " pour la méthode " + map.getAnnotatedMethod().getName() + " de la classe " + map.getTargetClass().getSimpleName());
            }
        }
    }

}
