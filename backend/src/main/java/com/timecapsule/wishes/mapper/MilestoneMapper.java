package com.timecapsule.wishes.mapper;

import com.timecapsule.wishes.dto.request.CreateMilestoneRequest;
import com.timecapsule.wishes.dto.request.UpdateMilestoneRequest;
import com.timecapsule.wishes.dto.response.MilestoneResponse;
import com.timecapsule.wishes.entity.Milestone;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

import java.util.List;

@Mapper(componentModel = "spring")
public interface MilestoneMapper {

    @Mapping(target = "recipientId", source = "recipient.id")
    MilestoneResponse toResponse(Milestone milestone);

    List<MilestoneResponse> toResponseList(List<Milestone> milestones);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "recipient", ignore = true)
    @Mapping(target = "wishes", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    Milestone toEntity(CreateMilestoneRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "recipient", ignore = true)
    @Mapping(target = "wishes", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateEntityFromRequest(UpdateMilestoneRequest request, @MappingTarget Milestone milestone);
}
