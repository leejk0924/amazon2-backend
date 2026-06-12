# 6개 Subagent 구현 로드맵

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Amazon2 프로젝트의 도메인 기반 개발 생산성 향상을 위해 6개 특화 subagent를 단계적으로 구현하고, 공유 기반을 통해 일관성을 보장합니다.

**Architecture:** 
- **Shared Foundation (harness-architecture-validator)**: 모든 에이전트가 사용할 아키텍처 규칙 정의
- **Phase 1** (1-2주): 4개 핵심 에이전트 (Code Reviewer, Domain Generator, Test Generator, API Documenter)
- **Phase 2** (1주): 2개 검증 에이전트 (Consistency Checker, Dependency Analyzer)
- **Error Registry**: `.claude/errors/` 에서 중앙화된 에러 관리
- **Git Worktree**: 각 에이전트는 독립 worktree에서 병렬 동작

**Tech Stack:** 
- Language: YAML (프롬프트), Markdown (문서)
- Tools: Claude API, Git Worktree, Regex (검증)
- Framework: Claude Code Agents/Skills

---

## File Structure

### Phase 1 & 2 공통 구조

```
.claude/
├── agents/
│   ├── shared-foundation.yaml              # 공유 기반 (아키텍처 검증 규칙)
│   ├── senior-code-reviewer.yaml           # Agent 1
│   ├── senior-code-reviewer-prompt.md      # Agent 1 프롬프트
│   ├── domain-generator.yaml               # Agent 2
│   ├── domain-generator-prompt.md          # Agent 2 프롬프트
│   ├── test-generator.yaml                 # Agent 3
│   ├── test-generator-prompt.md            # Agent 3 프롬프트
│   ├── api-documenter.yaml                 # Agent 4
│   ├── api-documenter-prompt.md            # Agent 4 프롬프트
│   ├── consistency-checker.yaml            # Agent 5
│   ├── consistency-checker-prompt.md       # Agent 5 프롬프트
│   ├── dependency-analyzer.yaml            # Agent 6
│   └── dependency-analyzer-prompt.md       # Agent 6 프롬프트
├── errors/
│   ├── ERRORS.md                           # 에러 레지스트리
│   └── ERROR_PATTERNS.md                   # 재발 방지 패턴
└── memory/
    └── error_patterns/
        └── agent_feedback.md               # 에이전트 피드백 저장
```

---

## Phase 1: 핵심 4개 에이전트 + 공유 기반

### Task 1: Shared Foundation (harness-architecture-validator) 구현

**Files:**
- Create: `.claude/agents/shared-foundation.yaml`
- Create: `docs/architecture/VALIDATION_RULES.md`

#### Step 1: 아키텍처 검증 규칙 정의 파일 작성

- [ ] **Create `.claude/agents/shared-foundation.yaml`**

