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
            // Fail fast here rather than looping zero times and throwing a null lastFailure below.
            throw new IllegalStateException("gemini.api.models must contain at least one model");
        }
    }

    // Tries each configured model in order, falling back on quota/rate-limit failures since
    // those are per-model, not per-application. Propagates the last real failure (not a
    // synthesized one) if every model is down, so its concrete type is preserved.
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

    // Package-private so tests can override this seam to exercise failover without a real call.
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
            // Caught as the broad RestClientException, not just its subtypes: a timeout while
            // Spring is still reading the response body surfaces as a plain RestClientException.
            throw new GeminiApiException("Gemini API request failed for model '" + model + "'", e);
        }
    }

    // Gemini's Retry-After header isn't reliably set; the useful quota/retry info is in the
    // response body instead, so log it for diagnosis.
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
