package com.pubginsight.client.telemetry;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.Map;

// Maps PUBG telemetry "damageCauserName" identifiers (e.g. "WeapAK47_C") to human-readable
// names (e.g. "AKM"), per CLAUDE.md's requirement that every user-facing label be
// understandable to someone who has never played PUBG.
//
// The mapping is bundled at src/main/resources/telemetry/damage-causer-names.json, copied
// verbatim from PUBG's own official dictionary
// (https://github.com/pubg/api-assets/blob/master/dictionaries/telemetry/damageCauserName.json,
// snapshot taken 2026-09-14) rather than hand-guessed - this is the same file the "official
// resources for PUBG API developers" repo ships for exactly this purpose, and it already
// covers non-weapon causers too (vehicles, environmental hazards, AI), which is why the field
// is named damageCauserName rather than weaponId.
//
// PUBG occasionally adds new items whose id won't be in this snapshot yet. Rather than
// fabricate a "pretty" name for an id we don't actually recognize, resolve() falls back to
// returning the raw id unchanged - an honest "we don't have a friendly name for this yet"
// signal instead of invented data.
@Component
public class WeaponNameResolver {

    private static final Logger log = LoggerFactory.getLogger(WeaponNameResolver.class);
    private static final String DICTIONARY_RESOURCE_PATH = "telemetry/damage-causer-names.json";

    private final Map<String, String> namesByCauserId;

    public WeaponNameResolver() {
        this.namesByCauserId = loadDictionary();
    }

    public String resolve(String damageCauserId) {
        if (damageCauserId == null) {
            return null;
        }
        return namesByCauserId.getOrDefault(damageCauserId, damageCauserId);
    }

    @SuppressWarnings("unchecked")
    private static Map<String, String> loadDictionary() {
        try (InputStream in = new ClassPathResource(DICTIONARY_RESOURCE_PATH).getInputStream()) {
            Map<String, String> dictionary = new ObjectMapper().readValue(in, Map.class);
            return Collections.unmodifiableMap(dictionary);
        } catch (IOException e) {
            // A missing/corrupt bundled resource must not crash the whole application at
            // startup (this feature is strictly additive) - fall back to "no mapping known",
            // which just means resolve() always returns the raw id until this is fixed.
            log.error("Failed to load bundled weapon name dictionary from '{}' - " +
                    "weapon breakdown will show raw PUBG item ids instead of friendly names",
                    DICTIONARY_RESOURCE_PATH, e);
            return Collections.emptyMap();
        }
    }
}
