package com.pubginsight.match;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pubginsight.client.pubg.PubgApiClient;
import com.pubginsight.client.telemetry.TelemetryClient;
import com.pubginsight.client.telemetry.TelemetryFetchException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

// New, strictly-additive feature: a telemetry-derived "which weapon got each of this
// player's kills in this match" breakdown, on top of the existing summary-stats-only match
// page. Deliberately its own service/controller instead of extending MatchService/MatchDto -
// see WeaponBreakdownController for the endpoint and match.WeaponKillDto for the response
// shape.
//
// Failure handling is intentionally asymmetric:
//   - PubgApiException/PubgRateLimitException from PubgApiClient.findMatchRawJson() are left
//     to propagate to common.exception.GlobalExceptionHandler, same as every other PUBG-backed
//     endpoint - those are already-understood, already-tested failure modes for "PUBG API is
//     down/rate-limited", and the frontend already knows how to show them.
//   - Anything specific to telemetry itself (no asset URL on the match, the telemetry CDN
//     being slow/unreachable, an unparseable payload) is caught here and turned into an empty
//     list instead of an error. This is a brand new, optional panel bolted onto an
//     already-working match page - it must never turn "the match page mostly works" into "the
//     match page shows an error", per this task's isolation requirement.
@Service
public class WeaponBreakdownService {

    private static final Logger log = LoggerFactory.getLogger(WeaponBreakdownService.class);
    private static final String ASSET_RESOURCE_TYPE = "asset";

    private final PubgApiClient pubgApiClient;
    private final TelemetryClient telemetryClient;
    // Same rationale as MatchService's own field: this Spring Boot version's auto-configured
    // Jackson bean is a Jackson 3 JsonMapper, not this classic ObjectMapper type.
    private final ObjectMapper objectMapper = new ObjectMapper();

    public WeaponBreakdownService(PubgApiClient pubgApiClient, TelemetryClient telemetryClient) {
        this.pubgApiClient = pubgApiClient;
        this.telemetryClient = telemetryClient;
    }

    public List<WeaponKillDto> getWeaponBreakdown(String matchId, String playerId) {
        String rawMatchJson = pubgApiClient.findMatchRawJson(matchId);
        if (rawMatchJson == null) {
            throw new MatchNotFoundException("Match '" + matchId + "' not found");
        }

        String telemetryUrl = extractTelemetryUrl(rawMatchJson, matchId);
        if (telemetryUrl == null) {
            log.warn("No telemetry asset URL found for match '{}' - returning empty weapon breakdown", matchId);
            return List.of();
        }

        try {
            return telemetryClient.fetchWeaponKillsForPlayer(telemetryUrl, playerId).stream()
                    .map(count -> new WeaponKillDto(count.weaponName(), count.kills()))
                    .toList();
        } catch (TelemetryFetchException e) {
            log.warn("Telemetry fetch/parse failed for match '{}' - returning empty weapon breakdown",
                    matchId, e);
            return List.of();
        }
    }

    private String extractTelemetryUrl(String rawMatchJson, String matchId) {
        JsonNode root;
        try {
            root = objectMapper.readTree(rawMatchJson);
        } catch (JsonProcessingException e) {
            log.warn("Could not parse raw match JSON for '{}' while looking for the telemetry URL", matchId, e);
            return null;
        }

        for (JsonNode item : root.path("included")) {
            if (!ASSET_RESOURCE_TYPE.equals(item.path("type").asText(null))) {
                continue;
            }
            String url = item.path("attributes").path("URL").asText(null);
            if (url != null && !url.isBlank()) {
                return url;
            }
        }
        return null;
    }
}