```yaml
# Shared Foundation: Amazon2 Harness Architecture Validator

name: harness-architecture-validator
version: 1.0.0

description: |
  모든 에이전트가 사용할 Amazon2의 도메인 기반 아키텍처 검증 규칙 정의.
  패키지 구조, 의존성, 네이밍 규칙을 중앙화하여 일관성 보장.

# 도메인 정의
domains:
  - member
  - category
  - posting

# 계층 구조 (각 도메인 내)
layers:
  entity:
    path: "src/main/java/com/jk/amazon2/{domain}/entity"
    responsibility: "JPA 엔티티, 비즈니스 로직"
    naming_pattern: "{Domain}"
    
  repository:
    path: "src/main/java/com/jk/amazon2/{domain}/repository"
    responsibility: "데이터 접근 (JpaRepository + QueryRepository)"
    naming_pattern: "{Domain}Repository|{Domain}QueryRepository"
    
  service:
    path: "src/main/java/com/jk/amazon2/{domain}/service"
    responsibility: "비즈니스 로직 (Command/Query)"
    naming_pattern: "{Domain}CommandService|{Domain}QueryService"
    
  dto:
    path: "src/main/java/com/jk/amazon2/{domain}/dto"
    responsibility: "Request/Response DTO"
    naming_pattern: "{Domain}(CreateRequest|UpdateRequest|Response)"
    
  exception:
    path: "src/main/java/com/jk/amazon2/{domain}/exception"
    responsibility: "도메인 예외"
    naming_pattern: "{Domain}(Exception|ErrorCode)"
    
  controller:
    path: "src/main/java/com/jk/amazon2/{domain}/controller"
    responsibility: "REST API"
    naming_pattern: "{Domain}Controller"

# 패키지 네이밍 규칙
package_naming:
  pattern: "com.jk.amazon2.{domain}.{layer}"
  examples:
    - "com.jk.amazon2.member.entity"
    - "com.jk.amazon2.member.service"
    - "com.jk.amazon2.member.dto"

# 의존성 규칙 (화살표는 import 방향)
dependency_rules:
  allowed:
    - "domain → common"        # 공통 레이어 의존 가능
    - "domain → config"        # Spring 설정 의존 가능
    - "service → repository"   # Service는 Repository 사용
    - "controller → service"   # Controller는 Service 사용
    - "dto → constant"         # DTO는 상수 참조 가능
    
  forbidden:
    - "domain ↔ domain"        # 도메인 간 직접 의존 금지
    - "controller → repository" # Controller는 Service를 거쳐야 함
    - "repository → service"   # 순환 의존성

# 테스트 구조
test_structure:
  entity_test:
    path: "src/test/java/com/jk/amazon2/{domain}/entity"
    pattern: "{Domain}Test"
    description: "핵심 도메인 entity 비즈니스 로직 테스트"
    
  controller_test:
    path: "src/test/java/com/jk/amazon2/{domain}/controller"
    pattern: "{Domain}ControllerTest"
    description: "통합 테스트 (Testcontainers, MockMvc)"
    
  service_test:
    path: "src/test/java/com/jk/amazon2/{domain}/service"
    pattern: "{Domain}ServiceTest"
    description: "단위 테스트 (Mockito)"
    
  repository_test:
    path: "src/test/java/com/jk/amazon2/{domain}/repository"
    pattern: "{Domain}RepositoryTest"
    description: "통합 테스트 (Testcontainers)"

# 공통 레이어 (모든 도메인에서 사용)
common_layers:
  - path: "src/main/java/com/jk/amazon2/common/exception"
    responsibility: "공통 예외 처리 (ErrorCode, GlobalExceptionHandler)"
    
  - path: "src/main/java/com/jk/amazon2/common/constant"
    responsibility: "공통 상수"
    
  - path: "src/main/java/com/jk/amazon2/common/dto"
    responsibility: "공통 DTO (ErrorResponse 등)"
    
  - path: "src/main/java/com/jk/amazon2/config"
    responsibility: "Spring 설정"
```

#### Step 2: 검증 규칙 문서화

- [ ] **Create `docs/architecture/VALIDATION_RULES.md`**

```markdown
# Amazon2 아키텍처 검증 규칙

## 패키지 구조

각 도메인은 다음 7개 계층을 필수로 포함해야 합니다:

```
com.jk.amazon2.{domain}/
├── entity/         [Domain].java
├── repository/     [Domain]Repository.java
├── service/        [Domain]CommandService.java, [Domain]QueryService.java
├── dto/            [Domain]CreateRequest.java, [Domain]UpdateRequest.java, [Domain]Response.java
├── exception/      [Domain]Exception.java, [Domain]ErrorCode.java
└── controller/     [Domain]Controller.java
```

## 네이밍 규칙

- Entity: `Member`, `Category`, `Posting` (단수형)
- Repository: `MemberRepository`, `MemberQueryRepository`
- Service: `MemberCommandService`, `MemberQueryService`
- DTO: `MemberCreateRequest`, `MemberUpdateRequest`, `MemberResponse`
- Exception: `MemberException`, `MemberErrorCode` (enum)
- Controller: `MemberController`
- Test: `MemberTest`, `MemberControllerTest`, `MemberServiceTest`, `MemberRepositoryTest`

## 의존성 규칙

**허용:**
- domain → common (공통 레이어)
- service → repository
- controller → service

**금지:**
- domain ↔ domain (직접 의존성)
- controller → repository (Service를 거쳐야 함)
```

#### Step 3: 커밋

- [ ] **Commit**

```bash
git add .claude/agents/shared-foundation.yaml docs/architecture/VALIDATION_RULES.md
git commit -m "Feat: Shared Foundation (harness-architecture-validator) 구현

- add: .claude/agents/shared-foundation.yaml (Amazon2 도메인 아키텍처 검증 규칙 정의)
- add: docs/architecture/VALIDATION_RULES.md (패키지/네이밍/의존성 규칙 문서화)"
```

