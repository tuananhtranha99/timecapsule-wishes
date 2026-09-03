package com.timecapsule.wishes.mapper;

import com.timecapsule.wishes.dto.request.CreateRecipientRequest;
import com.timecapsule.wishes.dto.request.UpdateRecipientRequest;
import com.timecapsule.wishes.dto.response.RecipientResponse;
import com.timecapsule.wishes.entity.Recipient;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

import java.util.List;

@Mapper(componentModel = "spring")
public interface RecipientMapper {

    RecipientResponse toResponse(Recipient recipient);

    List<RecipientResponse> toResponseList(List<Recipient> recipients);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "user", ignore = true)
    @Mapping(target = "milestones", ignore = true)
    @Mapping(target = "generatedWishes", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    Recipient toEntity(CreateRecipientRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "user", ignore = true)
    @Mapping(target = "milestones", ignore = true)
    @Mapping(target = "generatedWishes", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateEntityFromRequest(UpdateRecipientRequest request, @MappingTarget Recipient recipient);
}
