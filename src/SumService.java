import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Minimal REST web service: returns a + b as JSON. Supports GET and POST on /sum. */
public class SumService {

    public static void main(String[] args) throws IOException {
        // 0.0.0.0 = listen on all interfaces, so other machines (client1) can reach it
        HttpServer server = HttpServer.create(new InetSocketAddress("0.0.0.0", 5000), 0);
        server.createContext("/sum", SumService::handle);
        // each request is handled by a thread from this pool
        server.setExecutor(Executors.newFixedThreadPool(4));
        server.start();
        System.out.println("Sum service listening on port 5000");
    }

    static void handle(HttpExchange ex) throws IOException {
        try {
            double a, b;
            String method = ex.getRequestMethod();

            if (method.equals("GET")) {
                Map<String, String> q = parseQuery(ex.getRequestURI().getRawQuery());
                a = Double.parseDouble(q.get("a"));
                b = Double.parseDouble(q.get("b"));
            } else if (method.equals("POST")) {
                String body = new String(ex.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
                a = extract(body, "a");
                b = extract(body, "b");
            } else {
                send(ex, 405, "{\"error\":\"Use GET or POST\"}");
                return;
            }

            send(ex, 200, "{\"a\":" + a + ",\"b\":" + b + ",\"sum\":" + (a + b) + "}");
        } catch (Exception e) {
            send(ex, 400, "{\"error\":\"Provide numeric values a and b\"}");
        }
    }

    // "a=5&b=7" -> {a=5, b=7}
    static Map<String, String> parseQuery(String raw) {
        Map<String, String> map = new HashMap<>();
        for (String pair : raw.split("&")) {
            String[] kv = pair.split("=", 2);
            map.put(kv[0], kv[1]);
        }
        return map;
    }

    // reads a number from a JSON body like {"a": 5, "b": 7}
    static double extract(String json, String key) {
        Matcher m = Pattern.compile("\"" + key + "\"\\s*:\\s*(-?\\d+(?:\\.\\d+)?)").matcher(json);
        if (!m.find()) throw new IllegalArgumentException("missing " + key);
        return Double.parseDouble(m.group(1));
    }

    static void send(HttpExchange ex, int status, String json) throws IOException {
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().add("Content-Type", "application/json");
        ex.sendResponseHeaders(status, bytes.length);
        try (OutputStream os = ex.getResponseBody()) {
            os.write(bytes);
        }
    }
}