---

### Task 2: Senior Code Reviewer 에이전트 구현

**Files:**
- Create: `.claude/agents/senior-code-reviewer.yaml`
- Create: `.claude/agents/senior-code-reviewer-prompt.md`

#### Step 1: 에이전트 정의

- [ ] **Create `.claude/agents/senior-code-reviewer.yaml`**

```yaml
name: senior-code-reviewer
version: 1.0.0
type: agent

description: |
  PR/코드 변경사항을 검토하는 시니어 리뷰어.
  아키텍처 규칙 위반(50%) + Best Practices(50%)를 동등 비중으로 검사.

triggers:
  - "PR 올릴거야"
  - "코드 리뷰 해줄래"
  - "이 구현 맞아?"

dependencies:
  - harness-architecture-validator  # 공유 기반

capabilities:
  - 패키지 구조 검증
  - 네이밍 규칙 검증
  - 의존성 규칙 검증
  - QueryDSL 성능 분석 (N+1 감지)
  - Exception 처리 패턴 검증
  - Test coverage 검증
  - 코드 중복 감지
  - Security 취약점 감지

output_format: |
  [규칙 위반]
  - Line 45: 패키지명 규칙 어김
  - Line 102: 금지된 의존성 발견
  
  [Best Practices]
  - QueryDSL fetch join 누락 (N+1 위험)
  - Exception 처리 누락
  
  [개선 제안]
  - 메서드 길이가 길어 보임 (분해 제안)
  - 하드코딩된 값 → constant로 이동 제안

worktree_strategy: read_only  # 읽기 전용 분석

max_output_length: 2000
```

#### Step 2: 에이전트 프롬프트 작성

- [ ] **Create `.claude/agents/senior-code-reviewer-prompt.md`**

```markdown
# Senior Code Reviewer Agent

## 역할
당신은 Amazon2 프로젝트의 시니어 개발자입니다. 
PR/코드 변경사항을 **아키텍처 규칙**(50%) + **Best Practices**(50%) 동등 비중으로 검토합니다.

## 검토 항목 (체크리스트)

### 1. 아키텍처 규칙 (50%)
- [ ] 패키지명이 `com.jk.amazon2.[domain].[layer]` 규칙을 따르는가?
- [ ] 클래스명이 네이밍 규칙(`MemberCommandService` 등)을 따르는가?
- [ ] 의존성 규칙을 어기지 않는가? (domain ↔ domain 금지, controller → service 필수)
- [ ] 금지된 계층 간 의존성이 있는가? (controller → repository 금지)
- [ ] 도메인 간 간접 의존성이 있는가? (common 레이어를 거쳐야 함)

### 2. Best Practices (50%)
- [ ] QueryDSL 쿼리에 fetch join이 있는가? (N+1 문제 방지)
- [ ] Exception 처리가 명시적인가? (generic Exception 금지)
- [ ] Optional을 올바르게 사용하는가? (.get() 금지, ifPresentOrElse 권장)
- [ ] 하드코딩된 값이 없는가? (constant/enum 사용)
- [ ] Service 메서드가 너무 길지 않은가? (50줄 초과 시 분해 고려)
- [ ] Test가 구현과 함께 있는가?
- [ ] 보안 취약점이 없는가? (SQL injection, XSS 등)

## 출력 형식

```
[규칙 위반] (0-5개)
- L45: MemberService의 패키지명 오류

[Best Practices] (0-10개)
- QueryDSL에서 fetch join 누락
- Exception을 일반 RuntimeException으로 감싸고 있음

[개선 제안] (0-5개)
- Service 메서드를 2개로 분해하면 더 깔끔할 듯
- Constant로 옮길 수 있는 매직 넘버들 있음

[통과 여부]
✅ 통과 (minor issues only) / ⚠️ 재검토 필요 / ❌ 불통과 (major issues)
```

## 사용 예시

**사용자:** "Member API 구현했는데 코드 리뷰 해줄래?"

**당신의 응답:**
1. diff 또는 파일 목록 요청
2. 위의 체크리스트 항목 검토
3. 규칙 위반 + Best Practices + 개선 제안 제시
4. 통과/재검토/불통과 판정
```

#### Step 3: 커밋

- [ ] **Commit**

