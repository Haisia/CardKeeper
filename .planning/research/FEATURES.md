# Feature Landscape

**Domain:** Android business card management app (personal, ad-free, local-only)
**Project:** CardKeeper
**Researched:** 2026-03-24
**Confidence:** MEDIUM — based on domain knowledge of established apps (CamCard, ABBYY Business Card Reader, Haystack, Cardhop, Samsung Business Card); web search unavailable during this session

---

## Table Stakes

Features users expect from any business card manager. Missing = product feels incomplete or broken.

| Feature | Why Expected | Complexity | Notes |
|---------|--------------|------------|-------|
| Camera capture of business card | Primary input method; core reason to install | Low | Requires CameraX permission flow; real-time preview with card-edge guidance overlay helps UX |
| OCR text extraction | Without this, it's just a photo album | Medium | ML Kit Text Recognition v2; on-device, free, no API key |
| Parsed fields: name, company, title, phone, email, address | Users need structured data, not raw text | High | Parsing heuristics matter; multi-field ambiguity is a known pitfall |
| Manual correction of parsed fields | OCR is never 100%; users must be able to fix mistakes | Low | Editable form post-scan; critical for trust |
| Card photo stored alongside parsed data | Visual reference for context; users remember faces/layouts | Low | Save to app internal storage (not gallery); Room foreign key to image file path |
| Card list view | Entry point into all stored cards | Low | RecyclerView/LazyColumn; thumbnail + name + company |
| Card detail view | See all data for one card | Low | Full image, all fields, tags, memo |
| Search by name / company | Core utility; useless if you can't find a card | Low | Room FTS or LIKE query on name + company + title |
| Delete a card | Basic CRUD; users accumulate stale cards | Low | Confirm dialog to avoid accidents |
| Edit stored card | Fields change; people get promoted, change numbers | Low | Same form as post-scan correction |

---

## Differentiators

Features that set this product apart. Not universally expected, but meaningfully valued.

| Feature | Value Proposition | Complexity | Notes |
|---------|-------------------|------------|-------|
| Gallery import (existing photo) | Users have photos of cards they didn't scan live; bridges past accumulation | Low | `ActivityResultContracts.GetContent`; same OCR pipeline as camera path |
| Tag-based grouping (multi-tag per card) | More flexible than folders; a card can belong to "Client" and "Tokyo 2024" simultaneously | Medium | Many-to-many join table in Room; tag filter UI with chip multiselect |
| Filter by tag | Tags are useless without fast access; expected once tags exist | Low | Depends on tag feature; chips above list |
| Per-card memo field | Context users can't derive from the card itself ("met at Startup Weekend, follow up Q2") | Low | Plain text field; stored in Room alongside parsed fields |
| Export to Android Contacts | Converts passive archive into active tool; one tap to call/email | Medium | ContactsContract API; requires WRITE_CONTACTS permission; map parsed fields to vCard-equivalent |
| Zero ads / no tracking | Explicit design goal; primary reason this app exists over alternatives | None (policy) | Manifest: no ad SDK, no analytics SDK |
| Fully offline operation | No sign-in friction, no data sent to cloud, works without connectivity | Low (by architecture) | ML Kit on-device satisfies this; no network permission needed |
| Real-time search (instant filter as you type) | Users scan app lists, not submit forms | Low | `Flow` + `debounce` on search input; Room query reactive via `Flow<List<Card>>` |

---

## Anti-Features

Features to explicitly NOT build for v1 (and probably ever, given project goals).

| Anti-Feature | Why Avoid | What to Do Instead |
|--------------|-----------|-------------------|
| Cloud sync / backup | Adds auth complexity, privacy surface, infra cost; out of scope per PROJECT.md | Document how to use Android's local backup (ADB backup or manual APK data export) |
| In-app purchases / subscriptions | Contradicts the "ad-free personal app" premise | None; keep all features free |
| Ads (any form) | Primary motivation for building this app was to escape ads in existing apps | None |
| Card sharing / beam / NFC exchange | Turns app into a social network; scope creep; v1 is a personal archive | Defer to post-v1 if demand emerges |
| iOS / cross-platform port | Android-native is a deliberate choice (Kotlin + Compose); multiplatform adds significant cost | N/A |
| Team / multi-user access | Single-device personal use; sharing logic multiplies complexity | N/A |
| Business card design / creation | Output tool, not input tool | N/A |
| LinkedIn / CRM integration | API key management, OAuth flow, external dependency; overkill for personal use | Manual copy-paste from detail view is sufficient |
| AI "relationship suggestions" / smart follow-up reminders | Over-engineering for v1; maintenance burden | Per-card memo covers the manual version of this |
| Folder hierarchy (nested groups) | Tag-based grouping already covers categorization; folders add navigation complexity for no gain | Use tags |
| Duplicate detection / merge | Edge case, tricky to implement well (fuzzy match on phone/email); low value for single user | User manually manages duplicates via edit/delete |

