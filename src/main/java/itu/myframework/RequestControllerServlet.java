package itu.myframework;

import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;

import com.google.gson.Gson;

import itu.myframework.annotation.APIMethod;
import itu.myframework.annotation.RequestParam;
import itu.myframework.routing.MethodMapping;
import itu.myframework.routing.ModelAndView;
import itu.myframework.routing.RequestMethod;
import itu.myframework.routing.URLMethod;
import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class RequestControllerServlet extends HttpServlet {
    private HashMap<URLMethod, MethodMapping> mappings;
    private String viewPrefix;
    private String viewSuffix;
    private Object springContext;
    private final Gson gson = new Gson();

    @SuppressWarnings("unchecked")
    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);

        mappings = (HashMap<URLMethod, MethodMapping>) getServletContext().getAttribute("mappings");
        viewPrefix = (String) getServletContext().getInitParameter("view-prefix");
        viewSuffix = (String) getServletContext().getInitParameter("view-suffix");
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

        MethodMapping urlmap = mappings.get(new URLMethod(url, requestMethod));

        if (urlmap != null) {
            Class<?> targetClass = urlmap.getTargetClass();

            Method annotatedMethod = urlmap.getAnnotatedMethod();

            try {
                Object instance = targetClass.getDeclaredConstructor().newInstance();

                Object[] arguments = resolveArguments(annotatedMethod, req, res, springContext);

                Object result = annotatedMethod.invoke(instance, arguments);

                if (annotatedMethod.isAnnotationPresent(APIMethod.class)) {
                    handleApiResponse(result, res, printer);
                } else if (result instanceof ModelAndView) {
                    handleViewReponse((ModelAndView) result, req, res);
                } else {
                    handleSimpleResponse(result, res, printer);
                }
            } catch (Exception e) {
                throw new ServletException("Impossible d'executer la méthode" + e.getMessage());
            }
        } else {
            listURLs(res, printer);
        }
    }

    private Object[] resolveArguments(Method annotatedMethod, HttpServletRequest req, HttpServletResponse res,
            Object springContext) throws ServletException {
        Parameter[] params = annotatedMethod.getParameters();
        Object[] args = new Object[params.length];

        for (int i = 0; i < params.length; i++) {
            RequestParam rp = params[i].getAnnotation(RequestParam.class);

            if (rp != null) {
                args[i] = resolveRequestParam(rp, params[i].getType(), req);
            } else {
                args[i] = springContext;
            }
        }

        return args;
    }

    private Object resolveRequestParam(RequestParam rp, Class<?> type, HttpServletRequest req) throws ServletException {
        String rawParam = req.getParameter(rp.value());

        if (rawParam == null) {
            if (rp.required()) {
                throw new ServletException("Paramètre requis : " + rp.value());
            } else {
                return null;
            }
        }

        try {
            return resolveParamType(rawParam, type);
        } catch (Exception e) {
            throw new ServletException("Valeur invalide : " + rp.value());
        }
    }

    private Object resolveParamType(String param, Class<?> type) {
        if (type == String.class)
            return param;
        if (type == int.class || type == Integer.class) {
            return Integer.parseInt(param);
        }
        if (type == long.class || type == Long.class) {
            return Long.parseLong(param);
        }
        if (type == double.class || type == Double.class) {
            return Double.parseDouble(param);
        }
        if (type == boolean.class || type == Boolean.class) {
            return Boolean.parseBoolean(param);
        }
        if (type == LocalDate.class) {
            return LocalDate.parse(param);
        }

        throw new IllegalArgumentException("Type non supporté");
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
        res.setContentType("text/plain");
        for (Entry<String, List<Object>> entry : mv.getData().entrySet()) {
            req.setAttribute(entry.getKey(), entry.getValue());
        }

        String viewPath = viewPrefix + mv.getViewName() + viewSuffix;
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

    private void listURLs(HttpServletResponse res, PrintWriter printer) {
        res.setContentType("text/plain");

        printer.println("Voici les urls qui sont gérés :");
        for (URLMethod urlMethod : mappings.keySet()) {
            MethodMapping map = mappings.get(urlMethod);

            String urlValue = urlMethod.getUrl();
            RequestMethod rm = urlMethod.getRequestMethod();

            printer.println(rm + ": \"" + urlValue + "\"" + " pour la méthode " + map.getAnnotatedMethod().getName()
                    + " de la classe " + map.getTargetClass().getSimpleName());
        }
    }
}
