package itu.myframework;

import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;

import itu.myframework.annotation.Controller;
import itu.myframework.annotation.URL;
import itu.myframework.util.Scanner;
import itu.myframework.util.URLMapping;
import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class RequestControllerServlet extends HttpServlet {
    private HashMap<String, URLMapping> mappings;

    public void init(ServletConfig config) throws ServletException {
        super.init(config);

        String packageName = config.getInitParameter("package-name");

        if (packageName == null || packageName.isEmpty()) {
            packageName = "";
        }

        try {
            mappings = new HashMap<>();

            List<Class<?>> controllers = Scanner.getAnnotatedClasses(packageName, Controller.class);
            
            for (Class<?> ctrl : controllers) {
                List<Method> annotatedMethods = Scanner.findAnnotatedMethods(ctrl, URL.class);

                for (Method method : annotatedMethods) {
                    URL urlAnnotation = method.getAnnotation(URL.class);

                    mappings.put(urlAnnotation.value(), new URLMapping(ctrl, method));
                }
            }
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
        
        String contextPath = req.getContextPath();

        String url = req.getRequestURI().substring(contextPath.length());

        URLMapping urlmap = mappings.get(url);

        if (urlmap != null) {
            printer.println("\"" + url + "\"" + " : url associé à la méthode " + urlmap.getAnnotatedMethod().getName() + " de la classe " + urlmap.getTargetClass().getSimpleName());
        } else {
            for (String handledUrl : mappings.keySet()) {
                URLMapping map = mappings.get(handledUrl);
                
                printer.println("Voici les urls qui sont gérés :");
                printer.println("\"" + handledUrl + "\"" + " pour la méthode " + map.getAnnotatedMethod().getName() + " de la classe " + map.getTargetClass().getSimpleName());
            }
        }
    }

}
