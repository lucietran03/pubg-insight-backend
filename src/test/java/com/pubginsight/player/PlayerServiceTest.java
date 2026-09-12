package com.pubginsight.player;

import com.pubginsight.client.pubg.PubgApiClient;
import com.pubginsight.client.pubg.dto.PubgPlayerAttributes;
import com.pubginsight.client.pubg.dto.PubgPlayerData;
import com.pubginsight.client.pubg.dto.PubgPlayerListResponse;
import com.pubginsight.client.pubg.dto.PubgSeasonStatsAttributes;
import com.pubginsight.client.pubg.dto.PubgSeasonStatsData;
import com.pubginsight.client.pubg.dto.PubgSeasonStatsResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PlayerServiceTest {

    @Mock
    private PubgApiClient pubgApiClient;

    @Mock
    private PlayerMapper playerMapper;

    @InjectMocks
    private PlayerService playerService;

    @Test
    void throwsPlayerNotFoundWhenNoDataReturned() {
        when(pubgApiClient.findPlayerByName("ghost")).thenReturn(new PubgPlayerListResponse(List.of()));

        assertThatThrownBy(() -> playerService.searchPlayerByName("ghost"))
                .isInstanceOf(PlayerNotFoundException.class);
    }

    @Test
    void returnsMappedDtoWhenPlayerFound() {
        PubgPlayerData rawData = new PubgPlayerData("player", "account.1",
                new PubgPlayerAttributes("shroud", "steam", "pubg"), null);
        PlayerDto expectedDto = new PlayerDto("account.1", "shroud", "steam", List.of());

        when(pubgApiClient.findPlayerByName("shroud")).thenReturn(new PubgPlayerListResponse(List.of(rawData)));
        when(playerMapper.toPlayerDto(rawData)).thenReturn(expectedDto);

        PlayerDto result = playerService.searchPlayerByName("shroud");

        assertThat(result).isEqualTo(expectedDto);
    }

    @Test
    void attachesPreviousSeasonComparisonWhenPreviousSeasonStatsAvailable() {
        PubgSeasonStatsAttributes currentAttributes = new PubgSeasonStatsAttributes(Map.of());
        PubgSeasonStatsAttributes previousAttributes = new PubgSeasonStatsAttributes(Map.of());
        SeasonStatsDto currentDto = seasonStats(0.2, 0.4);
        SeasonStatsDto previousDto = seasonStats(0.1, 0.5);
        SeasonComparison comparison = new SeasonComparison(100.0, 0.0, 0.0, -20.0, 0.0);

        when(pubgApiClient.findCurrentSeasonId()).thenReturn("season-current");
        when(pubgApiClient.findSeasonStats("account.1", "season-current"))
                .thenReturn(new PubgSeasonStatsResponse(new PubgSeasonStatsData("playerSeason", "id", currentAttributes)));
        when(playerMapper.toSeasonStatsDto(currentAttributes)).thenReturn(currentDto);

        when(pubgApiClient.findPreviousSeasonId()).thenReturn("season-previous");
        when(pubgApiClient.findSeasonStats("account.1", "season-previous"))
                .thenReturn(new PubgSeasonStatsResponse(new PubgSeasonStatsData("playerSeason", "id", previousAttributes)));
        when(playerMapper.toSeasonStatsDto(previousAttributes)).thenReturn(previousDto);
        when(playerMapper.computeSeasonComparison(currentDto, previousDto)).thenReturn(comparison);

        SeasonStatsDto result = playerService.getSeasonStats("account.1");

        assertThat(result.previousSeasonComparison()).isEqualTo(comparison);
    }

    @Test
    void omitsComparisonWhenNoPreviousSeasonExists() {
        PubgSeasonStatsAttributes currentAttributes = new PubgSeasonStatsAttributes(Map.of());
        SeasonStatsDto currentDto = seasonStats(0.2, 0.4);

        when(pubgApiClient.findCurrentSeasonId()).thenReturn("season-current");
        when(pubgApiClient.findSeasonStats("account.1", "season-current"))
                .thenReturn(new PubgSeasonStatsResponse(new PubgSeasonStatsData("playerSeason", "id", currentAttributes)));
        when(playerMapper.toSeasonStatsDto(currentAttributes)).thenReturn(currentDto);

        // Brand-new game/shard: there is no season before the current one at all.
        when(pubgApiClient.findPreviousSeasonId()).thenReturn(null);

        SeasonStatsDto result = playerService.getSeasonStats("account.1");

        assertThat(result.previousSeasonComparison()).isNull();
        // No previous season ID means there's nothing to fetch stats for - only the
        // current season's findSeasonStats call should have happened.
        verify(pubgApiClient, times(1)).findSeasonStats(anyString(), anyString());
    }

    @Test
    void omitsComparisonWhenPlayerHasNoStatsForPreviousSeason() {
        PubgSeasonStatsAttributes currentAttributes = new PubgSeasonStatsAttributes(Map.of());
        SeasonStatsDto currentDto = seasonStats(0.2, 0.4);

        when(pubgApiClient.findCurrentSeasonId()).thenReturn("season-current");
        when(pubgApiClient.findSeasonStats("account.1", "season-current"))
                .thenReturn(new PubgSeasonStatsResponse(new PubgSeasonStatsData("playerSeason", "id", currentAttributes)));
        when(playerMapper.toSeasonStatsDto(currentAttributes)).thenReturn(currentDto);

        // Previous season exists, but this player (e.g. a newly created account) has no
        // recorded stats for it - PUBG returns 404, surfaced here as a null response.
        when(pubgApiClient.findPreviousSeasonId()).thenReturn("season-previous");
        when(pubgApiClient.findSeasonStats("account.1", "season-previous")).thenReturn(null);

        SeasonStatsDto result = playerService.getSeasonStats("account.1");

        assertThat(result.previousSeasonComparison()).isNull();
    }

    private static SeasonStatsDto seasonStats(double winRate, double headshotRate) {
        return new SeasonStatsDto(
                0, 0, winRate, 0.0, 0.0, headshotRate, 0.0, 0.0, 0.0,
                new RadarScores(0, 0, 0, 0, 0, 0), "Balanced Operator", null);
    }
}
