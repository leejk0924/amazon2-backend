# Subagent Architecture Design

**Date:** 2026-06-12  
**Project:** Amazon2 Backend (Spring Boot, Java 21)  
**Goal:** 도메인 기반 개발 생산성 향상을 위한 6개 특화 에이전트 시스템 구축

---

## 1. 개요

Amazon2는 **도메인 기반 아키텍처** (Member, Category, Posting)로 설계된 Spring Boot 프로젝트입니다. 
현재 **Harness Engineering 마이그레이션** 중이며, 향후 도메인 확장과 기능 추가가 빈번할 것으로 예상됩니다.

**목표:**
- 새 도메인 추가 시 반복적인 보일러플레이트 자동화
- 도메인별 단위/통합 테스트 자동 생성
- 도메인 간 아키텍처 규칙 검증 + Senior 리뷰어 역할
- API 문서(Swagger/OpenAPI) 자동 생성/관리
- Git Worktree를 활용한 **병렬 처리로 충돌 방지**

**사용 방식:**
- **수동 호출:** 사용자가 명시적으로 요청 (예: "새 도메인 추가해줘")
- **자동 제안:** 사용자 요청을 분석해 필요한 에이전트를 내가 제안 및 실행

---

## 2. 아키텍처 설계

### 2.1 전체 구조

```
┌──────────────────────────────────────────────────────────┐
│  USER REQUESTS (수동 호출 + 자동 제안)                    │
│  "새 도메인 추가" / "기능 구현" / "PR 올릴거야" 등...     │
└────────────────────┬─────────────────────────────────────┘
                     │
          ┌──────────┴──────────────┐
          │ 상황 판단 & 필요한      │
          │ 에이전트들 선택         │
          └──────────┬──────────────┘
                     │
┌────────────────────▼─────────────────────────────────────┐
│  🔧 SHARED FOUNDATION                                     │
│  harness-architecture-validator                           │
│  - 도메인 구조, 규칙, 의존성, 네이밍 정의               │
│  - 모든 에이전트가 import해서 재사용                      │
└────────────────────┬─────────────────────────────────────┘
                     │
      ┌──────────────┼──────────────┬───────────┬──────────┐
      │              │              │           │          │
  ┌──▼───┐  ┌─────┬──▼──┐  ┌──────▼───┐  ┌───▼─────┐  ┌──▼──────┐
  │Agent1│  │AGENT │ 2  │  │ Agent 3  │  │ Agent 4 │  │ Agent 5 │
  │SENIOR│  │TS   │    │  │  TEST    │  │  API    │  │CHECKER+ │
  │REVIEW│  │    │      │  │GENERATOR │  │DOCUM.   │  │Agent 6  │
  │ER    │  │─────┴───┘  │  │          │  │         │  │ANALYZER │
  └──────┘  │ DOMAIN GEN │  └──────────┘  └─────────┘  └─────────┘
            │ (Agent 2) │
            └───────────┘

각 에이전트는 독립 worktree에서 병렬 동작 ✅ 충돌 없음
```

### 2.2 설계 원칙

1. **공유 기반 (DRY):** 아키텍처 검증 로직은 한 곳에 집중
2. **병렬 처리:** Git Worktree로 독립적 실행
3. **점진적 도입:** Phase 1 → Phase 2 → Phase 3 순서로 구축
4. **자동 제안:** 사용자가 빠뜨려도 내가 필요한 에이전트 제안
5. **일관성 보장:** 모든 도메인이 동일한 패턴 유지

---

## 3. 공유 기반: `harness-architecture-validator`

### 3.1 목적

모든 에이전트가 사용할 Amazon2의 아키텍처 규칙을 정의하고 검증하는 공유 기반입니다.

### 3.2 정의 내용

**도메인 정보:**
```
domains:
  - member
  - category
  - posting
```

**도메인별 문서 구조:**
```
docs/domains/
├── member/
│   ├── README.md           도메인 개요, 주요 기능, 비즈니스 규칙
│   └── architecture.md     아키텍처, 주요 클래스, 의존성
├── category/
│   ├── README.md
│   └── architecture.md
└── posting/
    ├── README.md
    └── architecture.md

harnesses/ (개발 가이드):
├── README.md              전체 아키텍처
├── member/README.md       Member 도메인 개발 가이드
├── category/README.md     Category 도메인 개발 가이드
└── posting/README.md      Posting 도메인 개발 가이드
```

