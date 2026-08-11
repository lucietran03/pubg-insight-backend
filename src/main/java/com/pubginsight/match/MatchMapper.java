package com.pubginsight.match;

import com.pubginsight.client.pubg.dto.PubgMatchAttributes;
import com.pubginsight.client.pubg.dto.PubgParticipantStats;
import org.springframework.stereotype.Component;

@Component
public class MatchMapper {

    public MatchDto toMatchDto(String matchId, PubgMatchAttributes matchAttributes, PubgParticipantStats stats) {
        int kills = stats.kills() == null ? 0 : stats.kills();
        int headshotKills = stats.headshotKills() == null ? 0 : stats.headshotKills();
        double headshotRate = kills == 0 ? 0.0 : (double) headshotKills / kills;

        return new MatchDto(
                matchId,
                matchAttributes.mapName(),
                matchAttributes.gameMode(),
                kills,
                headshotKills,
                headshotRate,
                stats.damageDealt() == null ? 0.0 : stats.damageDealt(),
                stats.timeSurvived() == null ? 0.0 : stats.timeSurvived(),
                stats.winPlace() == null ? 0 : stats.winPlace()
        );
    }
}
