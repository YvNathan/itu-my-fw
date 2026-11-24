package itu.myframework.routing;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ModelAndView {
    private String viewName;
    private Map<String, Object> data = new HashMap<>();
    private Map<String, String> headers = new HashMap<>();

    public ModelAndView(String viewName, Map<String, Object> data) {
        this.viewName = viewName;
        this.data = data;
    }

    public ModelAndView(String viewName) {
        this();
        this.viewName = viewName;
    }

    public ModelAndView() {
    }

    public String getViewName() {
        return viewName;
    }

    public void setViewName(String viewName) {
        this.viewName = viewName;
    }

    public Map<String, Object> getData() {
        return data;
    }

    public void setData(Map<String, Object> data) {
        this.data = data;
    }

    public void addData(String name, Object value) {
        this.data.put(name, value);
    }

    @SuppressWarnings("unchecked")
    public void addDataToList(String name, Object value) {
        Object existing = this.data.computeIfAbsent(name, k -> new ArrayList<>());
        if (!(existing instanceof List)) {
            throw new IllegalStateException("La clé '" + name + "' n'est pas une liste");
        }
        ((List<Object>) existing).add(value);
    }

    public void addHeader(String name, String value){
        this.headers.put(name, value);
    }
    
    public Map<String, String> getHeaders(){
        return this.headers;
    }
}
