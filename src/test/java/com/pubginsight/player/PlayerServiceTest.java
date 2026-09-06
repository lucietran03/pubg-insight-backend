package com.pubginsight.player;

import com.pubginsight.client.pubg.PubgApiClient;
import com.pubginsight.client.pubg.dto.PubgPlayerAttributes;
import com.pubginsight.client.pubg.dto.PubgPlayerData;
import com.pubginsight.client.pubg.dto.PubgPlayerListResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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
}