```bash
git add .claude/agents/senior-code-reviewer.yaml .claude/agents/senior-code-reviewer-prompt.md
git commit -m "Feat: Senior Code Reviewer 에이전트 구현

- add: .claude/agents/senior-code-reviewer.yaml (에이전트 정의)
- add: .claude/agents/senior-code-reviewer-prompt.md (검토 항목 및 프롬프트)"
```

---

### Task 3: Domain Generator 에이전트 구현

**Files:**
- Create: `.claude/agents/domain-generator.yaml`
- Create: `.claude/agents/domain-generator-prompt.md`

#### Step 1: 에이전트 정의

- [ ] **Create `.claude/agents/domain-generator.yaml`**

```yaml
name: domain-generator
version: 1.0.0
type: agent

description: |
  새 도메인 추가 시 모든 보일러플레이트를 자동 생성.
  패키지 구조, 엔티티, 서비스, 레포지토리, DTO, 예외, 컨트롤러, 테스트, 문서화 자동 생성.

triggers:
  - "새 도메인 추가해줘"
  - "Booking 도메인 만들어줘"
  - "Member 같은 구조로 새 거 만들어"

dependencies:
  - harness-architecture-validator

capabilities:
  - 도메인 패키지 구조 생성
  - Entity 클래스 자동 생성
  - Repository (JpaRepository + QueryRepository) 생성
  - CommandService + QueryService 템플릿 생성
  - Request/Response DTO 생성
  - Exception + ErrorCode enum 생성
  - Controller CRUD 엔드포인트 생성
  - Test 스켈레톤 생성 (Controller, Service, Repository, Entity)
  - Harnesses 문서 생성 (harnesses/[domain]/README.md)
  - CLAUDE.md 업데이트 (200줄 제약 유지)

input:
  - domain_name: string (예: "booking")
  - fields: list[dict] (선택: 엔티티 필드)
  - relationships: list[dict] (선택: 다른 도메인과 관계)

output_format: |
  생성 파일 목록:
  - src/main/java/com/jk/amazon2/{domain}/entity/{Domain}.java
  - src/main/java/com/jk/amazon2/{domain}/repository/{Domain}Repository.java
  - ... (모든 파일 목록)
  
  생성 결과:
  ✅ 전체 도메인 구조 완성
  ✅ 패키지명/네이밍 검증 완료
  ✅ 테스트 스켈레톤 생성
  
  다음 단계:
  1. Entity 비즈니스 로직 추가
  2. Service 구현
  3. 테스트 작성

worktree_strategy: write  # 새 파일 생성

max_output_length: 5000
```

#### Step 2: 에이전트 프롬프트 작성

- [ ] **Create `.claude/agents/domain-generator-prompt.md`**

*(Step 2의 내용은 위 설계 문서와 동일하므로 생략)*

#### Step 3: 커밋

- [ ] **Commit**

```bash
git add .claude/agents/domain-generator.yaml .claude/agents/domain-generator-prompt.md
git commit -m "Feat: Domain Generator 에이전트 구현

- add: .claude/agents/domain-generator.yaml (에이전트 정의)
- add: .claude/agents/domain-generator-prompt.md (보일러플레이트 생성 가이드)"
```

---

### Task 4: Test Generator 에이전트 구현

#### Step 1: 에이전트 정의

- [ ] **Create `.claude/agents/test-generator.yaml`**

```yaml
name: test-generator
version: 1.0.0
type: agent

description: |
  도메인별 단위/통합 테스트 자동 생성.
  Controller (통합), Service (단위), Repository (통합), Entity (비즈니스 로직) 테스트.

triggers:
  - "기능 구현했어"
  - "테스트 작성해줘"
  - "이 API 테스트 코드 필요해"

dependencies:
  - harness-architecture-validator

capabilities:
  - Controller 통합 테스트 생성 (MockMvc, Testcontainers)
  - Service 단위 테스트 생성 (Mockito)
  - Repository 통합 테스트 생성 (Testcontainers)
  - Entity 비즈니스 로직 테스트 생성
  - Test fixture/data 자동 생성
  - Happy path + edge cases 커버
  - Exception 테스트 생성

input:
  - domain_name: string
  - implemented_methods: list[string]
  - special_test_cases: list[string] (선택)

output_format: |
  생성된 테스트 파일:
  - src/test/java/.../controller/{Domain}ControllerTest.java
  - src/test/java/.../service/{Domain}ServiceTest.java
  - src/test/java/.../repository/{Domain}RepositoryTest.java
  - src/test/java/.../entity/{Domain}Test.java
  
  테스트 항목:
  - Happy path (정상 케이스)
  - Edge cases (경계값, null 등)
  - Exception handling
  
  실행 명령:
  ./gradlew test --tests "com.jk.amazon2.{domain}.*"

worktree_strategy: write  # 테스트 파일 생성

max_output_length: 5000
```

