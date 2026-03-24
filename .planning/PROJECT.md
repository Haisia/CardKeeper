# 명함 관리 앱 (CardKeeper)

## What This Is

개인 사용을 위한 광고 없는 Android 명함 관리 앱. 카메라로 명함을 촬영하면 ML Kit OCR로 이름·회사명·전화번호 등을 자동 파싱하고, 명함 사진과 함께 로컬에 저장·관리한다. 태그 기반 분류, 빠른 검색, 연락처 내보내기 등 실사용에 필요한 편의 기능을 갖춘다.

## Core Value

카메라 한 번으로 명함을 디지털화하고, 즉시 찾아서 쓸 수 있어야 한다.

## Requirements

### Validated

(None yet — ship to validate)

### Active

- [ ] 카메라로 명함 촬영 → ML Kit OCR로 텍스트 추출
- [ ] 이름 / 회사명 / 직책 / 전화번호 / 이메일 / 주소 파싱 (파싱 결과 수동 수정 가능)
- [ ] 명함 사진 + 파싱 데이터를 하나의 카드로 저장
- [ ] 갤러리 이미지로도 명함 등록 가능
- [ ] 태그 기반 분류 (카드 하나에 복수 태그 부착 가능)
- [ ] 이름·회사·직책·태그로 실시간 검색 및 필터
- [ ] Android 기본 연락처 앱으로 파싱 데이터 내보내기
- [ ] 카드별 개인 메모 추가 가능
- [ ] 카드 목록 / 상세 보기 UI

### Out of Scope

- 클라우드 동기화 — 개인 사용 목적, 복잡도 증가 대비 필요성 낮음
- 광고·인앱 결제 — 제작 동기 자체가 광고 없는 앱
- 명함 공유/교환 기능 — v1 범위 초과, 향후 검토
- iOS 지원 — Android 전용

## Context

- 기존 명함 관리 앱들의 광고 문제를 피하기 위해 직접 제작
- OCR: Google ML Kit Text Recognition v2 (온디바이스, 무료, 인터넷 불필요)
- 저장: Room DB (로컬), 사진은 앱 내부 스토리지
- 언어: Kotlin, Jetpack Compose UI
- 최소 타겟: Android 8.0 (API 26) 이상

## Constraints

- **Tech Stack**: Kotlin + Jetpack Compose — Android 네이티브 표준
- **OCR**: ML Kit 온디바이스 — 외부 API 키 불필요, 비용 없음
- **Storage**: 로컬 전용 — 클라우드 연동 없음
- **Target**: 개인 사용 단일 기기 — 멀티유저/동기화 불필요

## Key Decisions

| Decision | Rationale | Outcome |
|----------|-----------|---------|
| ML Kit 온디바이스 OCR | 무료, 오프라인 동작, API 키 불필요 | — Pending |
| 태그 기반 분류 (폴더 아님) | 카드 하나가 여러 카테고리에 속할 수 있음 | — Pending |
| Room DB 로컬 저장 | 개인 사용 목적, 클라우드 불필요 | — Pending |
| Jetpack Compose UI | 최신 Android UI 표준, 빠른 개발 | — Pending |
| Hilt 2.59.2 사용 (2.57.1 계획) | AGP 9.x에서 2.57.1의 BaseExtension API 제거됨 | ✓ Good |
| Gradle 9.4.1 사용 (8.x 계획) | Java 25 환경 — Gradle 8.x는 Java 24까지만 지원 | ✓ Good |
| CardTagCrossRef에 tagId 인덱스 추가 | Room KSP 경고: 인덱스 없으면 풀 테이블 스캔 발생 | ✓ Good |

## Evolution

This document evolves at phase transitions and milestone boundaries.

**After each phase transition** (via `/gsd:transition`):
1. Requirements invalidated? → Move to Out of Scope with reason
2. Requirements validated? → Move to Validated with phase reference
3. New requirements emerged? → Add to Active
4. Decisions to log? → Add to Key Decisions
5. "What This Is" still accurate? → Update if drifted

**After each milestone** (via `/gsd:complete-milestone`):
1. Full review of all sections
2. Core Value check — still the right priority?
3. Audit Out of Scope — reasons still valid?
4. Update Context with current state

---
*Last updated: 2026-03-24 after Phase 1: Foundation complete*
