# Requirements: CardKeeper (명함 관리 앱)

**Defined:** 2026-03-24
**Core Value:** 카메라 한 번으로 명함을 디지털화하고, 즉시 찾아서 쓸 수 있어야 한다.

## v1 Requirements

### Scan & OCR

- [ ] **SCAN-01**: 카메라로 명함을 실시간 프리뷰 화면에서 촬영할 수 있다
- [ ] **SCAN-02**: 갤러리 이미지를 선택해 명함으로 등록할 수 있다
- [ ] **SCAN-03**: ML Kit (온디바이스, 한국어+영문 번들 모델)로 텍스트를 추출한다
- [ ] **SCAN-04**: 추출 텍스트에서 이름 / 회사명 / 직책 / 전화번호 / 이메일 / 주소를 파싱한다
- [ ] **SCAN-05**: OCR 결과를 저장 전에 수동으로 수정할 수 있는 편집 화면이 제공된다

### Card Storage

- [ ] **CARD-01**: 명함 사진과 파싱 데이터를 하나의 카드로 로컬에 저장한다
- [ ] **CARD-02**: 저장된 카드를 수정할 수 있다 (SCAN-05와 동일 폼 재사용)
- [ ] **CARD-03**: 저장된 카드를 삭제할 수 있다 (확인 다이얼로그 포함)
- [ ] **CARD-04**: 카드마다 개인 메모를 추가/수정할 수 있다

### Browse & Search

- [ ] **BROWSE-01**: 저장된 명함을 썸네일+이름+회사명 목록으로 볼 수 있다
- [ ] **BROWSE-02**: 명함 하나를 선택해 전체 정보(사진, 파싱 데이터, 태그, 메모)를 상세 보기할 수 있다
- [ ] **BROWSE-03**: 이름·회사명·직책으로 실시간 검색(타이핑 즉시 필터)할 수 있다

### Tags

- [ ] **TAG-01**: 태그를 생성·삭제·관리할 수 있다
- [ ] **TAG-02**: 하나의 명함에 복수 태그를 부착할 수 있다
- [ ] **TAG-03**: 태그로 명함 목록을 필터링할 수 있다 (OR 방식)

### Export

- [ ] **EXPORT-01**: 명함의 파싱 데이터를 Android 기본 연락처 앱으로 내보낼 수 있다 (Intent 방식, 별도 권한 불필요)

## v2 Requirements

### Enhanced Organization

- **ORG-01**: 정렬 옵션 (이름순 / 회사순 / 등록일순)
- **ORG-02**: 중복 명함 감지 및 병합 제안

### Batch Operations

- **BATCH-01**: 갤러리에서 여러 명함 사진 일괄 등록

### Extended Features

- **EXT-01**: 홈 스크린 위젯 (최근 명함)
- **EXT-02**: 명함 데이터 백업/복원 (로컬 파일 내보내기)

## Out of Scope

| Feature | Reason |
|---------|--------|
| 클라우드 동기화 | 개인 사용 목적, 복잡도 대비 필요성 낮음 |
| 광고 / 인앱 결제 | 제작 동기 자체가 광고 없는 앱 |
| 명함 공유 / NFC 교환 | 소셜 기능 — v1 범위 초과 |
| iOS 지원 | Android 네이티브 전용 (Kotlin + Compose) |
| 폴더 계층 구조 | 태그 기반 분류로 충분, 오히려 복잡도만 증가 |
| 중복 감지 | 단일 사용자 수동 관리 가능, v1 범위 초과 |
| CRM / LinkedIn 연동 | API 키·OAuth 필요, 개인 사용에 불필요 |
| AI 팔로업 리마인더 | 메모 기능으로 대체 가능, v1 범위 초과 |

## Traceability

| Requirement | Phase | Status |
|-------------|-------|--------|
| SCAN-01 | Phase 2 | Pending |
| SCAN-02 | Phase 2 | Pending |
| SCAN-03 | Phase 2 | Pending |
| SCAN-04 | Phase 2 | Pending |
| SCAN-05 | Phase 2 | Pending |
| CARD-01 | Phase 2 | Pending |
| CARD-02 | Phase 3 | Pending |
| CARD-03 | Phase 3 | Pending |
| CARD-04 | Phase 3 | Pending |
| BROWSE-01 | Phase 3 | Pending |
| BROWSE-02 | Phase 3 | Pending |
| BROWSE-03 | Phase 3 | Pending |
| TAG-01 | Phase 3 | Pending |
| TAG-02 | Phase 3 | Pending |
| TAG-03 | Phase 3 | Pending |
| EXPORT-01 | Phase 4 | Pending |

**Coverage:**
- v1 requirements: 16 total
- Mapped to phases: 16
- Unmapped: 0 ✓

---
*Requirements defined: 2026-03-24*
*Last updated: 2026-03-24 after initial definition*
