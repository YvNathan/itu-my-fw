package itu.myframework.routing;

import java.util.HashMap;
import java.util.Map;

public class ResponseEntity<T> {
    private int status;
    private T body;
    private Map<String, String> headers = new HashMap<>();

    public ResponseEntity(T body, int status) {
        this.body = body;
        this.status = status;
    }

    public ResponseEntity(int status) {
        this(null, status);
    }

    public static <T> ResponseEntity<T> ok(T body) {
        return new ResponseEntity<>(body, 200);
    }

    public static <T> ResponseEntity<T> created(T body) {
        return new ResponseEntity<>(body, 201);
    }

    public static ResponseEntity<Void> noContent() {
        return new ResponseEntity<>(204);
    }

    public static <T> ResponseEntity<T> status(int status, T body) {
        return new ResponseEntity<>(body, status);
    }

    public static ResponseEntity<Void> status(int status) {
        return new ResponseEntity<>(status);
    }

    public ResponseEntity<T> header(String name, String value) {
        if (name != null && !name.isBlank() && value != null) {
            this.headers.put(name, value);
        }
        return this;
    }

    public int getStatus() { return status; }
    public T getBody() { return body; }
    public Map<String, String> getHeaders() { return headers; }
}