**계층 구조:**
```
[domain]/
├── entity/          JPA 엔티티
├── repository/      데이터 접근 (JpaRepository + QueryRepository)
├── service/         비즈니스 로직 (CommandService + QueryService)
├── dto/             Request/Response DTO
├── exception/       도메인 예외 (ErrorCode enum)
└── controller/      REST API
```

**패키지 네이밍 규칙:**
```
com.jk.amazon2.[domain].[layer]

예:
- com.jk.amazon2.member.entity
- com.jk.amazon2.member.repository
- com.jk.amazon2.member.service
- com.jk.amazon2.member.dto
- com.jk.amazon2.member.exception
- com.jk.amazon2.member.controller
```

**클래스 네이밍 규칙:**
```
Entity:        [Domain]               (예: Member, Category, Posting)
Repository:    [Domain]Repository     (예: MemberRepository)
               [Domain]QueryRepository
Service:       [Domain]CommandService (Create, Update, Delete)
               [Domain]QueryService   (Read)
DTO:           [Domain]CreateRequest
               [Domain]UpdateRequest
               [Domain]Response
Exception:     [Domain]Exception
ErrorCode:     [Domain]ErrorCode      (enum)
Controller:    [Domain]Controller
```

**의존성 규칙:**
```
✅ 허용:
- domain → common (공통 유틸, 상수, 예외 기반)
- domain → config (Spring 설정)
- service → repository (조회/저장)
- controller → service (비즈니스 로직)

❌ 불허:
- domain ↔ domain (직접 의존성 - 공통 레이어를 통해서만)
- controller → repository (중간에 service 필수)
- repository → service (순환 의존성)
```

**테스트 구조:**
```
src/test/java/com/jk/amazon2/[domain]/
├── entity/          [Domain]Test (핵심 도메인 entity 테스트)
├── controller/      [Domain]ControllerTest (통합 테스트, Testcontainers)
├── service/         [Domain]ServiceTest (단위 테스트)
└── repository/      [Domain]RepositoryTest (통합 테스트)
```

**Entity 테스트:** 핵심 도메인의 entity는 비즈니스 로직을 포함하므로 별도 테스트 필요
- 예: `MemberTest` - 회원 생성, 소프트 삭제, 상태 변화 등

### 3.3 제공 메서드

```
검증 함수:
- validatePackageStructure(domain) → bool
- validateDependencies(files: List<File>) → List<Violation>
- validateNamingConvention(className, context) → bool
- validateLayerOrder(classPath) → bool

조회 함수:
- getArchitectureRules() → Rules
- getExpectedPackages(domain) → List<Package>
- getExpectedClasses(domain, layer) → List<ClassName>
- checkConsistency(domains: List<Domain>) → List<Inconsistency>
```

---

## 4. 6개 특화 에이전트

### 4.1 Agent 1: Senior Code Reviewer

**목적:** PR/코드 변경 검토 - 아키텍처 규칙 + Best Practices 동등 비중

**책임:**
- ✅ 패키지 구조, 네이밍, 의존성 규칙 위반 감지
- ✅ Best Practices 피드백 (성능, 보안, 가독성)
- ✅ 시니어 개발자 관점의 설계 개선 제안
- ✅ 테스트 커버리지 검증

**자동 제안 트리거:**
```
- "PR 올릴거야"
- "코드 리뷰 해줄래"
- "이거 맞게 한거야?"
- 기능 구현 완료 후 (자동)
```

**Worktree 전략:** 읽기 전용 분석 (shared worktree 또는 read-only worktree)

**입력:** 
- diff (staging/unstaged changes) 또는 파일 목록
- 컨텍스트 (새 도메인? 기존 도메인 수정?)

**출력:**
```
[규칙 위반]
- L45: MemberCommandService에서 repository 직접 접근 (service 거쳐야 함)
- L102: member → category 직접 의존성 발견

[Best Practices]
- QueryDSL 쿼리가 fetch join 없음 (N+1 위험)
- Exception 처리 없는 Optional.get() 발견
- 하드코딩된 상수가 constant로 옮겨져야 함

[개선 제안]
- Member 조회 로직을 spec 기반 필터링으로 개선 가능
- Service 메서드가 너무 길어 보임 (분해 제안)
```

