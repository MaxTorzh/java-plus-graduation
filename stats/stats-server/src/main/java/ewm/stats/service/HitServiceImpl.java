package ewm.stats.service;

import ewm.dto.EndpointHitDto;
import ewm.dto.EndpointHitResponseDto;
import ewm.dto.StatsRequestDto;
import ewm.dto.ViewStatsDto;
import ewm.stats.error.exception.BadRequestExceptions;
import ewm.stats.mapper.HitMapper;
import ewm.stats.model.Hit;
import ewm.stats.repository.HitRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class HitServiceImpl implements HitService {
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final String MISSING_DATES_ERROR = "You need to pass start and end dates";
    private static final String INVALID_DATE_RANGE_ERROR = "Start date must be before end date";

    private final HitMapper hitMapper;
    private final HitRepository hitRepository;

    @Override
    @Transactional
    public EndpointHitResponseDto create(EndpointHitDto endpointHitDto) {
        Hit hit = buildHitFromDto(endpointHitDto);
        Hit savedHit = hitRepository.save(hit);
        return hitMapper.hitToEndpointHitResponseDto(savedHit);
    }

    @Override
    public List<ViewStatsDto> getAll(StatsRequestDto statsRequest) {
        validateDateRange(statsRequest);
        LocalDateTime start = parseDateTime(statsRequest.getStart());
        LocalDateTime end = parseDateTime(statsRequest.getEnd());
        boolean hasUris = !statsRequest.getUris().isEmpty();

        List<HitRepository.ResponseHit> hits = fetchHits(statsRequest, start, end, hasUris);
        return convertHitsToViewStatsDTO(hits);
    }

    private Hit buildHitFromDto(EndpointHitDto dto) {
        return Hit.builder()
                .ip(dto.getIp())
                .app(dto.getApp())
                .uri(dto.getUri())
                .timestamp(parseDateTime(dto.getTimestamp()))
                .build();
    }

    private void validateDateRange(StatsRequestDto statsRequest) {
        if (statsRequest.getStart() == null || statsRequest.getEnd() == null) {
            throw new BadRequestExceptions(MISSING_DATES_ERROR);
        }
        LocalDateTime start = parseDateTime(statsRequest.getStart());
        LocalDateTime end = parseDateTime(statsRequest.getEnd());
        if (start.isAfter(end)) {
            throw new BadRequestExceptions(INVALID_DATE_RANGE_ERROR);
        }
    }

    private List<HitRepository.ResponseHit> fetchHits(StatsRequestDto statsRequest, LocalDateTime start,
                                                      LocalDateTime end, boolean hasUris) {
        if (Boolean.TRUE.equals(statsRequest.getUnique())) {
            return hitRepository.findAllUniqueBetweenDatesAndByUri(start, end, hasUris, statsRequest.getUris());
        }
        return hitRepository.findAllBetweenDatesAndByUri(start, end, hasUris, statsRequest.getUris());
    }

    private List<ViewStatsDto> convertHitsToViewStatsDTO(List<HitRepository.ResponseHit> hits) {
        return hits.stream()
                .map(this::buildViewStatsDtoFromResponseHit)
                .toList();
    }

    private ViewStatsDto buildViewStatsDtoFromResponseHit(HitRepository.ResponseHit responseHit) {
        return ViewStatsDto.builder()
                .app(responseHit.getApp())
                .uri(responseHit.getUri())
                .hits(responseHit.getHits())
                .build();
    }

    private LocalDateTime parseDateTime(String dateTime) {
        return LocalDateTime.parse(dateTime, DATE_TIME_FORMATTER);
    }
}