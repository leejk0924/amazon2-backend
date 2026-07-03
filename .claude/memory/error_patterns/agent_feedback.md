# 에러 패턴 메모리 시스템 (Agent Feedback)

에이전트 실행 결과, 에러 감지, 수정 사항 및 학습 내용을 기록하는 메모리 시스템입니다.

## 목적

1. 각 에이전트 실행 이력 추적
2. 발견된 에러 패턴 분석
3. 반복되는 문제 조기 감지
4. 프로젝트 아키텍처 개선

## 기록 구조

### 세션 기록 (Session Record)

```markdown
## [날짜] [에이전트명] - [도메인명]

**상태**: 완료 / 진행중 / 실패

**입력 파라미터**:
- domain_name: product
- check_type: all
- auto_fix: false

**발견 사항**:
- [심각도] [에러코드]: 설명
- [심각도] [에러코드]: 설명

**수정 사항**:
- [파일명]: [변경 내용]
- [파일명]: [변경 내용]

**학습 내용**:
- 패턴 설명
- 원인 분석
- 재발 방지 방법

**다음 단계**:
- [ ] 수정 사항 검증
- [ ] 테스트 실행
- [ ] PR 생성
```

---

## 기록된 세션

### 초기 설정

**상태**: 준비 완료

**설명**: 에러 관리 시스템 초기 생성
- ERRORS.md: 16개 에러 코드 정의
- ERROR_PATTERNS.md: 3가지 주요 패턴 정의
- agent_feedback.md: 메모리 시스템 구축

**에러 코드 범주**:
- E001-E099: 아키텍처 (4개)
- E100-E199: 네이밍 (3개)
- E200-E299: Validation (2개)
- E300-E399: 데이터베이스 (3개)
- E400-E499: 비즈니스 (2개)
- E500-E599: 설정 (2개)

---

## 세션 로그

> domain-generator, harness-consistency-checker, dependency-analyzer가 실행될 때마다 위 "기록 구조" 템플릿 형식으로 이 섹션 아래에 실제 세션이 누적됩니다.

### 2026-07-04 dependency-analyzer - member/category/posting

**상태**: 완료

**입력 파라미터**:
- analysis_type: comprehensive (circular + forbidden + graph)
- include_transitive: true (circular 검사), false (forbidden 검사)
- output_format: text + mermaid
- module_scope: member, category, posting (common/config는 참고용으로 포함)

**발견 사항**:
- [INFO] CIRCULAR: 순환 의존성 없음 — member/category/posting/common import 그래프 전수 조사, DFS 기준 cycle 미검출
- [ERROR] E003: `posting.service.PostingService`, `StatisticsService`, `BatchService`, `posting.dto.PostingResponse` 4개 파일이 `member.entity.Member` / `member.repository.MemberRepository`를 직접 import. posting→member 방향 자체는 허용되지만, Port/Adapter나 DTO 경유 없이 타 도메인 Repository·Entity를 직접 사용해 "cross-domain은 정의된 API로만" 원칙 위반
- [WARNING] `member.service.MemberService` → `common.port.CategoryValidationPort`(구현체는 `category.adapter.CategoryValidationAdapter`) 의존. DIP로 컴파일 타임 순환/직접 import는 회피했지만, Member(상위 계층)가 Category(중간 계층) 존재 검증에 런타임으로 의존 — 문서화된 계층 규칙(member→category 금지)과 방향이 반대. `member.categoryCode` 컬럼 무결성 검증 목적으로 의도된 설계로 보이나 팀 확인 필요
- [INFO] category 도메인은 현재 posting으로부터 전혀 참조되지 않음 (harnesses 상 posting→category는 허용되어 있으나 미구현/미사용 상태)
- [INFO] `duplicatetest123/entity/Marker.java` — domain-generator 중복 확인 테스트용 더미 파일, 실제 도메인과 무관한 잔재

**수정 사항**: 이번 세션은 분석 전용 (코드 수정 없음)

**학습 내용**:
- category 도메인은 `CategoryValidationPort`(common) + `CategoryValidationAdapter`(category.adapter) 패턴으로 cross-domain 검증을 노출 — 프로젝트 내 "권장 cross-domain 연동 템플릿"으로 삼을 만함
- posting은 동일 패턴을 따르지 않고 `MemberRepository`/`Member` 엔티티를 3개 서비스 클래스에서 직접 사용 중 — 향후 `MemberQueryPort`/`MemberQueryAdapter`로 통일 권장 (동일 근본 원인이 3개 파일에 반복 — 다음 세션에서도 재발 시 "발견된 반복 패턴" 섹션으로 승격 검토)
- Posting 엔티티들은 `BaseAudit`만 상속하고 Member/Category에 대한 JPA 연관관계(`@ManyToOne` 등)를 두지 않음 (ID만 저장) — 엔티티 레벨 결합은 없고 서비스 레벨 결합만 존재

**다음 단계**:
- [ ] `MemberQueryPort`/`MemberQueryAdapter` 도입 후 PostingService/StatisticsService/BatchService 리팩토링 여부 논의
- [ ] member→category(`CategoryValidationPort`) 의존 방향이 의도된 설계인지 팀 확인 및 harnesses/README에 명시
- [ ] `duplicatetest123` 디렉토리 삭제 여부 확인
- [ ] posting→category 연동 필요 시 동일 Port/Adapter 패턴 적용

---

## 분석 대시보드

### 에러 분포