---

### 4.2 Agent 2: Domain Generator

**목적:** 새 도메인 추가 시 모든 보일러플레이트 자동 생성

**책능:**
- ✅ 도메인 패키지 구조 생성 (7개 계층)
- ✅ 기본 Entity 클래스 생성 (Lombok, JPA 애너테이션)
- ✅ Entity 테스트 클래스 생성 (핵심 비즈니스 로직 테스트)
- ✅ Repository 인터페이스 + QueryRepository 클래스 생성
- ✅ CommandService + QueryService 템플릿 생성
- ✅ Request/Response DTO 생성
- ✅ Exception 클래스 + ErrorCode enum 생성
- ✅ Controller 기본 구조 생성 (CRUD 엔드포인트)
- ✅ 테스트 클래스 스켈레톤 생성 (Controller, Service, Repository, Entity)
- ✅ 도메인 문서 생성 (`harnesses/[domain]/README.md`)
- ✅ CLAUDE.md 최상위 파일 관리 (200줄 초과 방지)

**자동 제안 트리거:**
```
- "새 도메인 추가해줘"
- "Booking 도메인 추가"
- "Member 같은 구조로 새 거 만들어줘"
```

**Worktree 전략:** 독립 worktree (새 파일 생성)

**입력:**
```
- 도메인명 (예: Booking)
- 엔티티 필드 (optional, 기본은 id/name/description 등)
- 관계 (optional, 예: Member와 N:1 관계)
```

**출력:**
```
생성 파일:
src/main/java/com/jk/amazon2/booking/
├── entity/Booking.java
├── repository/BookingRepository.java
├── repository/BookingQueryRepository.java
├── service/BookingCommandService.java
├── service/BookingQueryService.java
├── dto/BookingCreateRequest.java
├── dto/BookingUpdateRequest.java
├── dto/BookingResponse.java
├── exception/BookingException.java
├── exception/BookingErrorCode.java
└── controller/BookingController.java

src/test/java/com/jk/amazon2/booking/
├── controller/BookingControllerTest.java
├── service/BookingServiceTest.java
└── repository/BookingRepositoryTest.java

harnesses/booking/README.md

모든 파일이 Amazon2 규칙을 따름 ✅
```

---

### 4.3 Agent 3: Test Generator

**목적:** 도메인별 단위/통합 테스트 자동 생성

**책임:**
- ✅ Controller 통합 테스트 생성 (Testcontainers, MockMvc)
- ✅ Service 단위 테스트 생성 (Mockito)
- ✅ Repository 통합 테스트 생성 (Testcontainers)
- ✅ 테스트 케이스 커버리지 (Happy path + edge cases)
- ✅ Fixture/Test data 생성

**자동 제안 트리거:**
```
- "기능 구현해줘" (구현 후 자동)
- "테스트 작성해줘"
- "이 API 테스트 필요해"
```

**Worktree 전략:** 독립 worktree (테스트 파일 생성)

**입력:**
- 도메인명 또는 클래스명
- 구현된 메서드 목록
- 특별한 테스트 케이스 (optional)

**출력:**
```
테스트 파일:
src/test/java/com/jk/amazon2/[domain]/

Controller Test:
- GET /api/[domains] - 200 OK
- POST /api/[domains] - 201 Created
- GET /api/[domains]/{id} - 200 OK
- PUT /api/[domains]/{id} - 204 No Content
- DELETE /api/[domains]/{id} - 204 No Content
- 유효성 검증 실패 시 400 Bad Request
- 리소스 없음 시 404 Not Found

Service Test:
- CommandService CRUD 메서드별 테스트
- Exception handling 테스트
- 비즈니스 로직 edge cases

Repository Test:
- find*, save*, delete* 메서드 테스트
- QueryRepository 동적 쿼리 테스트
```

---

### 4.4 Agent 4: API Documenter

**목적:** Swagger/OpenAPI 스펙 자동 생성/업데이트

