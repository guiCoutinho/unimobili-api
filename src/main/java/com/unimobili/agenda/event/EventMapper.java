package com.unimobili.agenda.event;

import com.unimobili.agenda.event.dto.EventResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface EventMapper {

    @Mapping(target = "externalUserId", source = "externalUser.id")
    EventResponse toResponse(Event event);
}
