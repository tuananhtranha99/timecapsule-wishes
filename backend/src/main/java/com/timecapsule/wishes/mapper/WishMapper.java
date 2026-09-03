package com.timecapsule.wishes.mapper;

import com.timecapsule.wishes.dto.response.WishResponse;
import com.timecapsule.wishes.entity.GeneratedWish;
import com.timecapsule.wishes.entity.Milestone;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;
import java.util.UUID;

@Mapper(componentModel = "spring")
public interface WishMapper {

    @Mapping(target = "recipientId", source = "recipient.id")
    @Mapping(target = "recipientName", source = "recipient.name")
    @Mapping(target = "milestoneIds", expression = "java(mapMilestonesToIds(wish))")
    WishResponse toResponse(GeneratedWish wish);

    List<WishResponse> toResponseList(List<GeneratedWish> wishes);

    default List<UUID> mapMilestonesToIds(GeneratedWish wish) {
        if (wish.getMilestones() == null) {
            return List.of();
        }
        return wish.getMilestones().stream()
                .map(Milestone::getId)
                .toList();
    }
}
