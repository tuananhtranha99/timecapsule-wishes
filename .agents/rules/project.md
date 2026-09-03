---
trigger: always_on
---

# PROJECT.md — Project-Specific Context

> This file complements `GEMINI.md`. `GEMINI.md` defines *how* we build (architecture, patterns, stack, deployment). This file defines *what* we're building for THIS project specifically: domain, business rules, entities, and any deviations from the global standards. `/planning` must read this file before producing any implementation plan.

---

## 1. Project Overview

**Name:** Time-Capsule Wishes (Thiệp Chúc & Quà Tặng Cá Nhân Hóa Theo Thời Gian)
**One-line pitch:** Instead of generic birthday messages, generate deeply personalized wishes assembled from the real milestones the recipient logged throughout the year.
**Target users:** Individuals who want to send more thoughtful, specific birthday/holiday wishes to friends and family, based on what actually happened in that person's life over the past year.
**Current stage:** MVP / early development.

---

## 2. Problem & Core Idea

Generic birthday messages ("Happy birthday! Wishing you all the best!") feel impersonal and interchangeable. The insight of this product: a truly touching wish references *specific, real things* the person went through that year — passed a visa interview, bought a car, finished a marathon, changed jobs. Instead of trying to remember all of this from memory at the last minute, the user logs short one-line notes throughout the year whenever the recipient hits a milestone. On the recipient's birthday, the app uses AI to synthesize those logged milestones into one cohesive, deeply personal wish message.

---

## 3. Domain Model

| Entity | Description | Key relationships |
|---|---|---|
| `User` | The person using the app (the one writing wishes, i.e. the "wisher") | has many `Recipient` |
| `Recipient` | A person the user wants to wish (friend, family member, partner) — holds birthday/anniversary date | belongs to `User`; has many `Milestone`; has many `GeneratedWish` |
| `Milestone` | A short, timestamped log entry describing something the recipient experienced ("passed visa interview", "bought a new car") | belongs to `Recipient`; optionally referenced by many `GeneratedWish` |
| `GeneratedWish` | An AI-generated wish message compiled from a set of milestones for a specific occasion | belongs to `Recipient`; references many `Milestone`; has an `occasion` (birthday, Tết, anniversary, custom) |
| `Occasion` (value, not necessarily its own table) | The event the wish is being generated for — birthday is default/primary, but the model should not hardcode "birthday" only | tied to `Recipient` (birthday) or manually specified |

*(Keep this table in sync as `/planning` introduces new entities — e.g. reminder scheduling, wish tone presets.)*

---

## 4. Core Business Flows

### Flow: Logging a milestone
1. **Trigger:** User manually adds a quick note about a recipient at any point during the year (e.g. from a mobile-friendly quick-add form).
2. **Steps:** User selects the `Recipient`, types a short free-text description (e.g. "passed her visa interview"), optionally tags a category (career, travel, health, relationship, achievement, other), and the milestone is saved with a timestamp.
3. **Result / side effects:** A new `Milestone` row is created and becomes eligible for inclusion in future wish generation for that recipient.
4. **Edge cases specific to this flow:**
   - Very short or vague milestone text (e.g. "good day") — should still be storable, but flag for the AI prompt to skip or handle gracefully rather than force it into the wish.
   - Duplicate/near-duplicate milestones logged twice for the same event.
   - Milestone logged with a backdated date (user remembers something from months ago and logs it late) — must be supported, since the whole point is capturing things that happened *during* the year, not just at logging time.

### Flow: Generating a personalized wish
1. **Trigger:** Recipient's birthday (or other configured occasion) approaches, or user manually requests wish generation ahead of time.
2. **Steps:**
   - System gathers all `Milestone` entries for that `Recipient` within the relevant time window (default: past 12 months, configurable).
   - Milestones are sent to the AI along with the recipient's name and the occasion type.
   - AI returns a synthesized wish message that naturally references the specific milestones (per §5 contract below).
   - User reviews/edits the generated wish before sending/copying it.
