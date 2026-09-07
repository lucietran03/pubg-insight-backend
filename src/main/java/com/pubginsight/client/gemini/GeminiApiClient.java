package com.pubginsight.client.gemini;

import com.pubginsight.client.gemini.dto.GeminiContent;
import com.pubginsight.client.gemini.dto.GeminiGenerateContentRequest;
import com.pubginsight.client.gemini.dto.GeminiGenerateContentResponse;
import com.pubginsight.client.gemini.dto.GeminiPart;
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

    private static final int CONNECT_TIMEOUT_MILLIS = 3000;
    // Generation takes longer than a simple PUBG lookup, so this gets a longer read timeout.
    private static final int READ_TIMEOUT_MILLIS = 15000;

    private final RestClient restClient;
    private final String model;
    private final String apiKey;

    public GeminiApiClient(GeminiApiProperties properties) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(CONNECT_TIMEOUT_MILLIS);
        requestFactory.setReadTimeout(READ_TIMEOUT_MILLIS);

        this.restClient = RestClient.builder()
                .baseUrl(properties.baseUrl())
                .requestFactory(requestFactory)
                .build();
        this.model = properties.model();
        this.apiKey = properties.key();
    }

    public String generateText(String prompt) {
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
            throw new GeminiApiException("Gemini API request failed", e);
        }
    }

    // Gemini's Retry-After (when present) is the delay-seconds form, not an HTTP-date -
    // parse defensively and simply omit it if it's not a plain integer.
    private GeminiRateLimitException toRateLimitException(HttpClientErrorException.TooManyRequests e) {
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
