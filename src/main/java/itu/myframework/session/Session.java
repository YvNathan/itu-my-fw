package itu.myframework.session;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

public class Session {
    private Map<String, Object> attributes;

    public Session() {
        this.attributes = new ConcurrentHashMap<>();
    }

    public Session(HttpServletRequest req) {
        this();
        reload(req);
    }

    public void reload(HttpServletRequest req) {
        HttpSession httpSession = req.getSession();
        Map<String, Object> copy = new ConcurrentHashMap<>();
        for (String name : Collections.list(httpSession.getAttributeNames())) {
            copy.put(name, httpSession.getAttribute(name));
        }
        attributes = copy;
    }

    public Object getAttribute(String name) {
        return attributes.get(name);
    }

    public void setAttribute(String name, Object value) {
        if (value == null) {
            attributes.remove(name);
        } else {
            attributes.put(name, value);
        }
    }

    public void clear(){
        attributes.clear();
    }

    public void removeAttribute(String name) {
        attributes.remove(name);
    }

    public void syncHttpSession(HttpServletRequest req) {
        HttpSession httpSession = req.getSession();

        for (Map.Entry<String, Object> entry : attributes.entrySet()) {
            httpSession.setAttribute(entry.getKey(), entry.getValue());
        }

        for (String name : Collections.list(httpSession.getAttributeNames())) {
            if (!attributes.containsKey(name)) {
                httpSession.removeAttribute(name);
            }
        }
    }
}
