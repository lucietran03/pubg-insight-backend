package com.pubginsight.client.telemetry;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pubginsight.client.telemetry.dto.PlayerBodyHitEvent;
import com.pubginsight.client.telemetry.dto.PlayerCombatEvents;
import com.pubginsight.client.telemetry.dto.PlayerKillEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.io.PushbackInputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.zip.GZIPInputStream;

// Fetches a match's telemetry file and extracts one player's kill events and the
// damage-dealt events where that player was the attacker.
//
// Deliberately NOT routed through client.pubg.PubgApiClient/PubgRateLimiter: telemetry
// files are served unauthenticated from a separate CDN host, not api.pubg.com, and are not
// subject to that rate limit.
//
// Streams the top-level JSON array token-by-token rather than loading it whole, since
// telemetry files can run into the tens of MB.
@Component
public class TelemetryClient {

    private static final Logger log = LoggerFactory.getLogger(TelemetryClient.class);
    // PUBG has renamed this event to "LogPlayerKillV2" (same schema) in some matches; accept both.
    private static final String LOG_PLAYER_KILL_EVENT_TYPE = "LogPlayerKill";
    private static final String LOG_PLAYER_KILL_V2_EVENT_TYPE = "LogPlayerKillV2";
    // Covers every hit landed in the match, not just killing blows.
    private static final String LOG_PLAYER_TAKE_DAMAGE_EVENT_TYPE = "LogPlayerTakeDamage";
    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(5);
    // More generous than PubgApiClient's 5s read timeout - telemetry files are much larger.
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(30);

    private final WeaponNameResolver weaponNameResolver;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public TelemetryClient(WeaponNameResolver weaponNameResolver) {
        this.weaponNameResolver = weaponNameResolver;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(CONNECT_TIMEOUT)
                .build();
    }

    public PlayerCombatEvents fetchCombatEventsForPlayer(String telemetryUrl, String playerAccountId) {
        HttpRequest request = HttpRequest.newBuilder(URI.create(telemetryUrl))
                .timeout(REQUEST_TIMEOUT)
                .header("Accept-Encoding", "gzip")
                .GET()
                .build();

        HttpResponse<InputStream> response;
        try {
            response = httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());
        } catch (IOException | InterruptedException e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            throw new TelemetryFetchException("Failed to fetch telemetry from '" + telemetryUrl + "'", e);
        }

        if (response.statusCode() != 200) {
            throw new TelemetryFetchException(
                    "Telemetry fetch returned HTTP " + response.statusCode() + " for '" + telemetryUrl + "'", null);
        }