#### Step 2: 에이전트 프롬프트 작성

- [ ] **Create `.claude/agents/test-generator-prompt.md`**

*(내용은 위의 설계 문서 참조)*

#### Step 3: 커밋

- [ ] **Commit**

```bash
git add .claude/agents/test-generator.yaml .claude/agents/test-generator-prompt.md
git commit -m "Feat: Test Generator 에이전트 구현

- add: .claude/agents/test-generator.yaml (에이전트 정의)
- add: .claude/agents/test-generator-prompt.md (테스트 생성 가이드 - Controller/Service/Repository/Entity)"
```

---

### Task 5: API Documenter 에이전트 구현

#### Step 1: 에이전트 정의

- [ ] **Create `.claude/agents/api-documenter.yaml`**

```yaml
name: api-documenter
version: 1.0.0
type: agent

description: |
  Swagger/OpenAPI 스펙 자동 생성/업데이트.
  Controller 분석 → Schema 생성 → Swagger UI 동기화.

triggers:
  - "API 추가했어"
  - "문서화 해줘"
  - "Swagger 업데이트"
  - Controller 변경 시 (자동)

dependencies:
  - harness-architecture-validator

capabilities:
  - Controller endpoint 자동 분석
  - OpenAPI 스펙 생성 (@Operation, @ApiResponse 등)
  - DTO → Schema 자동 변환
  - ErrorCode → Error Response 문서화
  - Swagger UI 동기화

input:
  - domain_name: string
  - changed_endpoints: list[string] (선택)

output_format: |
  업데이트 파일:
  - src/main/resources/openapi/{domain}-api.yaml
  - 또는 주석 기반 (@Operation, @Schema)
  
  Swagger UI:
  - http://localhost:8080/swagger-ui/index.html
  
  생성 내용:
  - GET, POST, PUT, DELETE 엔드포인트
  - Request/Response Schema
  - Error Response (400, 404, 500 등)

worktree_strategy: write

max_output_length: 3000
```

#### Step 2: 에이전트 프롬프트 작성

- [ ] **Create `.claude/agents/api-documenter-prompt.md`**

*(내용은 위의 설계 문서 참조)*

#### Step 3: 커밋

- [ ] **Commit**

```bash
git add .claude/agents/api-documenter.yaml .claude/agents/api-documenter-prompt.md
git commit -m "Feat: API Documenter 에이전트 구현

- add: .claude/agents/api-documenter.yaml (에이전트 정의)
- add: .claude/agents/api-documenter-prompt.md (Swagger/OpenAPI 문서화 가이드)"
```

---

## Phase 2: 검증 2개 에이전트

### Task 6: Harness Consistency Checker 에이전트 구현

#### Step 1: 에이전트 정의

- [ ] **Create `.claude/agents/consistency-checker.yaml`**

```yaml
name: consistency-checker
version: 1.0.0
type: agent

description: |
  도메인 간 구조/패턴/네이밍 일관성 검증.
  모든 도메인이 동일한 패턴을 따르는지 확인.

triggers:
  - 새 도메인 추가 후 (자동)
  - "일관성 체크해줘"

capabilities:
  - 패키지 구조 일관성 검증
  - 클래스 네이밍 일관성 검증
  - 어노테이션 사용 일관성
  - DTO 구조 일관성
  - Exception 패턴 일관성

output_format: |
  일관성 검증 결과:
  
  ✅ 통과한 항목
  - 패키지 구조: member, category, posting 동일
  
  ⚠️ 주의사항
  - Posting의 Exception 패턴이 다름
  
  ❌ 위반 항목
  - Member DTO는 enum, Category는 String

worktree_strategy: read_only
```

#### Step 2: 에이전트 프롬프트 작성

- [ ] **Create `.claude/agents/consistency-checker-prompt.md`**