**책임:**
- ✅ Controller의 @RestMapping 분석해 OpenAPI 스펙 생성
- ✅ DTO를 기반으로 Request/Response Schema 자동 생성
- ✅ Exception/ErrorCode를 기반으로 에러 응답 문서화
- ✅ 기존 Swagger UI와 동기화
- ✅ API 버전 관리 (선택사항)

**자동 제안 트리거:**
```
- "API 추가했어"
- "문서화 해줘"
- Controller 변경 시 (자동)
```

**Worktree 전략:** 독립 worktree (Swagger 설정/YAML 파일 수정)

**입력:**
- 도메인명 또는 Controller 클래스
- 변경사항 (신규 엔드포인트, 수정된 요청/응답)

**출력:**
```
업데이트 파일:
src/main/resources/openapi/[domain]-api.yaml (또는 주석 기반 생성)

예:
/api/members:
  get:
    summary: 회원 목록 조회
    tags:
      - Members
    responses:
      '200':
        description: 회원 목록
        content:
          application/json:
            schema:
              type: array
              items:
                $ref: '#/components/schemas/MemberResponse'

components:
  schemas:
    MemberResponse:
      type: object
      properties:
        id: { type: integer }
        name: { type: string }
        email: { type: string }
        deleteStatus: { type: string, enum: [ACTIVE, SOFT_DELETED] }
```

---

### 4.5 Agent 5: Harness Consistency Checker

**목적:** 도메인 간 구조/패턴/네이밍 일관성 검증

**책임:**
- ✅ 모든 도메인이 동일한 패킹 구조를 따르는지 검증
- ✅ 클래스 네이밍 일관성 확인 (CommandService, QueryService 등)
- ✅ 어노테이션 사용 일관성 (@Entity, @Service, @Repository)
- ✅ 예외 처리 패턴 일관성
- ✅ DTO 구조 일관성 (Request/Response)

**자동 제안 트리거:**
```
- 새 도메인 추가 후 (자동)
- 기존 도메인 수정 후 (자동)
- "일관성 체크해줘"
```

**Worktree 전략:** 읽기 전용 분석 (모든 도메인 검사)

**출력:**
```
[일관성 검증 결과]

✅ Member, Category, Posting
  - 패키지 구조 동일
  - 네이밍 규칙 준수
  - 계층 순서 일치

⚠️ 주의사항
  - Posting의 Exception 패턴이 다름 (ErrorCode가 없음)
  - Category Service에 @Service 애너테이션 누락

❌ 위반 사항
  - Member DTO의 deleteStatus는 enum, Category는 String
  → Category도 enum으로 통일 필요
```

---

### 4.6 Agent 6: Dependency Analyzer

**목적:** 도메인 간 의존성 분석 - 순환 의존성, 과도한 커플링 감지

**책임:**
- ✅ 도메인 간 순환 의존성 감지 (A → B → A)
- ✅ 과도한 크로스 도메인 의존성 감지
- ✅ Common 레이어 의존성 검증
- ✅ 불허된 계층 간 의존성 감지 (controller → repository 등)
- ✅ 의존성 그래프 시각화

**자동 제안 트리거:**
```
- 새 기능 추가 후 (자동)
- "의존성 분석해줘"
- PR 검토 시 (자동)
```

**Worktree 전략:** 읽기 전용 분석 (import 문 검사)

**출력:**
```
[의존성 분석 결과]

✅ 정상
  - Member ↔ Repository ↔ Service → Controller (계층 순서 OK)
  - Member → common.exception (허용된 외부 의존성)

⚠️ 주의
  - Category.Controller에서 Member.Service import 1회 발견
  → Common DTO 추가해서 분리 권장

❌ 위반
  - Posting.Service에서 Member.Repository 직접 import
  → Member.QueryService 거쳐야 함

의존성 그래프:
member/service → member/repository ✅
posting/service → posting/repository ✅
posting/service → member/service ✅
posting/service ↔ posting/repository ❌ (순환 위험 감지)
```

---

## 5. Git Worktree 병렬 처리

### 5.1 동작 메커니즘

