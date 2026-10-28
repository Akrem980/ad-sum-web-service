import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

/** Calls the sum web service on the domain controller with one GET and one POST request. */
public class SumClient {
    public static void main(String[] args) throws Exception {
        HttpClient client = HttpClient.newHttpClient();
        String base = "http://dc01.akrem.local:5000/sum";

        // GET request
        HttpRequest get = HttpRequest.newBuilder()
                .uri(URI.create(base + "?a=5&b=7"))
                .GET()
                .build();
        HttpResponse<String> r1 = client.send(get, HttpResponse.BodyHandlers.ofString());
        System.out.println("GET  -> " + r1.statusCode() + " " + r1.body());

        // POST request with a JSON body
        HttpRequest post = HttpRequest.newBuilder()
                .uri(URI.create(base))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString("{\"a\": 10, \"b\": 20}"))
                .build();
        HttpResponse<String> r2 = client.send(post, HttpResponse.BodyHandlers.ofString());
        System.out.println("POST -> " + r2.statusCode() + " " + r2.body());
    }
}