*(내용은 위의 설계 문서 참조)*

#### Step 3: 커밋

- [ ] **Commit**

```bash
git add .claude/agents/consistency-checker.yaml .claude/agents/consistency-checker-prompt.md
git commit -m "Feat: Harness Consistency Checker 에이전트 구현

- add: .claude/agents/consistency-checker.yaml (에이전트 정의)
- add: .claude/agents/consistency-checker-prompt.md (일관성 검증 항목)"
```

---

### Task 7: Dependency Analyzer 에이전트 구현

#### Step 1: 에이전트 정의

- [ ] **Create `.claude/agents/dependency-analyzer.yaml`**

```yaml
name: dependency-analyzer
version: 1.0.0
type: agent

description: |
  도메인 간 의존성 분석 - 순환 의존성, 과도한 커플링 감지.

triggers:
  - 기능 추가 후 (자동)
  - "의존성 분석해줘"

capabilities:
  - 순환 의존성 감지 (A → B → A)
  - 과도한 크로스 도메인 의존성 감지
  - Common 레이어 의존성 검증
  - 불허된 계층 간 의존성 감지
  - 의존성 그래프 시각화

output_format: |
  [의존성 분석 결과]
  
  ✅ 정상
  - Member → Repository → Service → Controller
  
  ⚠️ 주의
  - Category.Controller에서 Member.Service import 1회
  
  ❌ 순환/위반
  - Posting.Service ↔ Posting.Repository (순환)
  - Posting.Service → Member.Repository (금지된 직접 임포트)

worktree_strategy: read_only
```

#### Step 2: 에이전트 프롬프트 작성

- [ ] **Create `.claude/agents/dependency-analyzer-prompt.md`**

*(내용은 위의 설계 문서 참조)*

#### Step 3: 커밋

- [ ] **Commit**

```bash
git add .claude/agents/dependency-analyzer.yaml .claude/agents/dependency-analyzer-prompt.md
git commit -m "Feat: Dependency Analyzer 에이전트 구현

- add: .claude/agents/dependency-analyzer.yaml (에이전트 정의)
- add: .claude/agents/dependency-analyzer-prompt.md (순환/금지 의존성 감시 항목)"
```

---

## Task 8: 에러 관리 시스템 초기화

#### Step 1: 에러 레지스트리

- [ ] **Create `.claude/errors/ERRORS.md`**

```markdown
# Error Registry

Amazon2 프로젝트의 발생한 에러들을 기록합니다.
각 에러는 고유 ID를 가지며, 재발을 방지하기 위해 메모리에 저장됩니다.

---

## E001: INVALID_PACKAGE_STRUCTURE

**Description:** 도메인 패키지가 규칙을 따르지 않음

**Severity:** High

**Root Cause:** Domain Generator 또는 수동 작성 시 잘못된 package name

**Symptoms:**
- `com.jk.amazon2.member.services.MemberService` (❌)
- 정상: `com.jk.amazon2.member.service.MemberCommandService`

**Prevention:**
- Domain Generator의 패키지 네이밍 로직 검증
- Consistency Checker로 주기적 검증

**Status:** OPEN (예방 단계)

---

## E002: FORBIDDEN_CROSS_DOMAIN_DEPENDENCY

**Description:** 도메인 간 금지된 직접 의존성

**Severity:** High

**Root Cause:** Service가 다른 도메인의 Repository를 직접 import

**Symptoms:**
```
PostingService → MemberRepository (❌)
정상: PostingService → MemberQueryService
```

**Prevention:**
- Dependency Analyzer로 자동 감지
- Senior Code Reviewer에서 import 검증

**Status:** OPEN (감시 단계)
```

#### Step 2: 에러 패턴 문서화

- [ ] **Create `.claude/errors/ERROR_PATTERNS.md`**

