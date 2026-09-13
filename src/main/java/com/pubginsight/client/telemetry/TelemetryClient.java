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
import java.util.List;
import java.util.zip.GZIPInputStream;

// Fetches and parses a single match's telemetry file (a flat JSON array of typed gameplay
// events) into the raw lists of one player's LogPlayerKill events (weapon + distance) and
// LogPlayerTakeDamage events where that player was the attacker (body-part hit location).
// match.WeaponBreakdownService derives the weapon tally, shot-distance breakdown, and
// body-part breakdown all from this same pair of lists - one telemetry pass covers all three
// presentations.
//
// Deliberately NOT routed through client.pubg.PubgApiClient/PubgRateLimiter: telemetry files
// are static assets served from a separate CDN host (the "URL" in the match response's
// "asset" resource, e.g. telemetry-cdn.playbattlegrounds.com), not from api.pubg.com, and are
// fetched unauthenticated - no "Authorization: Bearer <key>" header is sent. This isn't a
// guess: the community pubgjava client (github.com/mautini/pubgjava) tags every other
// endpoint's Retrofit method with a custom "@Headers("@: Auth")" marker that its interceptor
// uses to attach the API key, but its getTelemetry(String url) method carries no such marker
// - the only endpoint in that whole interface without it. That is real, working, referenced
// client code treating telemetry as an unauthenticated fetch to an arbitrary URL, distinct
// from every rate-limited api.pubg.com call. This project's own sandbox could not confirm
// this by making a live call against a real match (api.pubg.com and the telemetry CDN are
// both blocked by this environment's outbound proxy, category "Online and Other Games" -
// confirmed via curl, not assumed), so treat this as strong secondary evidence, not a
// first-party empirical result - re-verify against a real match on a real dev machine before
// relying on it for anything beyond this course project's demo.
//
// Telemetry files can run into the tens of MB. To avoid loading the whole array into memory,
// this streams the top-level array token-by-token and only fully materializes (as a JsonNode)
// the individual objects worth inspecting - memory stays bounded by event size, not file size.
@Component
public class TelemetryClient {

    private static final Logger log = LoggerFactory.getLogger(TelemetryClient.class);
    private static final String LOG_PLAYER_KILL_EVENT_TYPE = "LogPlayerKill";
    // LogPlayerTakeDamage covers every hit landed across the match (not just the killing
    // blow), and carries a "damageReason" field with a specific hit-location value
    // ("HeadShot", "TorsoShot", "ArmShot", "LegShot", "PelvisShot") alongside the generic
    // "None"/"NonSpecific" reasons used for non-directional damage (bluezone, falls,
    // vehicles, etc). Confirmed against two independent sources: PUBG's own official
    // dictionary (github.com/pubg/api-assets, enums/telemetry/damageReason.json) and the
    // community pubgjava client's LogPlayerTakeDamage/DamageReason model classes, which agree
    // on both the field name and every enum value.
    private static final String LOG_PLAYER_TAKE_DAMAGE_EVENT_TYPE = "LogPlayerTakeDamage";
    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(5);
    // Generous compared to PubgApiClient's 5s read timeout on purpose - telemetry files are
    // orders of magnitude larger than any api.pubg.com JSON response.
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

    // PUBG's telemetry CDN serves files gzip-compressed - java.net.http.HttpClient does NOT
    // auto-decompress responses the way a browser or a library like OkHttp would, so the raw
    // gzip bytes were being fed straight into the JSON parser (real bug: every single fetch
    // failed with "Illegal character (CTRL-CHAR, code 31)" at line 1, column 2 - 0x1F is
    // literally the first byte of the gzip magic number 0x1F8B, which is exactly what a JSON
    // parser sees as "not whitespace, not a valid token start"). Checking Content-Encoding
    // alone isn't fully reliable in practice, so this also sniffs the first 2 bytes directly
    // and falls back to that if the header is missing/wrong.
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

