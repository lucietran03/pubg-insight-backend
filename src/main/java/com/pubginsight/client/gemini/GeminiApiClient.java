package com.pubginsight.client.gemini;

import com.pubginsight.client.gemini.dto.GeminiContent;
import com.pubginsight.client.gemini.dto.GeminiGenerateContentRequest;
import com.pubginsight.client.gemini.dto.GeminiGenerateContentResponse;
import com.pubginsight.client.gemini.dto.GeminiPart;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.List;

@Component
public class GeminiApiClient {

    private static final Logger log = LoggerFactory.getLogger(GeminiApiClient.class);

    private static final int CONNECT_TIMEOUT_MILLIS = 3000;
    // Generation takes longer than a simple PUBG lookup, so this gets a longer read timeout.
    private static final int READ_TIMEOUT_MILLIS = 15000;

    private final RestClient restClient;
    private final List<String> models;
    private final String apiKey;

    public GeminiApiClient(GeminiApiProperties properties) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(CONNECT_TIMEOUT_MILLIS);
        requestFactory.setReadTimeout(READ_TIMEOUT_MILLIS);

        this.restClient = RestClient.builder()
                .baseUrl(properties.baseUrl())
                .requestFactory(requestFactory)
                .build();
        this.models = properties.models();
        this.apiKey = properties.key();

        if (models.isEmpty()) {
            // Fail fast at startup rather than looping zero times and throwing a null
            // lastFailure the first time a request comes in.
            throw new IllegalStateException("gemini.api.models must contain at least one model");
        }
    }

    // Tries each configured model in priority order. Quota/rate-limit failures are
    // per-model, not per-application, so a busy or exhausted model is a reason to try the
    // next one rather than give up - the last real failure (not a synthesized one) is
    // what propagates if every model is down, since GlobalExceptionHandler needs the
    // actual exception type (e.g. GeminiRateLimitException's retryAfterSeconds) to render
    // the right status.
    public String generateText(String prompt) {
        RuntimeException lastFailure = null;

        for (String model : models) {
            try {
                return callModel(model, prompt);
            } catch (GeminiRateLimitException | GeminiApiException e) {
                log.warn("Gemini model '{}' failed ({}), trying next model", model, e.getClass().getSimpleName());
                lastFailure = e;
            }
        }

        throw lastFailure;
    }

    // Extracted as its own method (rather than inlined in the loop above) so a unit test
    // can override this single seam to exercise the failover logic without a real
    // RestClient call, API key, or quota.
    String callModel(String model, String prompt) {
        GeminiGenerateContentRequest request = new GeminiGenerateContentRequest(
                List.of(new GeminiContent(List.of(new GeminiPart(prompt)))));

        try {
            GeminiGenerateContentResponse response = restClient.post()
                    .uri("/v1beta/models/{model}:generateContent?key={key}", model, apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .body(GeminiGenerateContentResponse.class);

            return response.candidates().stream()
                    .findFirst()
                    .flatMap(candidate -> candidate.content().parts().stream().findFirst())
                    .map(GeminiPart::text)
                    .orElseThrow(() -> new GeminiApiException("Gemini returned no usable content", null));
        } catch (HttpClientErrorException.TooManyRequests e) {
            throw toRateLimitException(e);
        } catch (RestClientException e) {
            // Catching RestClientException itself, not just its ResourceAccessException/
            // HttpStatusCodeException subtypes: a read timeout that happens while Spring is
            // still reading response headers/body (readWithMessageConverters) surfaces as a
            // plain RestClientException, not ResourceAccessException - confirmed via a real
            // uncaught 500 in production logs before this was widened to the common superclass.
            throw new GeminiApiException("Gemini API request failed for model '" + model + "'", e);
        }
    }

    // Gemini puts the actually-useful diagnostic info (WHICH quota was exceeded - per
    // model, per minute vs per day - and its own suggested retry delay) inside the
    // response BODY (a google.rpc.QuotaFailure / RetryInfo structure), not the HTTP
    // Retry-After header Gemini doesn't reliably set. Logging the raw body here is the
    // only way to tell a transient per-minute limit apart from an exhausted per-day one
    // after the fact - `retryAfterSeconds=null` alone (the previous behavior) couldn't.
    private GeminiRateLimitException toRateLimitException(HttpClientErrorException.TooManyRequests e) {
        log.warn("Gemini rate limit response body: {}", e.getResponseBodyAsString());

        HttpHeaders headers = e.getResponseHeaders();
        String retryAfterHeader = headers != null ? headers.getFirst(HttpHeaders.RETRY_AFTER) : null;

        Long retryAfterSeconds = null;
        if (retryAfterHeader != null) {
            try {
                retryAfterSeconds = Long.parseLong(retryAfterHeader);
            } catch (NumberFormatException ignored) {
                // Not a delay-seconds value - leave retryAfterSeconds null rather than guessing.
            }
        }

        return new GeminiRateLimitException(retryAfterSeconds);
    }
}