```markdown
# Error Patterns & Prevention

실제 발생했던 에러들의 패턴과 재발 방지 방법입니다.

## 패턴 1: 패키지 네이밍 에러

**증상:** IDE 자동 임포트 실패, 테스트 검색 누락

**원인:** Domain Generator에서 잘못된 패키지명 생성

**재발 방지:**
- Domain Generator: 생성 직후 검증 강화
- Consistency Checker: 모든 도메인 주기적 검증
- Senior Code Reviewer: PR에서 감지

---

## 패턴 2: 크로스 도메인 의존성

**증상:** 도메인 간 강한 결합, 변경 파급 범위 증가

**원인:** Service에서 다른 도메인의 Repository 직접 import

**재발 방지:**
- Dependency Analyzer: 자동 감지
- 공통 DTO로 분리
- Senior Code Reviewer: import 검증

---

## 패턴 3: 순환 의존성

**증상:** 컴파일 오류, 예측 불가능한 버그

**원인:** Service ↔ Repository 상호 의존

**재발 방지:**
- Dependency Analyzer: 순환 감지
- Test 실행으로 빌드 오류 확인
```

#### Step 3: 메모리 시스템 초기화

- [ ] **Create `.claude/memory/error_patterns/agent_feedback.md`**

```markdown
---
name: agent-feedback-system
description: 에이전트들의 에러 피드백을 저장하고 재사용하는 메모리 시스템
metadata:
  type: feedback
---

# Agent Feedback Memory System

각 에이전트가 발견한 에러나 개선사항을 저장합니다.

## Senior Code Reviewer Feedback

*아직 피드백 없음*

## Domain Generator Feedback

*아직 피드백 없음*

## Consistency Checker Feedback

*아직 피드백 없음*

## Dependency Analyzer Feedback

*아직 피드백 없음*

---

## 피드백 형식

[에이전트명]: [에러/개선사항]

Why: [이유]
How to apply: [적용 방법]
Impact: [영향도]
```

#### Step 4: 커밋

- [ ] **Commit**

```bash
git add .claude/errors/ .claude/memory/error_patterns/
git commit -m "Feat: 에러 관리 시스템 초기화

- add: .claude/errors/ERRORS.md (에러 레지스트리)
- add: .claude/errors/ERROR_PATTERNS.md (재발 방지 패턴)
- add: .claude/memory/error_patterns/agent_feedback.md (에이전트 피드백 메모리)"
```

---

## Task 9: Phase 1 & 2 최종 통합

#### Step 1: 모든 파일 확인

- [ ] **Verify all files created**

```bash
ls -la .claude/agents/ | wc -l
# 예상: 13개 파일 (yaml + prompt pairs + shared-foundation)

ls -la .claude/errors/
ls -la .claude/memory/error_patterns/
```

#### Step 2: CLAUDE.md 업데이트

- [ ] **Add Subagent Section to CLAUDE.md**

현재 CLAUDE.md의 마지막에 추가:

```markdown
## Subagent System

도메인 기반 개발 생산성 향상을 위해 6개 특화 에이전트 운영:

**Phase 1 (핵심 4개)**
- Senior Code Reviewer: PR/코드 검토 (규칙 50% + Best Practices 50%)
- Domain Generator: 새 도메인 자동 생성 (패키지, 엔티티, 서비스 등)
- Test Generator: 단위/통합 테스트 자동 생성
- API Documenter: Swagger/OpenAPI 자동 문서화

**Phase 2 (검증 2개)**
- Consistency Checker: 도메인 간 패턴 일관성 검증
- Dependency Analyzer: 순환/금지 의존성 감시

자세한 설계: [docs/superpowers/specs/2026-06-12-subagent-architecture-design.md](docs/superpowers/specs/2026-06-12-subagent-architecture-design.md)
```

#### Step 3: 최종 커밋

- [ ] **Commit Phase 1 & 2 완료**

```bash
git add CLAUDE.md
git commit -m "Docs: Subagent System CLAUDE.md에 문서화

- add: Subagent System 개요 (Phase 1, Phase 2)
- add: 6개 에이전트 설명 및 설계 문서 링크"
```

---

## 실행 옵션

**계획 완료! 두 가지 실행 방식이 있습니다:**

### 🚀 1. **Subagent-Driven (권장)**

각 Task마다 새로운 subagent를 디스패치하고, 완료 후 2단계 리뷰를 진행합니다.
- Task별 병렬 처리 가능
- 빠른 반복
- 리뷰 체크포인트 있음

```bash
# 구현 준비
./gradlew clean build  # 현재 상태 확인
```

### 📋 2. **Inline Execution**

이 세션에서 모든 Tasks를 순차 실행하고 체크포인트에서 리뷰합니다.
- 현재 세션 내 완료
- 리뷰 후 다음 Task 진행

---

**어떤 방식으로 진행하시겠습니까?**