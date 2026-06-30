package itu.myframework;

import java.util.HashMap;

import itu.myframework.routing.MethodMapping;
import itu.myframework.routing.URLMethod;
import itu.myframework.scanner.Scanner;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;

public class RequestControllerListener implements ServletContextListener {
    @Override
    public void contextInitialized(ServletContextEvent sce) {
        ServletContext servletContext = sce.getServletContext();

        String packageName = servletContext.getInitParameter("package-name");

        if (packageName == null || packageName.isEmpty()) {
            packageName = "";
        }

        HashMap<URLMethod, MethodMapping> mappings = new HashMap<>();
        try {
            Scanner.getMappings(packageName, mappings);
            servletContext.setAttribute("mappings", mappings);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
