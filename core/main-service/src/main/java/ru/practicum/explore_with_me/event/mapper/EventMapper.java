package ru.practicum.explore_with_me.event.mapper;

import org.mapstruct.*;
import ru.practicum.explore_with_me.event.dto.*;
import ru.practicum.explore_with_me.event.model.Event;
import ru.practicum.explore_with_me.user.mapper.UserMapper;

@Mapper(
        componentModel = "spring",
        uses = {UserMapper.class},
        unmappedTargetPolicy = ReportingPolicy.ERROR   // пусть падает, если что-то забудем
)
public interface EventMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "category.id", source = "category")
    @Mapping(target = "createdOn", expression = "java(java.time.LocalDateTime.now())")
    @Mapping(target = "state", expression = "java(ru.practicum.explore_with_me.event.model.enums.EventState.PENDING)")
    @Mapping(target = "participantLimit", source = "participantLimit", defaultValue = "0")
    @Mapping(target = "initiator",          ignore = true)
    @Mapping(target = "confirmedRequests",  ignore = true)
    @Mapping(target = "publishedOn",        ignore = true)
    @Mapping(target = "views",              ignore = true)
    Event toEvent(NewEventDto newEventDto);

    @Mapping(target = "initiator",  source = "initiator")          // User -> UserShortDto  (UserMapper поможет)
    @Mapping(target = "confirmedRequests", source = "confirmedRequests")
    @Mapping(target = "publishedOn",  source = "publishedOn")
    @Mapping(target = "views",        source = "views")
    @Mapping(target = "id",           source = "id")
    @Mapping(target = "state",        source = "state")
    @Mapping(target = "createdOn",    source = "createdOn")
    EventFullDto toFullDto(Event event);

    @Mapping(target = "id",               source = "id")
    @Mapping(target = "annotation",       source = "annotation")
    @Mapping(target = "eventDate",        source = "eventDate")
    @Mapping(target = "paid",             source = "paid")
    @Mapping(target = "title",            source = "title")
    @Mapping(target = "confirmedRequests", source = "confirmedRequests")
    @Mapping(target = "views",             source = "views")
    @Mapping(target = "initiator",         source = "initiator")  // User -> UserShortDto
    EventShortDto toShortDto(Event event);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "category.id", source = "category")
    @Mapping(target = "id",                ignore = true)
    @Mapping(target = "initiator",         ignore = true)
    @Mapping(target = "state",             ignore = true)
    @Mapping(target = "createdOn",         ignore = true)
    @Mapping(target = "confirmedRequests", ignore = true)
    @Mapping(target = "publishedOn",       ignore = true)
    @Mapping(target = "views",             ignore = true)
    void updateUserRequest(UpdateEventUserRequest userRequest, @MappingTarget Event event);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "category.id", source = "category")
    @Mapping(target = "id",                ignore = true)
    @Mapping(target = "initiator",         ignore = true)
    @Mapping(target = "state",             ignore = true)
    @Mapping(target = "createdOn",         ignore = true)
    @Mapping(target = "confirmedRequests", ignore = true)
    @Mapping(target = "publishedOn",       ignore = true)
    @Mapping(target = "views",             ignore = true)
    void patchUserRequest(AdminPatchEventDto adminPatchEventDto, @MappingTarget Event event);
}