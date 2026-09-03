package com.timecapsule.wishes.service;

import com.timecapsule.wishes.enums.WishLanguage;

import java.util.List;

public interface AiClient {

    /**
     * Generates a personalized wish based on recipient milestones and occasion context.
     *
     * @param prompt     Additional prompt/context instructions (e.g. occasion details, recipient relationship)
     * @param milestones List of milestone descriptions to weave into the wish
     * @param language   Target language (VI or EN)
     * @return Synthesized personalized wish text
     */
    String generateWish(String prompt, List<String> milestones, WishLanguage language);
}