```
Main Branch (main) - 불변
│
├─ worktree-1: Senior Code Reviewer
│  └─ 읽기 전용 분석 (기존 코드 검토)
│
├─ worktree-2: Domain Generator
│  └─ 새 파일 생성 → commit
│
├─ worktree-3: Test Generator
│  └─ 테스트 파일 생성 → commit
│
├─ worktree-4: API Documenter
│  └─ 문서 수정 → commit
│
├─ worktree-5: Consistency Checker
│  └─ 읽기 전용 분석
│
└─ worktree-6: Dependency Analyzer
   └─ 읽기 전용 분석

[모두 완료]
↓
결과 병합 (squash/rebase) → PR 생성 또는 직접 merge
```

### 5.2 Worktree 생성 전략

**Type A: 쓰기 권한이 필요한 에이전트 (Agent 2, 3, 4)**
```bash
git worktree add worktree-[agent-name] -b [feature-branch]

예:
git worktree add worktree-domain-generator -b feat/new-domain
git worktree add worktree-test-generator -b feat/tests
git worktree add worktree-api-documenter -b feat/api-docs
```

각 worktree는 독립적인 브랜치에서 동작하므로 충돌 없음.

**Type B: 읽기 전용 에이전트 (Agent 1, 5, 6)**
```bash
# Option 1: Shared worktree (최신 코드 기반)
# 분석 결과는 보고서로만 출력 (파일 수정 없음)

# Option 2: 별도 worktree (필요 시)
git worktree add worktree-reviewer
```

### 5.3 결과 병합 전략

```
Step 1: 각 worktree의 commit 확인
git log worktree-domain-generator -1
git log worktree-test-generator -1
git log worktree-api-documenter -1

Step 2: Main branch로 돌아가기
git checkout main
git pull origin main

Step 3: 각 worktree의 변경사항을 main에 병합
git merge worktree-domain-generator
git merge worktree-test-generator
git merge worktree-api-documenter

또는 Squash merge (히스토리 깔끔):
git merge --squash worktree-domain-generator
git commit -m "Feat: 새 도메인 추가 + 테스트 + 문서화"

Step 4: Worktree 정리
git worktree remove worktree-domain-generator
git worktree remove worktree-test-generator
git worktree remove worktree-api-documenter
git branch -d feat/new-domain feat/tests feat/api-docs
```

### 5.4 충돌 처리

**충돌 시나리오:**
- Agent 2, 3, 4가 동일 파일을 수정하면? (보통 발생 안 함)
  - Agent 2 (Domain Generator): `src/main/java/...` 생성
  - Agent 3 (Test Generator): `src/test/java/...` 생성
  - Agent 4 (API Documenter): `src/main/resources/...` 수정
  → 다른 디렉토리이므로 충돌 안 남

**만약 충돌 발생:**
```bash
# 수동 병합
git merge worktree-domain-generator
# conflict 발생 시
git status  # 충돌 파일 확인
# 파일 수정해서 해결
git add [파일]
git commit -m "Merge: conflict 해결"
```

---

## 6. 워크플로우 예시

### 6.1 새 도메인 추가

**사용자 요청:**
```
"Booking 도메인 추가해줘. Member와 N:1 관계야."
```

**내 응답 및 실행:**
```
다음 에이전트들을 병렬로 실행합니다:

[병렬 실행 - 동시 진행]
┌─ worktree-1: Senior Code Reviewer
│  └─ 기존 Member 코드 검토 (best practices 확인)
│
├─ worktree-2: Domain Generator
│  └─ Booking 도메인 생성 (entity, service, repository, dto 등)
│
├─ worktree-3: Test Generator
│  └─ Booking 테스트 생성 (Controller, Service, Repository test)
│
├─ worktree-4: API Documenter
│  └─ Booking API Swagger 문서화
│
├─ worktree-5: Consistency Checker
│  └─ Booking이 Member/Category/Posting과 일관성 검증
│
└─ worktree-6: Dependency Analyzer
   └─ Booking의 Member 의존성 검증 (N:1 관계 올바른지)

[완료 후]
✅ 6개 worktree 병합
✅ 최종 검증 (빌드, 테스트)
✅ feature/new-booking 브랜치 생성
✅ PR 준비 완료
```

### 6.2 기존 도메인에 기능 추가

**사용자 요청:**
```
"Member API에 소프트 삭제 기능 추가해줘."
```