| 심각도 | 개수 | 비율 | 담당 에이전트 |
|-------|------|------|-------------|
| ERROR | 8개 | 50% | Consistency Checker, Dependency Analyzer |
| WARNING | 5개 | 31% | Consistency Checker |
| INFO | 3개 | 19% | Consistency Checker |

### 패턴별 빈도

| 패턴 | 에러코드 | 발생빈도 | 심각도 |
|------|---------|---------|-------|
| 패키지 네이밍 | E100-E102 | 높음 | WARNING |
| 크로스 도메인 | E003, E008 | 중간 | ERROR |
| 순환 의존성 | E004 | 낮음 | ERROR |
| 아키텍처 계층 | E001, E002 | 높음 | ERROR |
| Entity 설계 | E300-E302 | 중간 | ERROR |

---

## 에이전트별 역할 정리

### 1. Domain Generator
**목적**: 새 도메인 자동 생성
- 생성 내용: 패키지 구조, Entity, DTO, Service, Controller, Repository
- 예방 에러: E100-E102 (패키지 네이밍)
- 권장 사용: 새 도메인 추가시

### 2. Test Generator
**목적**: 테스트 코드 자동 생성
- 생성 대상: Controller, Service, Repository, Entity 테스트
- 관련 에러: 테스트 누락 (직접 코드 에러는 아님)
- 권장 사용: 도메인 구현 후

### 3. API Documenter
**목적**: API 문서 자동 생성
- 생성 내용: Swagger/OpenAPI 어노테이션, YAML
- 관련 에러: 없음 (문서화 목적)
- 권장 사용: API 개발 완료후

### 4. Consistency Checker
**목적**: 일관성 검증
- 검증 대상: 패키지, 네이밍, 어노테이션, DTO, Exception
- 감지 에러: E001-E102, E300-E302, E400-E401
- 권장 사용: 정기적 검증 (PR 전 등)

### 5. Dependency Analyzer
**목적**: 의존성 분석
- 검증 대상: 순환 의존성, 금지 의존성, 크로스 도메인
- 감지 에러: E003, E004, E008
- 권장 사용: 아키텍처 리뷰, 대규모 리팩토링

---

## 사용 시나리오

### 시나리오 1: 새 도메인 추가

```
1. Domain Generator 실행
   - domain_name: newdomain
   - create_dto: true
   - create_exception: true
   - create_enum: false

2. Consistency Checker 실행 (검증)
   - domain_name: newdomain
   - check_type: all
   - auto_fix: true

3. Test Generator 실행
   - domain_name: newdomain
   - test_type: all

4. API Documenter 실행
   - domain_name: newdomain
   - documentation_type: both

5. Dependency Analyzer 실행 (최종 검증)
   - analysis_type: all
```

### 시나리오 2: 기존 도메인 리팩토링

```
1. Consistency Checker 실행 (현상 파악)
   - domain_name: product
   - check_type: all
   - report_format: detailed

2. 이슈 분류 및 수정

3. Consistency Checker 재실행 (검증)
   - auto_fix: true

4. Dependency Analyzer 실행 (아키텍처 검증)
   - analysis_type: all
```

### 시나리오 3: 정기 아키텍처 감사

```
1. Dependency Analyzer 전체 실행
   - analysis_type: all
   - output_format: json

2. 순환 의존성 감지 및 분석

3. Cross-domain 의존성 검토

4. 개선 계획 수립 및 실행
```

---

## 개선 사항 추적

### 발견된 반복 패턴

> 위 "세션 로그"에 같은 유형의 이슈(같은 에러코드 또는 같은 근본 원인)가 3회 이상 누적되면, 아래에 패턴/근본 원인/해결책을 추가합니다.

(아직 반복 패턴 없음)

---

## 학습 기록

### 에러 패턴 학습

1. **패키지 네이밍 패턴**
   - 학습: 명확한 서브패키지 구조의 중요성
   - 개선: 자동화로 90% 이상의 오류 사전 방지

2. **크로스 도메인 의존성 패턴**
   - 학습: 도메인 계층을 명확히 정의해야 함
   - 개선: Event-driven 아키텍처 선택

3. **순환 의존성 패턴**
   - 학습: 빌드 타임에 감지 불가능한 경우 존재
   - 개선: 정기적인 Dependency Analyzer 실행

---

## 향후 개선 계획

### 단기 (1-2주)

- [ ] 모든 기존 도메인에 Consistency Checker 실행
- [ ] 발견된 이슈 수정
- [ ] member, category 도메인 검증 완료

### 중기 (1개월)

- [ ] Dependency Analyzer로 전체 아키텍처 평가
- [ ] Cross-domain 의존성 최적화
- [ ] Test Generator로 테스트 커버리지 80% 이상 달성

### 장기 (3개월+)

- [ ] CI/CD에 Consistency Checker 통합
- [ ] 정기적 아키텍처 감사 자동화
- [ ] 에이전트 기반 개발 워크플로우 정립

---

## 레퍼런스

- 에러 정의: `.claude/errors/ERRORS.md`
- 패턴 분석: `.claude/errors/ERROR_PATTERNS.md`
- 에이전트 목록: `.claude/agents/`
  - domain-generator.md
  - test-generator.md
  - api-doc-generator.md
  - harness-consistency-checker.md
  - dependency-analyzer.md
  - senior-code-reviewer.md

---

**마지막 업데이트**: 2026-07-04 — 가짜 예시 데이터 제거, 깨진 파일 링크 수정, 실제 세션 기록이 쌓이도록 3개 에이전트(domain-generator, harness-consistency-checker, dependency-analyzer)에 기록 지시 연결
