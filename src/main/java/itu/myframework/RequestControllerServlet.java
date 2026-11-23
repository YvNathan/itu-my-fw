package itu.myframework;

import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

import com.google.gson.Gson;

import itu.myframework.annotation.APIMethod;
import itu.myframework.annotation.Auth;
import itu.myframework.annotation.Controller;
import itu.myframework.annotation.PathVariable;
import itu.myframework.annotation.RequestModel;
import itu.myframework.annotation.RequestParam;
import itu.myframework.binding.Binder;
import itu.myframework.config.AppProperties;
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

    @Override
    public void doGet(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        processRequest(req, res);
    }

    @Override
    public void doPost(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        processRequest(req, res);
    }

    @Override
    public void doPut(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        processRequest(req, res);
    }

    @Override
    public void doDelete(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        processRequest(req, res);
    }

    @Override
    public void doOptions(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        processRequest(req, res);
    }

    @SuppressWarnings("unchecked")
    private void processRequest(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        String contextPath = req.getContextPath();

        String url = req.getRequestURI().substring(contextPath.length());

        RequestMethod requestMethod = RequestMethod.valueOf(req.getMethod());

        HashMap<URLMethod, MethodMapping> mappings = (HashMap<URLMethod, MethodMapping>) getServletContext()
                .getAttribute("mappings");
        Map<String, String> pathVariables = new HashMap<>();
        MethodMapping urlmap = mappings.get(new URLMethod(url, requestMethod));

        if (urlmap == null) {
            for (Entry<URLMethod, MethodMapping> entry : mappings.entrySet()) {
                URLMethod candidate = entry.getKey();
                if (candidate.getRequestMethod() != requestMethod || !candidate.getUrl().contains("{")) {
                    continue;
                }
                Map<String, String> extracted = matchPath(candidate.getUrl(), url);
                if (extracted != null) {
                    urlmap = entry.getValue();
                    pathVariables = extracted;
                    break;
                }
            }
        }

        if (urlmap != null) {
            Class<?> targetClass = urlmap.getTargetClass();

            Method annotatedMethod = urlmap.getAnnotatedMethod();

            String authKey = (String) getServletContext().getAttribute("authSessionKey");
            Object userBefore = currentUser(req, authKey);

            String roleKey = (String) getServletContext().getAttribute("roleSessionKey");

            Auth auth = findAuth(url, annotatedMethod, targetClass);
            if (auth != null) {
                if (userBefore == null) {
                    denyAccess(annotatedMethod, req, res);
                    return;
                }
                if (auth.value().length > 0 && !hasAnyRole(req, roleKey, auth.value())) {
                    forbidAccess(annotatedMethod, req, res);
                    return;
                }
            }

            try {
                Object instance;
                if (targetClass.getAnnotation(Controller.class).singleton()) {
                    instance = ((HashMap<Class<?>, Object>) getServletContext().getAttribute("controllers"))
                            .get(targetClass);
                } else {
                    instance = targetClass.getDeclaredConstructor().newInstance();
                }

                Object[] arguments = resolveArguments(annotatedMethod, req, res, springContext, pathVariables);

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
                    handleApiResponse(result, res);
                    return;
                } else if (result instanceof ModelAndView) {
                    handleViewReponse((ModelAndView) result, req, res);
                    return;
                } else {
                    handleSimpleResponse(result, res);
                    return;
                }
            } catch (Exception e) {
                Throwable cause = (e instanceof InvocationTargetException && e.getCause() != null) ? e.getCause() : e;
                throw new ServletException("Impossible d'exécuter la méthode : " + cause, cause);
            }
        } else {
            boolean isStatic = !url.startsWith("/WEB-INF/") && !url.endsWith("/") && getServletContext().getResource(url) != null;

            if (isStatic) {
                getServletContext().getNamedDispatcher("default").forward(req, res);
                return ;
            }

            if ("true".equalsIgnoreCase(AppProperties.get("app.debug"))) {
                listURLs(res, mappings);
            } else {
                res.sendError(HttpServletResponse.SC_NOT_FOUND);
            }

        }
    }

    private Map<String, String> matchPath(String template, String url) {
        String[] templateParts = template.split("/");
        String[] urlParts = url.split("/");

        if (templateParts.length != urlParts.length) {
            return null;
        }
        Map<String, String> vars = new HashMap<>();
        for (int i = 0; i < templateParts.length; i++) {
            String tp = templateParts[i];
            String ap = urlParts[i];

            if (tp.startsWith("{") && tp.endsWith("}")) {
                String varName = tp.substring(1, tp.length() - 1);
                vars.put(varName, ap);
            } else if (!tp.equals(ap)) {
                return null;
            }
        }
        return vars;
    }

    @SuppressWarnings("unchecked")
    private Object[] resolveArguments(Method annotatedMethod, HttpServletRequest req, HttpServletResponse res,
            Object springContext, Map<String, String> pathVariables) throws ServletException {
        Parameter[] params = annotatedMethod.getParameters();
        Object[] args = new Object[params.length];

        for (int i = 0; i < params.length; i++) {
            RequestParam rp = params[i].getAnnotation(RequestParam.class);
            RequestModel rm = params[i].getAnnotation(RequestModel.class);
            PathVariable pv = params[i].getAnnotation(PathVariable.class);
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
            } else if (pv != null) {
                String raw = pathVariables.get(pv.value());
                if (raw == null) {
                    throw new ServletException("PathVariable manquant : " + pv.value());
                }
                try {
                    args[i] = Binder.resolveParamType(raw, (Class<?>) params[i].getParameterizedType());
                } catch (Exception e) {
                    throw new ServletException("PathVariable invalide : " + pv.value(), e);
                }
            } else if (springContext != null && params[i].getType().isInstance(springContext)) {
                args[i] = springContext;
            } else {
                throw new ServletException("Paramètre non résolu : " + params[i].getName() + "("
                        + params[i].getType().getSimpleName() + ")");
            }
        }

        return args;
    }

    private Auth findAuth(String url, Method annotatedMethod, Class<?> targetClass) {
        Auth auth = annotatedMethod.getAnnotation(Auth.class);
        if (auth == null) {
            auth = targetClass.getAnnotation(Auth.class);
        }
        String loginUrl = getServletContext().getInitParameter("auth-login-url");
        if (loginUrl != null && loginUrl.equals(url)) {
            return null;
        }
        return auth;
    }

    private boolean hasAnyRole(HttpServletRequest req, String roleKey, String[] allowed) {
        HttpSession httpSession = req.getSession(false);
        if (httpSession == null) {
            return false;
        }
        Object value = httpSession.getAttribute(roleKey);

        List<String> roles = new ArrayList<>();
        if (value instanceof Collection) {
            for (Object r : (Collection<?>) value) {
                roles.add(String.valueOf(r));
            }
        } else if (value != null) {
            roles.add(value.toString());
        }

        for (String role : allowed) {
            if (roles.contains(role)) {
                return true;
            }
        }
        return false;
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

    private void forbidAccess(Method annotatedMethod, HttpServletRequest req, HttpServletResponse res)
            throws IOException {
        String forbiddenUrl = getServletContext().getInitParameter("auth-forbidden-url");

        if (annotatedMethod.isAnnotationPresent(APIMethod.class)) {
            res.setStatus(HttpServletResponse.SC_FORBIDDEN);
            res.setContentType("application/json;charset=UTF-8");
            res.getWriter().print(gson.toJson(java.util.Map.of("error", "Accès refusé")));
        } else if (forbiddenUrl != null && !forbiddenUrl.isBlank()) {
            res.sendRedirect(req.getContextPath() + forbiddenUrl);
        } else {
            res.sendError(HttpServletResponse.SC_FORBIDDEN, "Accès refusé");
        }
    }

    private void handleApiResponse(Object result, HttpServletResponse res) throws IOException {
        res.setContentType("application/json;charset=UTF-8");
        PrintWriter printer = res.getWriter();

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

        for (Entry<String, List<Object>> entry : mv.getData().entrySet()) {
            req.setAttribute(entry.getKey(), entry.getValue());
        }

        String viewPath = (String) getServletContext().getInitParameter("view-prefix") + mv.getViewName()
                + (String) getServletContext().getInitParameter("view-suffix");
        req.getRequestDispatcher(viewPath).forward(req, res);
    }

    private void handleSimpleResponse(Object result, HttpServletResponse res) throws IOException {
        res.setContentType("text/plain;charset=UTF-8");
        PrintWriter printer = res.getWriter();

        if (result != null) {
            printer.println(result.toString());
        } else {
            printer.println("La méthode n'a pas de retour mais a bien été executée");
        }
    }

    private void listURLs(HttpServletResponse res, HashMap<URLMethod, MethodMapping> mappings) throws IOException {
        res.setStatus(HttpServletResponse.SC_NOT_FOUND);
        res.setContentType("text/plain;charset=UTF-8");
        PrintWriter printer = res.getWriter();

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
