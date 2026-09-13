package com.pubginsight.client.telemetry;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pubginsight.client.telemetry.dto.WeaponKillCount;
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
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPInputStream;

// Fetches and parses a single match's telemetry file (a flat JSON array of typed gameplay
// events) to build a per-weapon kill tally for one player.
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

    private static final String LOG_PLAYER_KILL_EVENT_TYPE = "LogPlayerKill";
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

    public List<WeaponKillCount> fetchWeaponKillsForPlayer(String telemetryUrl, String killerAccountId) {
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
            Map<String, Integer> killsByWeaponId = tallyKillsByWeapon(body, killerAccountId);
            return toSortedWeaponKillCounts(killsByWeaponId);
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

    private Map<String, Integer> tallyKillsByWeapon(InputStream telemetryJson, String killerAccountId) throws IOException {
        Map<String, Integer> killsByWeaponId = new LinkedHashMap<>();
        JsonFactory jsonFactory = objectMapper.getFactory();

        try (JsonParser parser = jsonFactory.createParser(telemetryJson)) {
            if (parser.nextToken() != JsonToken.START_ARRAY) {
                throw new IOException("Telemetry payload is not a JSON array");
            }

            while (parser.nextToken() == JsonToken.START_OBJECT) {
                // Reads just this one array element as a tree, then leaves the parser
                // positioned right after it - the rest of the (possibly huge) array is never
                // materialized at once.
                JsonNode event = objectMapper.readTree(parser);
                if (!isKillByPlayer(event, killerAccountId)) {
                    continue;
                }

                String weaponId = extractDamageCauserId(event);
                if (weaponId == null || weaponId.isBlank()) {
                    continue;
                }

                killsByWeaponId.merge(weaponId, 1, Integer::sum);
            }
        }

        return killsByWeaponId;
    }

    private static boolean isKillByPlayer(JsonNode event, String killerAccountId) {
        if (!LOG_PLAYER_KILL_EVENT_TYPE.equals(event.path("_T").asText(null))) {
            return false;
        }
        String eventKillerAccountId = event.path("killer").path("accountId").asText(null);
        return killerAccountId.equals(eventKillerAccountId);
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

    private List<WeaponKillCount> toSortedWeaponKillCounts(Map<String, Integer> killsByWeaponId) {
        List<WeaponKillCount> result = new ArrayList<>(killsByWeaponId.size());
        for (Map.Entry<String, Integer> entry : killsByWeaponId.entrySet()) {
            String weaponId = entry.getKey();
            result.add(new WeaponKillCount(weaponId, weaponNameResolver.resolve(weaponId), entry.getValue()));
        }
        result.sort(Comparator.comparingInt(WeaponKillCount::kills).reversed());
        return result;
    }
}
