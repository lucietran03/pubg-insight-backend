package com.pubginsight.common.exception;

import com.pubginsight.client.pubg.PubgApiException;
import com.pubginsight.match.MatchNotFoundException;
import com.pubginsight.player.PlayerNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler({PlayerNotFoundException.class, MatchNotFoundException.class})
    public ResponseEntity<Map<String, String>> handleNotFound(RuntimeException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("error", e.getMessage()));
    }

    @ExceptionHandler(PubgApiException.class)
    public ResponseEntity<Map<String, String>> handlePubgApiException(PubgApiException e) {
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                .body(Map.of("error", "Failed to reach PUBG API. Please try again later."));
    }
}
