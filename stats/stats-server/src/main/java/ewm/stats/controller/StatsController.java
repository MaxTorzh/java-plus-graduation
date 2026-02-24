package ewm.stats.controller;

import ewm.dto.EndpointHitDto;
import ewm.dto.EndpointHitResponseDto;
import ewm.dto.StatsRequestDto;
import ewm.dto.ViewStatsDto;
import ewm.stats.service.HitService;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@AllArgsConstructor
public class StatsController extends ErrorHandler {
    private HitService hitService;

    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping("/hit")
    public EndpointHitResponseDto createHit(@RequestBody @Valid EndpointHitDto endpointHitDto) {
        return hitService.create(endpointHitDto);
    }

    @GetMapping("/stats")
    public List<ViewStatsDto> getHits(
            StatsRequestDto statsRequestDTO
    ) {
        return hitService.getAll(statsRequestDTO);
    }
}