**내 응답 및 실행:**
```
다음 에이전트들을 병렬로 실행합니다:

[병렬 실행]
┌─ worktree-1: Senior Code Reviewer
│  └─ 소프트 삭제 구현 검토 (deleteStatus enum 사용 검증)
│
├─ worktree-3: Test Generator
│  └─ 소프트 삭제 테스트 생성
│
├─ worktree-4: API Documenter
│  └─ DELETE /api/members/{id} API 문서화
│
└─ worktree-5: Consistency Checker
   └─ Category/Posting도 같은 패턴인지 검증

[완료 후]
✅ 모든 worktree 병합
✅ 전체 테스트 패스
✅ feature/member-soft-delete 브랜치 생성
```

### 6.3 코드 리뷰 요청

**사용자 요청:**
```
"PR 올릴건데 코드 리뷰 해줄래?"
```

**내 응답:**
```
Senior Code Reviewer를 실행합니다:

[검토 결과]

✅ 아키텍처 규칙
- 패키지 구조: OK
- 의존성: OK
- 네이밍: OK

⚠️ Best Practices
- QueryDSL에서 fetch join 누락 (N+1 위험)
- Exception을 일반 RuntimeException으로 래핑 (MemberException 사용)
- 하드코딩된 "ACTIVE" → DeleteStatus.ACTIVE 사용

[개선 제안]
- Service 메서드가 70줄 → 메서드 추출 권장
- DTO validation이 없음 → @Valid 애너테이션 추가
```

---

## 7. 구현 계획

### Phase 1: 즉시 필요 (우선순위 1-4)

| # | Agent | 상태 | 예상 기간 |
|---|-------|------|----------|
| 1 | Senior Code Reviewer | TODO | 1-2주 |
| 2 | Domain Generator | TODO | 1-2주 |
| 3 | Test Generator | TODO | 2주 |
| 4 | API Documenter | TODO | 1주 |

**Phase 1 목표:**
- 새 도메인 추가 자동화 (Domain Generator + Test Generator)
- PR 리뷰 자동화 (Senior Code Reviewer)
- API 문서화 자동화 (API Documenter)

### Phase 2: 함께 필요 (우선순위 5-6)

| # | Agent | 상태 | 예상 기간 |
|---|-------|------|----------|
| 5 | Harness Consistency Checker | TODO | 1주 |
| 6 | Dependency Analyzer | TODO | 1-2주 |

**Phase 2 목표:**
- 도메인 간 일관성 자동 검증
- 의존성 문제 자동 감지

### Phase 3: 향후 추가 고려

- Performance & Security Auditor
- Documentation Gap Detector
- CI/CD Integration

---

## 8. 성공 기준

### 8.1 Phase 1 완료 시

- [ ] Agent 1-4 구현 및 테스트 완료
- [ ] 새 도메인 추가 시 보일러플레이트 100% 자동화
- [ ] PR 리뷰 자동화로 수동 검토 시간 50% 감소
- [ ] API 문서 자동 생성 및 동기화
- [ ] Git Worktree 병렬 처리 충돌 0건

### 8.2 Phase 2 완료 시

- [ ] Agent 5-6 구현 및 테스트 완료
- [ ] 도메인 간 일관성 자동 검증
- [ ] 순환 의존성 자동 감지
- [ ] 전체 개발 생산성 30% 향상

---

## 9. 위험 요소 및 완화책

| 위험 | 영향 | 완화책 |
|------|------|--------|
| Worktree 충돌 | 병렬 처리 중 코드 손실 | 독립 브랜치 + 충돌 감지 로직 |
| 에이전트 간 순서 의존성 | 병렬 실행 불가 | 각 에이전트 독립적 설계 |
| 아키텍처 변경 시 업데이트 | 모든 에이전트 수정 필요 | harness-architecture-validator 중앙화 |
| 에이전트 오류 | 잘못된 코드 생성 | 항상 사용자 검토 후 merge |

---

## 10. 에러 관리 패턴 (Error Registry + Memory)

### 10.1 개요

Harness Engineering 진행 중 발생하는 에러를 체계적으로 기록하고 관리하여 **다음에 동일한 에러가 반복되지 않도록** 합니다.

### 10.2 디렉터리 구조

