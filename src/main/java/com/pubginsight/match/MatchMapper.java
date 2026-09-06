package com.pubginsight.match;

import com.pubginsight.client.pubg.dto.PubgMatchAttributes;
import com.pubginsight.client.pubg.dto.PubgParticipantStats;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class MatchMapper {

    // PUBG's API returns internal map codes, not the names players know - not every map
    // PUBG has ever shipped is listed here, only the ones in current/recent rotation.
    // Unmapped codes fall through to the raw value rather than disappearing.
    private static final Map<String, String> MAP_NAMES = Map.ofEntries(
            Map.entry("Baltic_Main", "Erangel"),
            Map.entry("Desert_Main", "Miramar"),
            Map.entry("Savage_Main", "Sanhok"),
            Map.entry("DihorOtok_Main", "Vikendi"),
            Map.entry("Tiger_Main", "Taego"),
            Map.entry("Kiki_Main", "Deston"),
            Map.entry("Chimera_Main", "Paramo"),
            Map.entry("Heaven_Main", "Haven"),
            Map.entry("Summerland_Main", "Karakin"),
            Map.entry("Range_Main", "Camp Jackal"),
            Map.entry("Neon_Main", "Rondo")
    );

    // Same idea as MAP_NAMES: PUBG's gameMode is a lowercase/hyphenated code, not a label.
    private static final Map<String, String> GAME_MODE_LABELS = Map.ofEntries(
            Map.entry("solo", "Solo"),
            Map.entry("solo-fpp", "Solo FPP"),
            Map.entry("duo", "Duo"),
            Map.entry("duo-fpp", "Duo FPP"),
            Map.entry("squad", "Squad"),
            Map.entry("squad-fpp", "Squad FPP")
    );

    public MatchDto toMatchDto(String matchId, PubgMatchAttributes matchAttributes, PubgParticipantStats stats) {
        int kills = stats.kills() == null ? 0 : stats.kills();
        int headshotKills = stats.headshotKills() == null ? 0 : stats.headshotKills();
        double headshotRate = kills == 0 ? 0.0 : (double) headshotKills / kills;

        return new MatchDto(
                matchId,
                MAP_NAMES.getOrDefault(matchAttributes.mapName(), matchAttributes.mapName()),
                GAME_MODE_LABELS.getOrDefault(matchAttributes.gameMode(), matchAttributes.gameMode()),
                kills,
                headshotKills,
                headshotRate,
                stats.damageDealt() == null ? 0.0 : stats.damageDealt(),
                stats.timeSurvived() == null ? 0.0 : stats.timeSurvived(),
                stats.winPlace() == null ? 0 : stats.winPlace(),
                matchAttributes.createdAt()
        );
    }
}
