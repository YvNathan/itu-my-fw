package itu.myframework.routing;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ModelAndView {
    private String viewName;
    private Map<String, List<Object>> data = new HashMap<>();

    public ModelAndView(String viewName, Map<String, List<Object>> data) {
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

    public Map<String, List<Object>> getData() {
        return data;
    }

    public void setData(Map<String, List<Object>> data) {
        this.data = data;
    }

    public void addData(String name, List<Object> value) {
        this.data.put(name, value);
    }

    public void addDataToList(String name, Object value) {
        this.data.computeIfAbsent(name, k -> new java.util.ArrayList<>()).add(value);
    }
}
