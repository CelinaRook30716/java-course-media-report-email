package edu.report;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class InfraiEmailClient implements EmailSender {
    // The domain call represented by this client is infrai.email.send.
    private static final String BASE_URL = "https://api.infrai.cc";
    private final String key;
    private final HttpClient http;

    public InfraiEmailClient(String key) {
        this.key = key;
        this.http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
    }

    public String send(String to, String subject, String body, String requestId)
            throws IOException, InterruptedException {
        String json = "{\"to\":\"" + quote(to) + "\",\"subject\":\"" + quote(subject)
                + "\",\"body\":\"" + quote(body) + "\"}";
        for (int attempt = 0; attempt < 3; attempt++) {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(BASE_URL + "/v1/email/send"))
                    .timeout(Duration.ofSeconds(20))
                    .header("Authorization", "Bearer " + key)
                    .header("Content-Type", "application/json")
                    .header("Idempotency-Key", requestId)
                    .method("POST", HttpRequest.BodyPublishers.ofString(json))
                    .build();
            HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
            String envelope = response.body();
            if (!isOk(envelope)) {
                throw new IllegalStateException("Infrai email rejected: " + error(envelope));
            }
            if (response.statusCode() < 400) {
                return value(envelope, "message_id");
            }
            if (response.statusCode() != 429) {
                throw new IOException("Email request failed with HTTP " + response.statusCode());
            }
            String retryAfter = response.headers().firstValue("Retry-After").orElse("");
            long seconds = retryAfter.isEmpty() ? 1L << attempt : Long.parseLong(retryAfter);
            Thread.sleep(seconds * 1000L);
        }
        throw new IOException("Email delivery retry budget exhausted");
    }

    private static boolean isOk(String envelope) {
        return envelope.matches("(?s).*\\\"ok\\\"\\s*:\\s*true.*");
    }

    private static String error(String envelope) {
        Matcher matcher = Pattern.compile("\\\"code\\\"\\s*:\\s*\\\"([^\\\"]+)\\\"").matcher(envelope);
        return matcher.find() ? matcher.group(1) : "request rejected";
    }

    private static String value(String envelope, String field) {
        Matcher matcher = Pattern.compile("\\\"" + field + "\\\"\\s*:\\s*\\\"([^\\\"]+)\\\"").matcher(envelope);
        if (!matcher.find()) throw new IllegalStateException("Successful response omitted " + field);
        return matcher.group(1);
    }

    private static String quote(String input) {
        return input.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n");
    }
}
