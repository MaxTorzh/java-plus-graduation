package ewm.stats.service;

import ewm.dto.EndpointHitDto;
import ewm.dto.EndpointHitResponseDto;
import ewm.dto.StatsRequestDto;
import ewm.dto.ViewStatsDto;

import java.util.List;

public interface HitService {
    EndpointHitResponseDto create(EndpointHitDto endpointHitDTO);

    List<ViewStatsDto> getAll(StatsRequestDto statsRequestDTO);
}
