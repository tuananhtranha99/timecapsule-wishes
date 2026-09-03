package com.timecapsule.wishes.service;

import com.timecapsule.wishes.dto.request.EditWishRequest;
import com.timecapsule.wishes.dto.request.GenerateWishRequest;
import com.timecapsule.wishes.dto.response.WishResponse;
import com.timecapsule.wishes.security.UserPrincipal;

import java.util.List;
import java.util.UUID;

public interface WishGenerationService {

    WishResponse generateWish(GenerateWishRequest request, UserPrincipal principal);

    List<WishResponse> getWishesByRecipient(UUID recipientId, UserPrincipal principal);

    WishResponse getWishById(UUID id, UserPrincipal principal);

    WishResponse editWish(UUID id, EditWishRequest request, UserPrincipal principal);
}