    // Single streamed pass over the telemetry array producing both event lists at once -
    // deliberately not two separate streaming passes (kills, then body hits), since telemetry
    // files can run into the tens of MB and each full re-parse would double that cost.
    private PlayerCombatEvents extractCombatEvents(InputStream telemetryJson, String playerAccountId) throws IOException {
        List<PlayerKillEvent> kills = new ArrayList<>();
        List<PlayerBodyHitEvent> bodyHits = new ArrayList<>();
        JsonFactory jsonFactory = objectMapper.getFactory();

        // Diagnostic-only bookkeeping (see the post-loop check below): if this match really has
        // LogPlayerKill events but none matched playerAccountId, sampling a few real
        // killer.accountId values actually seen is what tells us whether this is an accountId
        // format mismatch, not a guess.
        int totalKillEvents = 0;
        List<String> sampleKillerAccountIds = new ArrayList<>();

        try (JsonParser parser = jsonFactory.createParser(telemetryJson)) {
            if (parser.nextToken() != JsonToken.START_ARRAY) {
                throw new IOException("Telemetry payload is not a JSON array");
            }

            while (parser.nextToken() == JsonToken.START_OBJECT) {
                // Reads just this one array element as a tree, then leaves the parser
                // positioned right after it - the rest of the (possibly huge) array is never
                // materialized at once.
                JsonNode event = objectMapper.readTree(parser);
                String eventType = event.path("_T").asText(null);

                if (LOG_PLAYER_KILL_EVENT_TYPE.equals(eventType)) {
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
                            // Diagnostic only: a real kill matched by accountId but neither known
                            // weapon-id field path resolved anything - most likely PUBG's
                            // telemetry schema has moved the field again. Logging the event's own
                            // top-level (and killerDamageInfo's, if present) field names, not
                            // assumed guesses, is what actually tells us where the data lives now.
                            log.warn("LogPlayerKill matched player '{}' but no weapon id field resolved - "
                                            + "top-level fields: {}, killerDamageInfo fields: {}",
                                    playerAccountId, fieldNamesOf(event), fieldNamesOf(event.path("killerDamageInfo")));
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
            // Diagnostic only: this match has real kill events, but not one had a killer
            // accountId equal to playerAccountId - comparing the queried id against real ids
            // actually present in this telemetry file is what tells us if this is an id-format
            // mismatch (e.g. one side has a "account." prefix the other doesn't).
            log.warn("Match has {} LogPlayerKill event(s) but none matched playerAccountId='{}' - "
                    + "sample killer.accountId values actually seen: {}",
                    totalKillEvents, playerAccountId, sampleKillerAccountIds);
        }

        return new PlayerCombatEvents(kills, bodyHits);
    }

    private static List<String> fieldNamesOf(JsonNode node) {
        List<String> names = new ArrayList<>();
        node.fieldNames().forEachRemaining(names::add);
        return names;
    }

    // "attacker" (not "victim") is who DEALT this damage - matches the pubgjava
    // LogPlayerTakeDamage model (attacker/victim, both a "Character" object with an
    // accountId), the same shape as LogPlayerKill's "killer"/"victim". Damage events with no
    // attacker at all (bluezone, falls, drowning, etc.) safely fall through to "not equal"
    // here rather than throwing, since JsonNode.path() on a missing field returns a
    // MissingNode whose .asText(null) is null.
    private static boolean isDamageDealtByPlayer(JsonNode event, String attackerAccountId) {
        String eventAttackerAccountId = event.path("attacker").path("accountId").asText(null);
        return attackerAccountId.equals(eventAttackerAccountId);
    }

    // "damageCauserName" is the field name in every telemetry sample this project could find
    // documented (including the pubgjava model below), but PUBG's telemetry schema is known to
    // drift over time - defensively also check a "killerDamageInfo.damageCauserName" nesting
    // some newer event types use elsewhere in the schema, in case a future match's
    // LogPlayerKill has moved the field there too.
    private static String extractDamageCauserId(JsonNode event) {
        String flat = event.path("damageCauserName").asText(null);
        if (flat != null && !flat.isBlank()) {
            return flat;
        }
        return event.path("killerDamageInfo").path("damageCauserName").asText(null);
    }

    // PUBG telemetry reports LogPlayerKill's "distance" in CENTIMETERS, not meters - a known,
    // easy-to-miss quirk of this schema. Returns null (not 0.0) when the field is genuinely
    // absent, so a kill with no distance data is never misrepresented as a 0m point-blank kill.
    private static Double extractDistanceMeters(JsonNode event) {
        JsonNode distanceNode = event.path("distance");
        if (!distanceNode.isNumber()) {
            return null;
        }
        return distanceNode.asDouble() / 100.0;
    }
}
