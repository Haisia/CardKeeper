---
phase: 1
slug: foundation
status: draft
nyquist_compliant: false
wave_0_complete: false
created: 2026-03-24
---

# Phase 1 — Validation Strategy

> Per-phase validation contract for feedback sampling during execution.

---

## Test Infrastructure

| Property | Value |
|----------|-------|
| **Framework** | Android instrumented tests (androidx.test) + JUnit 4 |
| **Config file** | `app/src/androidTest/` and `app/src/test/` |
| **Quick run command** | `./gradlew :app:testDebugUnitTest` |
| **Full suite command** | `./gradlew :app:testDebugUnitTest :app:connectedDebugAndroidTest` |
| **Estimated runtime** | ~30 seconds (unit) / ~120 seconds (instrumented) |

---

## Sampling Rate

- **After every task commit:** Run `./gradlew :app:testDebugUnitTest`
- **After every plan wave:** Run `./gradlew :app:testDebugUnitTest`
- **Before `/gsd:verify-work`:** Full suite must be green
- **Max feedback latency:** 30 seconds

---

## Per-Task Verification Map

| Task ID | Plan | Wave | Requirement | Test Type | Automated Command | File Exists | Status |
|---------|------|------|-------------|-----------|-------------------|-------------|--------|
| 1-01-01 | 01-01 | 1 | Gradle setup | build | `./gradlew :app:assembleDebug` | ❌ W0 | ⬜ pending |
| 1-02-01 | 01-02 | 1 | Room schema | unit | `./gradlew :app:testDebugUnitTest` | ❌ W0 | ⬜ pending |
| 1-03-01 | 01-03 | 2 | Hilt modules | build | `./gradlew :app:assembleDebug` | ❌ W0 | ⬜ pending |
| 1-04-01 | 01-04 | 2 | Navigation | build | `./gradlew :app:assembleDebug` | ❌ W0 | ⬜ pending |

*Status: ⬜ pending · ✅ green · ❌ red · ⚠️ flaky*

---

## Wave 0 Requirements

- [ ] `app/src/test/java/com/cardkeeper/data/CardDaoTest.kt` — Room DAO unit tests
- [ ] `app/src/test/java/com/cardkeeper/data/TagDaoTest.kt` — Tag DAO unit tests

*Existing infrastructure covers build verification tasks.*

---

## Manual-Only Verifications

| Behavior | Requirement | Why Manual | Test Instructions |
|----------|-------------|------------|-------------------|
| App launches to card list stub | Phase 1 goal | Requires Android emulator/device | Run app on emulator, verify CardList stub screen appears |
| Navigation routes between stubs | Phase 1 goal | UI interaction required | Tap stub nav elements, verify all 5 screens reachable |

---

## Validation Sign-Off

- [ ] All tasks have `<automated>` verify or Wave 0 dependencies
- [ ] Sampling continuity: no 3 consecutive tasks without automated verify
- [ ] Wave 0 covers all MISSING references
- [ ] No watch-mode flags
- [ ] Feedback latency < 30s
- [ ] `nyquist_compliant: true` set in frontmatter

**Approval:** pending