3. **Result / side effects:** A `GeneratedWish` record is stored (for history/reuse), linked to the milestones it drew from.
4. **Edge cases specific to this flow:**
   - **Zero milestones logged** for that recipient — the AI must not hallucinate fake achievements; fall back to a warm generic wish and clearly signal to the user that no milestones were found to personalize with.
   - **Only 1 milestone** — wish should still read naturally, not awkwardly padded.
   - **Very many milestones** (10+) — the AI shouldn't cram all of them in; needs a strategy for picking/prioritizing the most significant ones (see Open Questions).
   - Recipient's birthday falls on a date with no logged activity that year — same as zero-milestone case.

### Flow: Reminder before the occasion (if in scope for MVP — confirm in §10)
1. **Trigger:** A scheduled check (e.g. daily cron) finds recipients whose birthday is N days away.
2. **Steps:** Notify the user (in-app, email, or push — TBD) that it's almost time, prompting them to review/generate the wish in advance.
3. **Result / side effects:** No data mutation beyond a notification log, if tracked.
4. **Edge cases:** Recipient with no birthday date set; multiple recipients with birthdays on the same day.

---

---

## 6. Non-Functional Requirements Specific to This Project

- **Performance:** Wish generation does not need to be instant — a short loading state (few seconds) is acceptable since it's a deliberate, occasional action, not a hot path.
- **Notification/scheduling needs:** if the reminder flow is in scope, a daily cron/scheduled job is needed to scan upcoming birthdays — mind Render's free-tier cold start behavior when scheduling this (see `GEMINI.md` §6).
- **Localization:** primary users are Vietnamese-speaking; wish generation prompt and UI copy should support Vietnamese as a first-class language, not just English. Confirm whether English support is also needed.
- **Free-tier constraints to design around:** Supabase connection pooling (per `GEMINI.md` §4) matters more here if the reminder cron causes periodic connection bursts; Netlify build minutes are not a major concern for this app's scale.

---

## 7. Deviations from GEMINI.md

_None yet — global rules apply as-is. Add rows here if a decision is made to deviate (e.g. choosing BIGSERIAL over UUID for simplicity at this scale)._

| Global rule | Deviation in this project | Reason |
|---|---|---|
| | | |

---

## 8. Out of Scope (for MVP)

- Multi-user collaboration on the same recipient (e.g. a whole family jointly logging milestones for one person) — single-owner model only for now.
- Physical gift recommendations/e-commerce integration (the "& Quà Tặng" / gift half of the concept) — MVP focuses on the wish-text generation only, unless explicitly greenlit later.
- Automatic milestone detection (e.g. scanning social media/calendar for events) — all milestones are manually logged by the user for MVP.
- Multi-language wish generation beyond Vietnamese/English (until confirmed needed).

---

## 9. Glossary

| Term | Meaning |
|---|---|
| Milestone | A short, user-logged note describing something the recipient experienced during the year |
| Recipient | The person being wished (not the app's registered user) |
| Wisher | The registered `User` who logs milestones and generates wishes |
| Occasion | The event a wish is generated for — birthday by default, extensible to Tết/anniversary/other |
| Time-Capsule | The overall concept: milestones accumulate quietly over time, then "open up" into a wish at the occasion |

---

## 10. Open Questions
- Is the **reminder/notification flow** in scope for MVP, or is wish generation purely on-demand for now? -> on-demand
- How should the system **prioritize milestones** when there are many (10+) for one recipient — most recent? most "significant" (needs a definition)? user-selectable? -> let me choose
- Do we need **user consent/awareness messaging** about recipient data being sent to a third-party AI provider? -> no
- Is editing a `GeneratedWish` after creation saved as a revision, or is regenerating always a fresh AI call? -> y
- Single language (Vietnamese) for MVP, or bilingual VI/EN from day one? -> bilingual VI/EN