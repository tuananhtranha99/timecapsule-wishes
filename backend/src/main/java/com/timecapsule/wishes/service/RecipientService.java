package com.timecapsule.wishes.service;

import com.timecapsule.wishes.dto.request.CreateRecipientRequest;
import com.timecapsule.wishes.dto.request.UpdateRecipientRequest;
import com.timecapsule.wishes.dto.response.RecipientResponse;
import com.timecapsule.wishes.security.UserPrincipal;

import java.util.List;
import java.util.UUID;

public interface RecipientService {

    List<RecipientResponse> getAllRecipients(UserPrincipal principal);

    RecipientResponse getRecipientById(UUID id, UserPrincipal principal);

    RecipientResponse createRecipient(CreateRecipientRequest request, UserPrincipal principal);

    RecipientResponse updateRecipient(UUID id, UpdateRecipientRequest request, UserPrincipal principal);

    void deleteRecipient(UUID id, UserPrincipal principal);
}
