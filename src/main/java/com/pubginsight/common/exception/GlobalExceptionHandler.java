package com.pubginsight.common.exception;

import com.pubginsight.client.dynamodb.AnalysisHistoryException;
import com.pubginsight.client.gemini.GeminiApiException;
import com.pubginsight.client.pubg.PubgApiException;
import com.pubginsight.client.pubg.PubgRateLimitException;
import com.pubginsight.match.MatchNotFoundException;
import com.pubginsight.player.PlayerNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler({PlayerNotFoundException.class, MatchNotFoundException.class})
    public ResponseEntity<Map<String, String>> handleNotFound(RuntimeException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("error", e.getMessage()));
    }

    @ExceptionHandler(PubgRateLimitException.class)
    public ResponseEntity<Map<String, String>> handleRateLimit(PubgRateLimitException e) {
        log.warn("PUBG API rate limit reached (retryAfterSeconds={})", e.getRetryAfterSeconds());
        ResponseEntity.BodyBuilder builder = ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS);
        if (e.getRetryAfterSeconds() != null) {
            builder.header(HttpHeaders.RETRY_AFTER, String.valueOf(e.getRetryAfterSeconds()));
        }
        return builder.body(Map.of("error", "PUBG API rate limit reached. Please try again shortly."));
    }

    @ExceptionHandler(PubgApiException.class)
    public ResponseEntity<Map<String, String>> handlePubgApiException(PubgApiException e) {
        log.error("PUBG API call failed: {}", e.getMessage(), e.getCause());
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                .body(Map.of("error", "Failed to reach PUBG API. Please try again later."));
    }

    @ExceptionHandler(GeminiApiException.class)
    public ResponseEntity<Map<String, String>> handleGeminiApiException(GeminiApiException e) {
        log.error("Gemini API call failed: {}", e.getMessage(), e.getCause());
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                .body(Map.of("error", "Failed to generate AI insights. Please try again later."));
    }

    // No handler for S3CacheException on purpose: MatchService catches and soft-fails it
    // internally (a broken cache must never break the feature), so it should never reach
    // here. Unlike the S3 cache, DynamoDB write/read failure in HistoryService IS a real
    // feature failure (there's nothing to fall back to), so AnalysisHistoryException is
    // handled explicitly below.
    @ExceptionHandler(AnalysisHistoryException.class)
    public ResponseEntity<Map<String, String>> handleAnalysisHistoryException(AnalysisHistoryException e) {
        log.error("DynamoDB analysis history call failed: {}", e.getMessage(), e.getCause());
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                .body(Map.of("error", "Failed to save or load analysis history. Please try again later."));
    }
}
