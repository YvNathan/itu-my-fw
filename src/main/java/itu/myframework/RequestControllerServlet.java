package itu.myframework;

import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

import com.google.gson.Gson;

import itu.myframework.annotation.APIMethod;
import itu.myframework.annotation.Auth;
import itu.myframework.annotation.RequestModel;
import itu.myframework.annotation.RequestParam;
import itu.myframework.binding.Binder;
import itu.myframework.routing.MethodMapping;
import itu.myframework.routing.ModelAndView;
import itu.myframework.routing.RequestMethod;
import itu.myframework.routing.URLMethod;
import itu.myframework.session.Session;
import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

public class RequestControllerServlet extends HttpServlet {
    private Object springContext;
    private final Gson gson = new Gson();

    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);

        springContext = getServletContext().getAttribute("springContext");
    }

    public void doGet(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        processRequest(req, res);
    }

    public void doPost(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        processRequest(req, res);
    }

    private void processRequest(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        PrintWriter printer = res.getWriter();

        String contextPath = req.getContextPath();

        String url = req.getRequestURI().substring(contextPath.length());

        RequestMethod requestMethod = RequestMethod.valueOf(req.getMethod());

        @SuppressWarnings("unchecked")
        HashMap<URLMethod, MethodMapping> mappings = (HashMap<URLMethod, MethodMapping>) getServletContext()
                .getAttribute("mappings");
        MethodMapping urlmap = mappings.get(new URLMethod(url, requestMethod));

        if (urlmap != null) {
            Class<?> targetClass = urlmap.getTargetClass();

            Method annotatedMethod = urlmap.getAnnotatedMethod();

            String authKey = (String) getServletContext().getAttribute("authSessionKey");
            Object userBefore = currentUser(req, authKey);

            if (isAuthRequired(url, annotatedMethod, targetClass) && userBefore == null) {
                denyAccess(annotatedMethod, req, res);
                return;
            }

            try {
                Object instance = targetClass.getDeclaredConstructor().newInstance();

                Object[] arguments = resolveArguments(annotatedMethod, req, res, springContext);

                Object result = annotatedMethod.invoke(instance, arguments);

                for (Object argument : arguments) {
                    if (argument instanceof Session) {
                        ((Session) argument).syncHttpSession(req);
                    }
                }

                if (!Objects.equals(userBefore, currentUser(req, authKey)) && req.getSession(false) != null) {
                    req.changeSessionId();
                }

                if (annotatedMethod.isAnnotationPresent(APIMethod.class)) {
                    handleApiResponse(result, res, printer);
                    return;
                } else if (result instanceof ModelAndView) {
                    handleViewReponse((ModelAndView) result, req, res);
                    return;
                } else {
                    handleSimpleResponse(result, res, printer);
                    return;
                }
            } catch (Exception e) {
                Throwable cause = (e instanceof InvocationTargetException && e.getCause() != null) ? e.getCause() : e;
                throw new ServletException("Impossible d'exécuter la méthode : " + cause, cause);
            }
        } else {
            listURLs(res, printer, mappings);
        }
    }

    @SuppressWarnings("unchecked")
    private Object[] resolveArguments(Method annotatedMethod, HttpServletRequest req, HttpServletResponse res,
            Object springContext) throws ServletException {
        Parameter[] params = annotatedMethod.getParameters();
        Object[] args = new Object[params.length];

        for (int i = 0; i < params.length; i++) {
            RequestParam rp = params[i].getAnnotation(RequestParam.class);
            RequestModel rm = params[i].getAnnotation(RequestModel.class);
            if (params[i].getType() == Session.class) {
                ConcurrentHashMap<String, Session> sessionMap = (ConcurrentHashMap<String, Session>) getServletContext()
                        .getAttribute("sessionMap");
                Session session = sessionMap.computeIfAbsent(req.getSession().getId(), id -> new Session(req));
                session.reload(req);
                args[i] = session;
            } else if (rm != null) {
                Object model = Binder.bindModel(req, params[i].getParameterizedType(), rm.value());
                args[i] = model;
            } else if (rp != null) {
                args[i] = Binder.bindRequestParam(rp, params[i].getParameterizedType(), req);
            } else {
                args[i] = springContext;
            }
        }

        return args;
    }

    private boolean isAuthRequired(String url, Method annotatedMethod, Class<?> targetClass) {
        if (!annotatedMethod.isAnnotationPresent(Auth.class) && !targetClass.isAnnotationPresent(Auth.class)) {
            return false;
        }
        String loginUrl = getServletContext().getInitParameter("auth-login-url");
        return loginUrl == null || !loginUrl.equals(url);
    }

    private Object currentUser(HttpServletRequest req, String authKey) {
        HttpSession httpSession = req.getSession(false);
        return httpSession == null ? null : httpSession.getAttribute(authKey);
    }

    private void denyAccess(Method annotatedMethod, HttpServletRequest req, HttpServletResponse res)
            throws IOException {
        String loginUrl = getServletContext().getInitParameter("auth-login-url");

        if (annotatedMethod.isAnnotationPresent(APIMethod.class)) {
            res.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            res.setContentType("application/json;charset=UTF-8");
            res.getWriter().print(gson.toJson(java.util.Map.of("error", "Veuillez vous connecter")));
        } else if (loginUrl != null && !loginUrl.isBlank()) {
            res.sendRedirect(req.getContextPath() + loginUrl);
        } else {
            res.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Authentification requise");
        }
    }

    private void handleApiResponse(Object result, HttpServletResponse res, PrintWriter printer) throws IOException {
        res.setContentType("application/json");

        String json;
        if (result instanceof String) {
            json = (String) result;
        } else {
            json = gson.toJson(result);
        }

        printer.print(json);
        printer.flush();
    }

    private void handleViewReponse(ModelAndView mv, HttpServletRequest req, HttpServletResponse res)
            throws ServletException, IOException {
        if (mv.getViewName() != null && mv.getViewName().startsWith("redirect:")) {
            res.sendRedirect(req.getContextPath() + mv.getViewName().substring("redirect:".length()));
            return;
        }

        res.setContentType("text/plain");
        for (Entry<String, List<Object>> entry : mv.getData().entrySet()) {
            req.setAttribute(entry.getKey(), entry.getValue());
        }

        String viewPath = (String) getServletContext().getInitParameter("view-prefix") + mv.getViewName()
                + (String) getServletContext().getInitParameter("view-suffix");
        req.getRequestDispatcher(viewPath).forward(req, res);
    }

    private void handleSimpleResponse(Object result, HttpServletResponse res, PrintWriter printer) {
        res.setContentType("text/plain");

        if (result != null) {
            printer.println(result.toString());
        } else {
            printer.println("La méthode n'a pas de retour mais a bien été executée");
        }
    }

    private void listURLs(HttpServletResponse res, PrintWriter printer, HashMap<URLMethod, MethodMapping> mappings) {
        res.setStatus(HttpServletResponse.SC_NOT_FOUND);
        res.setContentType("text/plain");

        printer.println("Voici les urls qui sont gérés :");
        for (URLMethod urlMethod : mappings.keySet()) {
            MethodMapping map = mappings.get(urlMethod);
            if (map.getAnnotatedMethod().isAnnotationPresent(Auth.class)
                    || map.getTargetClass().isAnnotationPresent(Auth.class)) {
                continue;
            }

            String urlValue = urlMethod.getUrl();
            RequestMethod rm = urlMethod.getRequestMethod();

            printer.println(rm + ": \"" + urlValue + "\"" + " pour la méthode " + map.getAnnotatedMethod().getName()
                    + " de la classe " + map.getTargetClass().getSimpleName());
        }
    }
}
