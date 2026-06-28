package itu.myframework.routing;

import java.util.Objects;

public class URLMethod {
    private String url;
    private RequestMethod requestMethod;

    public URLMethod(String url, RequestMethod requestMethod) {
        this.url = url;
        this.requestMethod = requestMethod;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public RequestMethod getRequestMethod() {
        return requestMethod;
    }

    public void setRequestMethod(RequestMethod requestMethod) {
        this.requestMethod = requestMethod;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (obj == null || this.getClass() != obj.getClass()) {
            return false;
        }

        URLMethod toCompare = (URLMethod) obj;

        return this.url.equals(toCompare.getUrl()) && this.requestMethod.equals(toCompare.getRequestMethod());
    }

    @Override
    public int hashCode(){
        return Objects.hash(url, requestMethod);
    }
}
