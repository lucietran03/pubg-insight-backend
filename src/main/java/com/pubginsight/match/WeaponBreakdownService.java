package com.pubginsight.match;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pubginsight.client.pubg.PubgApiClient;
import com.pubginsight.client.telemetry.TelemetryClient;
import com.pubginsight.client.telemetry.TelemetryFetchException;
import com.pubginsight.client.telemetry.dto.PlayerBodyHitEvent;
import com.pubginsight.client.telemetry.dto.PlayerCombatEvents;
import com.pubginsight.client.telemetry.dto.PlayerKillEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

// Telemetry failures (missing asset URL, unreachable CDN, bad payload) are caught and
// turned into an empty breakdown rather than an error - this panel must never break the
// match page. PUBG API errors still propagate normally.
@Service
public class WeaponBreakdownService {

    private static final Logger log = LoggerFactory.getLogger(WeaponBreakdownService.class);
    private static final String ASSET_RESOURCE_TYPE = "asset";

    private final PubgApiClient pubgApiClient;
    private final TelemetryClient telemetryClient;
    // Jackson 3 JsonMapper is auto-configured, not this classic ObjectMapper type.
    private final ObjectMapper objectMapper = new ObjectMapper();

    public WeaponBreakdownService(PubgApiClient pubgApiClient, TelemetryClient telemetryClient) {
        this.pubgApiClient = pubgApiClient;
        this.telemetryClient = telemetryClient;
    }

    // Mirrors PUBG's own in-client shot-distance breakdown; upper bound exclusive except
    // the last bucket.
    private static final double[] DISTANCE_BUCKET_UPPER_BOUNDS_METERS = {30, 120, 300};
    private static final String[] DISTANCE_BUCKET_LABELS = {"0-30m", "30-120m", "120-300m", "300m+"};

    // "None"/"NonSpecific" (bluezone, falls, vehicles) are excluded - there's no body
    // part to honestly attribute them to.
    private static final Map<String, String> BODY_PART_LABELS_BY_DAMAGE_REASON = Map.of(
            "HeadShot", "Head",
            "TorsoShot", "Torso",
            "PelvisShot", "Pelvis",
            "ArmShot", "Arms",
            "LegShot", "Legs"
    );
    private static final List<String> BODY_PART_DAMAGE_REASON_ORDER =
            List.of("HeadShot", "TorsoShot", "PelvisShot", "ArmShot", "LegShot");

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
            PlayerCombatEvents combatEvents = telemetryClient.fetchCombatEventsForPlayer(telemetryUrl, playerId);
            List<PlayerKillEvent> killEvents = combatEvents.kills();
            return new MatchCombatBreakdownDto(
                    toWeaponBreakdown(killEvents),
                    toDistanceBuckets(killEvents),
                    toBodyPartBreakdown(combatEvents.bodyHits()));
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

    // Kills with no distance data are excluded rather than guessed into a bucket.
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

    // Unrecognized damageReason values are excluded rather than guessed into a bucket.
    private static List<BodyPartDamageDto> toBodyPartBreakdown(List<PlayerBodyHitEvent> bodyHitEvents) {
        Map<String, Integer> hitsByReason = new LinkedHashMap<>();
        for (PlayerBodyHitEvent event : bodyHitEvents) {
            if (!BODY_PART_LABELS_BY_DAMAGE_REASON.containsKey(event.damageReason())) {
                continue;
            }
            hitsByReason.merge(event.damageReason(), 1, Integer::sum);
        }

        List<BodyPartDamageDto> result = new ArrayList<>(BODY_PART_DAMAGE_REASON_ORDER.size());
        for (String reason : BODY_PART_DAMAGE_REASON_ORDER) {
            result.add(new BodyPartDamageDto(BODY_PART_LABELS_BY_DAMAGE_REASON.get(reason), hitsByReason.getOrDefault(reason, 0)));
        }
        return result;
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
