package ru.practicum.explore_with_me.compilations.mapper;

import org.mapstruct.*;
import ru.practicum.explore_with_me.compilations.dto.CompilationDto;
import ru.practicum.explore_with_me.compilations.dto.CompilationResponse;
import ru.practicum.explore_with_me.compilations.model.Compilation;
import ru.practicum.explore_with_me.event.model.Event;

import java.util.Set;

@Mapper(componentModel = "spring")
public interface CompilationMapper {
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "events", source = "events")
    @Mapping(target = "pinned", defaultValue = "false")
    Compilation createRequestToCompilation(CompilationDto compilationDto, Set<Event> events);

    CompilationResponse compilationToResponse(Compilation compilation);

    @Mapping(target = "events", source = "events")
    @Mapping(target = "pinned", defaultValue = "false")
    @Mapping(target = "id", ignore = true)
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void compilationUpdateRequest(CompilationDto updateCompilationRequest, @MappingTarget Compilation compilation, Set<Event> events);
}
