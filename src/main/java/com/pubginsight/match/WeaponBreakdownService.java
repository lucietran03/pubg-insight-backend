package com.pubginsight.match;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pubginsight.client.pubg.PubgApiClient;
import com.pubginsight.client.telemetry.TelemetryClient;
import com.pubginsight.client.telemetry.TelemetryFetchException;
import com.pubginsight.client.telemetry.dto.PlayerKillEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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

    // Distance buckets mirror PUBG's own in-client "shot distance" breakdown ranges (see the
    // reference screenshots this feature was built from) - upper bound is exclusive except
    // the last, open-ended bucket.
    private static final double[] DISTANCE_BUCKET_UPPER_BOUNDS_METERS = {30, 120, 300};
    private static final String[] DISTANCE_BUCKET_LABELS = {"0-30m", "30-120m", "120-300m", "300m+"};

    public MatchCombatBreakdownDto getWeaponBreakdown(String matchId, String playerId) {
        String rawMatchJson = pubgApiClient.findMatchRawJson(matchId);
        if (rawMatchJson == null) {
            throw new MatchNotFoundException("Match '" + matchId + "' not found");
        }

        String telemetryUrl = extractTelemetryUrl(rawMatchJson, matchId);
        if (telemetryUrl == null) {
            log.warn("No telemetry asset URL found for match '{}' - returning empty weapon breakdown", matchId);
            return MatchCombatBreakdownDto.empty();
        }

        try {
            List<PlayerKillEvent> killEvents = telemetryClient.fetchKillEventsForPlayer(telemetryUrl, playerId);
            return new MatchCombatBreakdownDto(toWeaponBreakdown(killEvents), toDistanceBuckets(killEvents));
        } catch (TelemetryFetchException e) {
            log.warn("Telemetry fetch/parse failed for match '{}' - returning empty weapon breakdown",
                    matchId, e);
            return MatchCombatBreakdownDto.empty();
        }
    }

    private static List<WeaponKillDto> toWeaponBreakdown(List<PlayerKillEvent> killEvents) {
        Map<String, Integer> killsByWeaponName = new LinkedHashMap<>();
        for (PlayerKillEvent event : killEvents) {
            killsByWeaponName.merge(event.weaponName(), 1, Integer::sum);
        }

        List<WeaponKillDto> result = new ArrayList<>(killsByWeaponName.size());
        killsByWeaponName.forEach((weaponName, kills) -> result.add(new WeaponKillDto(weaponName, kills)));
        result.sort(Comparator.comparingInt(WeaponKillDto::kills).reversed());
        return result;
    }

    // Kills with no distance data (see PlayerKillEvent.distanceMeters) are silently excluded
    // from the histogram rather than guessed into a bucket - an honest "we don't know" beats a
    // fabricated data point.
    private static List<DistanceBucketDto> toDistanceBuckets(List<PlayerKillEvent> killEvents) {
        int[] counts = new int[DISTANCE_BUCKET_LABELS.length];
        for (PlayerKillEvent event : killEvents) {
            Double distance = event.distanceMeters();
            if (distance == null) {
                continue;
            }
            counts[bucketIndexFor(distance)]++;
        }

        List<DistanceBucketDto> buckets = new ArrayList<>(DISTANCE_BUCKET_LABELS.length);
        for (int i = 0; i < DISTANCE_BUCKET_LABELS.length; i++) {
            buckets.add(new DistanceBucketDto(DISTANCE_BUCKET_LABELS[i], counts[i]));
        }
        return buckets;
    }

    private static int bucketIndexFor(double distanceMeters) {
        for (int i = 0; i < DISTANCE_BUCKET_UPPER_BOUNDS_METERS.length; i++) {
            if (distanceMeters < DISTANCE_BUCKET_UPPER_BOUNDS_METERS[i]) {
                return i;
            }
        }
        return DISTANCE_BUCKET_LABELS.length - 1;
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
