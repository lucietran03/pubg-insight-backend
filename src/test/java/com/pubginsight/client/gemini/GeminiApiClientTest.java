package com.pubginsight.client.gemini;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

// Exercises the model-failover loop in GeminiApiClient by overriding its package-private
// callModel() seam, so no real RestClient call, API key, or Gemini quota is needed.
class GeminiApiClientTest {

    private static GeminiApiProperties propertiesWithModels(String... models) {
        return new GeminiApiProperties("https://example.invalid", "test-key", List.of(models));
    }

    @Test
    void returnsFirstModelResultWithoutTryingSecondModel() {
        List<String> attemptedModels = new ArrayList<>();
        GeminiApiClient client = new GeminiApiClient(propertiesWithModels("model-a", "model-b")) {
            @Override
            String callModel(String model, String prompt) {
                attemptedModels.add(model);
                return "response from " + model;
            }
        };

        String result = client.generateText("prompt");

        assertThat(result).isEqualTo("response from model-a");
        assertThat(attemptedModels).containsExactly("model-a");
    }

    @Test
    void fallsBackToNextModelWhenFirstIsRateLimited() {
        List<String> attemptedModels = new ArrayList<>();
        GeminiApiClient client = new GeminiApiClient(propertiesWithModels("model-a", "model-b")) {
            @Override
            String callModel(String model, String prompt) {
                attemptedModels.add(model);
                if (model.equals("model-a")) {
                    throw new GeminiRateLimitException(30L);
                }
                return "response from " + model;
            }
        };

        String result = client.generateText("prompt");

        assertThat(result).isEqualTo("response from model-b");
        assertThat(attemptedModels).containsExactly("model-a", "model-b");
    }

    @Test
    void propagatesLastFailureWhenAllModelsFail() {
        GeminiRateLimitException modelAFailure = new GeminiRateLimitException(10L);
        GeminiApiException modelBFailure = new GeminiApiException("boom", null);

        GeminiApiClient client = new GeminiApiClient(propertiesWithModels("model-a", "model-b")) {
            @Override
            String callModel(String model, String prompt) {
                if (model.equals("model-a")) {
                    throw modelAFailure;
                }
                throw modelBFailure;
            }
        };

        // The exception from the LAST attempted model must propagate as-is (e.g. so a
        // GeminiRateLimitException's retryAfterSeconds still reaches GlobalExceptionHandler
        // unchanged), not a synthesized generic failure.
        assertThatThrownBy(() -> client.generateText("prompt"))
                .isSameAs(modelBFailure);
    }
}