        try (InputStream body = decodeIfGzipped(response)) {
            return extractCombatEvents(body, playerAccountId);
        } catch (IOException e) {
            throw new TelemetryFetchException("Failed to parse telemetry from '" + telemetryUrl + "'", e);
        }
    }

    // HttpClient does not auto-decompress gzip responses, unlike a browser - Content-Encoding
    // alone isn't always reliable either, so this also sniffs the gzip magic bytes as a fallback.
    private static InputStream decodeIfGzipped(HttpResponse<InputStream> response) throws IOException {
        InputStream raw = response.body();
        boolean declaredGzip = response.headers().firstValue("Content-Encoding")
                .map(value -> value.equalsIgnoreCase("gzip"))
                .orElse(false);

        PushbackInputStream pushback = new PushbackInputStream(raw, 2);
        byte[] magic = new byte[2];
        int bytesRead = pushback.read(magic);
        if (bytesRead > 0) {
            pushback.unread(magic, 0, bytesRead);
        }
        boolean looksGzipped = bytesRead == 2 && (magic[0] & 0xFF) == 0x1F && (magic[1] & 0xFF) == 0x8B;

        return (declaredGzip || looksGzipped) ? new GZIPInputStream(pushback) : pushback;
    }

    // Single streamed pass produces both event lists - two separate passes would double the
    // parse cost on multi-MB telemetry files.
    private PlayerCombatEvents extractCombatEvents(InputStream telemetryJson, String playerAccountId) throws IOException {
        List<PlayerKillEvent> kills = new ArrayList<>();
        List<PlayerBodyHitEvent> bodyHits = new ArrayList<>();
        JsonFactory jsonFactory = objectMapper.getFactory();

        // Bookkeeping for the diagnostic warnings below, in case of an accountId mismatch or
        // a future PUBG event rename.
        int totalKillEvents = 0;
        List<String> sampleKillerAccountIds = new ArrayList<>();
        Set<String> distinctEventTypesSeen = new LinkedHashSet<>();

        try (JsonParser parser = jsonFactory.createParser(telemetryJson)) {
            if (parser.nextToken() != JsonToken.START_ARRAY) {
                throw new IOException("Telemetry payload is not a JSON array");
            }

            while (parser.nextToken() == JsonToken.START_OBJECT) {
                JsonNode event = objectMapper.readTree(parser);
                String eventType = event.path("_T").asText(null);
                if (distinctEventTypesSeen.size() < 40) {
                    distinctEventTypesSeen.add(eventType);
                }

                if (LOG_PLAYER_KILL_EVENT_TYPE.equals(eventType) || LOG_PLAYER_KILL_V2_EVENT_TYPE.equals(eventType)) {
                    totalKillEvents++;
                    String eventKillerAccountId = event.path("killer").path("accountId").asText(null);
                    if (sampleKillerAccountIds.size() < 5 && eventKillerAccountId != null) {
                        sampleKillerAccountIds.add(eventKillerAccountId);
                    }

                    if (playerAccountId.equals(eventKillerAccountId)) {
                        String weaponId = extractDamageCauserId(event);
                        if (weaponId != null && !weaponId.isBlank()) {
                            kills.add(new PlayerKillEvent(weaponId, weaponNameResolver.resolve(weaponId), extractDistanceMeters(event)));
                        } else {
                            // Logs the event's actual field names to help diagnose a future schema change.
                            log.warn("{} matched player '{}' but no weapon id field resolved - "
                                            + "top-level fields: {}, killerDamageInfo fields: {}",
                                    eventType, playerAccountId, fieldNamesOf(event), fieldNamesOf(event.path("killerDamageInfo")));
                        }
                    }
                } else if (LOG_PLAYER_TAKE_DAMAGE_EVENT_TYPE.equals(eventType) && isDamageDealtByPlayer(event, playerAccountId)) {
                    String damageReason = event.path("damageReason").asText(null);
                    if (damageReason != null && !damageReason.isBlank()) {
                        bodyHits.add(new PlayerBodyHitEvent(damageReason));
                    }
                }
            }
        }

        if (kills.isEmpty() && totalKillEvents > 0) {
            // Helps diagnose an accountId format mismatch between this and other PUBG endpoints.
            log.warn("Match has {} LogPlayerKill(V2) event(s) but none matched playerAccountId='{}' - "
                    + "sample killer.accountId values actually seen: {}",
                    totalKillEvents, playerAccountId, sampleKillerAccountIds);
        } else if (totalKillEvents == 0) {
            // Helps diagnose a future PUBG kill-event rename.
            log.warn("No LogPlayerKill/LogPlayerKillV2 events found in this match's telemetry at all - "
                    + "distinct event types actually present: {}", distinctEventTypesSeen);
        }

        return new PlayerCombatEvents(kills, bodyHits);
    }

    private static List<String> fieldNamesOf(JsonNode node) {
        List<String> names = new ArrayList<>();
        node.fieldNames().forEachRemaining(names::add);
        return names;
    }

    // Damage events with no attacker (bluezone, falls, etc.) safely resolve to "not equal"
    // here rather than throwing, since a missing field's .asText(null) is null.
    private static boolean isDamageDealtByPlayer(JsonNode event, String attackerAccountId) {
        String eventAttackerAccountId = event.path("attacker").path("accountId").asText(null);
        return attackerAccountId.equals(eventAttackerAccountId);
    }

    // Some event shapes nest damageCauserName under killerDamageInfo instead of the top level.
    private static String extractDamageCauserId(JsonNode event) {
        String flat = event.path("damageCauserName").asText(null);
        if (flat != null && !flat.isBlank()) {
            return flat;
        }
        return event.path("killerDamageInfo").path("damageCauserName").asText(null);
    }

    // PUBG telemetry reports distance in centimeters, not meters. Returns null (not 0.0) when
    // the field is absent, so a missing distance isn't misread as a point-blank kill.
    private static Double extractDistanceMeters(JsonNode event) {
        JsonNode distanceNode = event.path("distance");
        if (!distanceNode.isNumber()) {
            return null;
        }
        return distanceNode.asDouble() / 100.0;
    }
}