```
.claude/
├── errors/                     에러 레지스트리 (프로젝트별 에러 기록)
│   ├── ERRORS.md              에러 목록 및 해결책
│   └── ERROR_LOG.md           에러 발생 타임라인
└── memory/                     에러 및 학습 메모리 (재사용 가능한 인사이트)
    ├── MEMORY.md              메모리 인덱스
    └── error_patterns/        재발 방지 패턴
        ├── package_naming_errors.md
        ├── dependency_errors.md
        ├── test_errors.md
        └── ...
```

### 10.3 에러 기록 형식

**.claude/errors/ERRORS.md**
```markdown
# Error Registry

## Error ID: E001_INVALID_PACKAGE_STRUCTURE

**Description:** 도메인 패키지가 규칙을 따르지 않음  
**First Occurred:** 2026-06-12 (Member 마이그레이션 중)  
**Severity:** High  
**Root Cause:** Domain Generator가 잘못된 package name 생성

**Symptoms:**
- `com.jk.amazon2.member.services.MemberService` (❌ 잘못됨)
- 정확해야 할 것: `com.jk.amazon2.member.service.MemberCommandService`

**Solution:**
1. Domain Generator의 package naming 로직 수정
2. harness-architecture-validator에서 검증 강화
3. Consistency Checker로 사후 검증

**Status:** RESOLVED ✅  
**Resolution Date:** 2026-06-12  
**Prevention:** Agent 2 (Domain Generator) 검증 강화

---
```

### 10.4 메모리 활용

에러 메모리는 프로젝트의 auto-memory 시스템에 저장:

```markdown
# .claude/memory/error_patterns/package_naming_errors.md

---
name: package-naming-validation
description: 패키지 네이밍 규칙 위반 방지 — Domain Generator 및 Consistency Checker에서 검증
metadata:
  type: feedback
---

**규칙:** 패키지명은 항상 `com.jk.amazon2.[domain].[layer]` 형식

**Why:** 잘못된 패키지명은 IDE 자동 임포트 실패, 테스트 검색 누락, 문서 생성 오류 야기

**How to apply:** 
- Domain Generator: 생성 직후 검증
- Consistency Checker: 모든 도메인 주기적 검증
- Senior Code Reviewer: PR에서 감지
```

### 10.5 에러 관리 워크플로우

```
에러 발생
    ↓
1. .claude/errors/ERRORS.md에 기록
   - Error ID (E001, E002, ...)
   - 원인, 증상, 해결책
   ↓
2. 메모리 저장 (.claude/memory/error_patterns/)
   - 재발 방지를 위한 학습 내용
   - 어떤 에이전트가 검증할지 명시
   ↓
3. 해당 에이전트 개선
   - Domain Generator: 생성 로직 수정
   - Consistency Checker: 검증 규칙 추가
   - Senior Code Reviewer: 리뷰 항목 추가
   ↓
4. 테스트 추가
   - 동일 에러 재발 방지 테스트
   ↓
5. Status 업데이트
   - ERRORS.md에서 Status를 RESOLVED로 변경
   - .claude/memory에서 메모리 확인 가능
```

### 10.6 자동 제안

내가 (Claude) 에러를 감지하면 자동으로 제안:

```
사용자: "새 도메인 추가해줘"

나: 다음을 확인합니다:
  1. .claude/errors/ERRORS.md (과거 에러)
  2. .claude/memory/error_patterns/ (재발 방지 패턴)
  
  만약 과거에 패키지 네이밍 에러가 있었다면:
  "이전에 E001_INVALID_PACKAGE_STRUCTURE 에러가 있었습니다.
   Domain Generator에서 다시 발생하지 않도록 주의하겠습니다."
```

---

## 11. 참고 자료

- **프로젝트 CLAUDE.md:** [CLAUDE.md](../../CLAUDE.md)
- **Harnesses 가이드:** [harnesses/README.md](../../harnesses/README.md)
- **Member 가이드:** [harnesses/member/README.md](../../harnesses/member/README.md)
- **Harness Engineering 마이그레이션:** [2026-06-12-harness-engineering-migration.md](./2026-06-12-harness-engineering-migration.md)

---

**설계 작성자:** Claude Code  
**검토 예정:** 사용자 (정현권)  
**최종 승인 후:** writing-plans 스킬로 구현 계획 수립
