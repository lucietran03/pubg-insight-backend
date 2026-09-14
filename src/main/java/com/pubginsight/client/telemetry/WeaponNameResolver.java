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

// resolve() falls back to the raw id when it isn't in the dictionary yet, rather than
// fabricating a name.
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
            // A missing/corrupt bundled resource must not crash startup - fall back to no
            // mapping, so resolve() just returns raw ids until this is fixed.
            log.error("Failed to load bundled weapon name dictionary from '{}' - " +
                    "weapon breakdown will show raw PUBG item ids instead of friendly names",
                    DICTIONARY_RESOURCE_PATH, e);
            return Collections.emptyMap();
        }
    }
}
