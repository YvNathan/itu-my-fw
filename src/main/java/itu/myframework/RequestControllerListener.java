package itu.myframework;

import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;

import itu.myframework.config.AppProperties;
import itu.myframework.routing.MethodMapping;
import itu.myframework.routing.URLMethod;
import itu.myframework.scanner.Scanner;
import itu.myframework.session.Session;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.http.HttpSessionEvent;
import jakarta.servlet.http.HttpSessionIdListener;
import jakarta.servlet.http.HttpSessionListener;

public class RequestControllerListener implements ServletContextListener, HttpSessionListener, HttpSessionIdListener {
    private static final String SPRING_ROOT_CONTEXT_ATTRIBUTE = "org.springframework.web.context.WebApplicationContext.ROOT";

    private ConcurrentHashMap<String, Session> sessionMap;

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        try {
            AppProperties.load("application.properties");
        } catch (Exception e) {
            throw new IllegalStateException("Lecture de application.properties impossible", e);
        }

        ServletContext servletContext = sce.getServletContext();

        String packageName = servletContext.getInitParameter("package-name");

        if (packageName == null || packageName.isEmpty()) {
            packageName = "";
        }

        HashMap<URLMethod, MethodMapping> mappings = new HashMap<>();
        try {
            Scanner.getMappings(packageName, mappings);
            
            HashMap<Class<?>, Object> controllers = new HashMap<>();
            for (MethodMapping mapping : mappings.values()) {
                Class<?> ctrl = mapping.getTargetClass();
                if (!controllers.containsKey(ctrl)) {
                    controllers.put(ctrl, ctrl.getDeclaredConstructor().newInstance());
                }
            }

            servletContext.setAttribute("mappings", mappings);
            servletContext.setAttribute("controllers", controllers);
        } catch (Exception e) {
            e.printStackTrace();
        }

        String authSessionKey = servletContext.getInitParameter("auth-session-key");
        if (authSessionKey == null || authSessionKey.isBlank()) {
            authSessionKey = "user";
        }
        servletContext.setAttribute("authSessionKey", authSessionKey);

        String roleSessionKey = servletContext.getInitParameter("role-session-key");
        if (roleSessionKey == null || roleSessionKey.isBlank()) {
            roleSessionKey = "role";
        }
        servletContext.setAttribute("roleSessionKey", roleSessionKey);

        sessionMap = new ConcurrentHashMap<>();
        servletContext.setAttribute("sessionMap", sessionMap);

        servletContext.setAttribute("springContext", servletContext.getAttribute(SPRING_ROOT_CONTEXT_ATTRIBUTE));
    }

    @Override
    public void sessionDestroyed(HttpSessionEvent se) {
        sessionMap.remove(se.getSession().getId());
    }

    @Override
    public void sessionIdChanged(HttpSessionEvent se, String oldSessionId) {
        Session session = sessionMap.remove(oldSessionId);
        if (session != null) {
            sessionMap.put(se.getSession().getId(), session);
        }
    }
}