---

## Feature Dependencies

```
Camera capture ────────────────┐
                                ├──> OCR extraction ──> Field parsing ──> Manual correction ──> Save card
Gallery import ─────────────────┘                                                                    │
                                                                                                     │
                                    ┌────────────────────────────────────────────────────────────────┘
                                    │
                                    ├──> Card list view ──> Search / real-time filter
                                    │                  └──> Filter by tag (requires tag feature)
                                    │
                                    ├──> Card detail view ──> Edit ──> Save
                                    │                    └──> Delete
                                    │                    └──> Per-card memo (read/write)
                                    │
                                    ├──> Tag assignment (requires saved card)
                                    │       └──> Tag filter (requires tags)
                                    │
                                    └──> Export to Contacts (requires parsed fields: name, phone, email)
```

**Hard dependencies:**
- Tag filter requires tag assignment to exist
- Export to Contacts requires at minimum name + one contact field (phone or email) to be non-empty
- Gallery import re-uses OCR pipeline; build OCR pipeline first, then plug gallery source in
- Manual correction form can be built once and reused for both post-scan and "edit stored card" flows

---

## MVP Recommendation

The minimum product where the core value proposition ("camera once, find instantly") is fully realized:

**Prioritize (v1 core loop):**
1. Camera capture + ML Kit OCR extraction
2. Field parsing (name / company / title / phone / email / address) + manual correction form
3. Card photo + parsed data saved to Room DB
4. Card list view + card detail view
5. Real-time search (name, company, title)
6. Edit + delete

**Prioritize (v1 convenience layer — high leverage, low effort):**
7. Gallery import (reuses OCR pipeline; low marginal cost)
8. Per-card memo (single text field; trivial to add)
9. Tag assignment + tag filter (medium effort; core differentiator for organization)
10. Export to Android Contacts (medium effort; converts app from passive archive to active tool)

**Defer (post-v1):**
- Batch import (multiple cards at once)
- Sort options beyond default (e.g., by company, by date added)
- Card sharing via intent / NFC
- Home screen widget showing recently added cards

---

## Complexity Notes on Key Features

### OCR Field Parsing (HIGH complexity)
ML Kit returns raw text lines; extracting *which* line is the name vs company vs phone requires heuristics. Common approaches:
- Phone: regex on E.164 / local formats
- Email: regex
- Name: often largest font (line order heuristic), no reliable ML signal from raw text
- Company: typically second line, but business card layouts vary wildly
- Address: multi-line; hardest to isolate without a dedicated address parser

Expect: wrong field attribution on ~20–30% of cards, which is why manual correction is non-optional.

### Export to Android Contacts (MEDIUM complexity)
`ContactsContract` API is verbose but well-documented. Map fields:
- `StructuredName` ← parsed name
- `Phone` ← parsed phone (handle multiple numbers)
- `Email` ← parsed email
- `Organization` ← company + title
- `StructuredPostal` ← address

Requires `WRITE_CONTACTS` runtime permission. Show a preview before inserting so users can confirm.

### Tag System (MEDIUM complexity)
Room schema: `Card`, `Tag`, `CardTagCrossRef` (many-to-many). Tag creation UX: inline chip input on card edit screen. Filter UX: multi-select chip strip above list (AND vs OR semantics — OR is more forgiving for users).

---

## Sources

- PROJECT.md — project requirements, constraints, out-of-scope decisions (HIGH confidence; authoritative)
- Domain knowledge: CamCard, ABBYY Business Card Reader, Haystack, Cardhop, Samsung Business Card Scanner feature sets as known at training cutoff (MEDIUM confidence — apps may have updated)
- Android ContactsContract API behavior (MEDIUM confidence — stable API, unchanged since API 5)
- ML Kit Text Recognition v2 capabilities (MEDIUM confidence — verified against known Google documentation patterns; Context7 / web search unavailable this session)

**Note:** WebSearch and Bash tools were denied during this research session. Findings rely on training data (cutoff August 2025) and project-provided context. Recommend verifying competitor feature sets via Play Store listings before finalizing roadmap priorities.
