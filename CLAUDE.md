# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

Amazon2 - 네이버 블로그 모임 관리 서비스 | Java 21, Spring Boot 4.0.0, MySQL 8.x

---

## 빠른 시작

```bash
# 로컬 환경 설정
cp src/main/resources/application-local.yml.example src/main/resources/application-local.yml

# 실행
./gradlew bootRun --args='--spring.profiles.active=local'

# 테스트 (Docker 필수)
./gradlew test

# 빌드
./gradlew clean build
```

---

## 핵심 명령어

| 명령 | 설명 |
|------|------|
| `./gradlew bootRun --args='--spring.profiles.active=local'` | 로컬 개발 모드 |
| `./gradlew test` | 전체 테스트 |
| `./gradlew test --tests "com.jk.amazon2.member.*"` | 도메인별 테스트 |
| `./gradlew clean bootJar` | JAR 빌드 |
| `docker compose up -d` | Docker 실행 |

---

## 프로젝트 구조

```
src/main/java/com/jk/amazon2/
├── member/              # 회원 모듈
│   ├── controller/      # REST API
│   ├── service/         # 비즈니스 로직
│   ├── repository/      # 데이터 접근 (JPA)
│   ├── entity/          # JPA 엔티티
│   ├── dto/             # DTO (Request/Response)
│   └── exception/       # 도메인 예외
├── category/            # 카테고리 모듈
│   ├── controller/      # REST API
│   ├── service/         # 비즈니스 로직
│   ├── repository/      # 데이터 접근 (JPA)
│   ├── entity/          # JPA 엔티티
│   ├── dto/             # DTO (Request/Response)
│   └── exception/       # 도메인 예외
├── posting/             # 포스팅 모듈
│   ├── controller/      # REST API
│   ├── service/         # 비즈니스 로직
│   ├── repository/      # 데이터 접근 (JPA)
│   ├── entity/          # JPA 엔티티
│   ├── dto/             # DTO (Request/Response)
│   └── exception/       # 도메인 예외
├── common/              # 공통 코드
│   ├── exception/       # 글로벌 예외 처리 (ErrorCode, GlobalExceptionHandler, ErrorResponse)
│   ├── constant/        # 공통 상수
│   ├── dto/             # 공통 DTO
│   └── utils/           # 유틸리티 클래스
└── config/              # Spring 설정
```

### 개발 방식

- 모든 새로운 기능은 해당 도메인 폴더 아래에서 개발합니다
- 도메인별로 완전히 독립적인 모듈 구조입니다
- 새 도메인 추가 시 동일한 구조로 폴더를 생성합니다
- 각 도메인의 상세 가이드는 `harnesses/[domain]/README.md` 참조

---

## 개발 가이드

- **전체 아키텍처**: [harnesses/README.md](harnesses/README.md)
- **Member (회원)**: [harnesses/member/README.md](harnesses/member/README.md)
- **Category (카테고리)**: [harnesses/category/README.md](harnesses/category/README.md)
- **Posting (포스팅)**: [harnesses/posting/README.md](harnesses/posting/README.md)
- **기여 규칙**: [docs/CONTRIBUTING.md](docs/CONTRIBUTING.md)

---

## 환경 설정

프로필별 설정 파일:
- `application.yml` - 공통
- `application-local.yml` - 로컬 (DB 접속)
- `application-test.yml` - 테스트 (Testcontainers)
- `application-prod.yml` - 운영 (환경 변수)

---

## Subagent System (Phase 1 & 2)

Amazon2 백엔드 개발을 자동화하고 일관성을 보장하기 위한 6개의 Claude 에이전트 시스템입니다.

### 개요

```
Phase 1: 자동화 (도메인 생성 및 테스트)
├── Domain Generator
├── Test Generator
└── API Documenter

Phase 2: 검증 (아키텍처 일관성)
├── Harness Consistency Checker
├── Dependency Analyzer
└── Error Management System
```

### Phase 1: 자동화 (개발 속도 향상)

#### 1. Domain Generator
**목적**: Spring Boot 도메인 구조 및 보일러플레이트 자동 생성

**가이드**: [.claude/agents/domain-generator-prompt.md](.claude/agents/domain-generator-prompt.md)

**입력 파라미터**:
```yaml
domain_name: product          # 도메인명 (required)
create_dto: true             # DTO 생성 여부
create_exception: true       # Exception 생성 여부
create_enum: false           # Enum 생성 여부
```

**생성 내용** (10단계):
1. 패키지 구조 생성 (entity, dto, repository, service, controller, exception)
2. Entity 클래스 (JPA 어노테이션)
3. DTO 클래스 (CreateRequest, UpdateRequest, Response)
4. Repository 인터페이스 (JpaRepository)
5. Service 클래스 (@Service, @Transactional)
6. Controller 클래스 (@RestController, CRUD API)
7. Exception 클래스 (커스텀 Exception)
8. Enum 클래스 (상태 관리)
9. 테스트 스켈레톤 (Controller/Service/Repository/Entity)
10. 설정 및 검증 (패키지 경로, 네이밍, 일관성)

**사용 예시**:
```bash
# 새 도메인 "product" 생성
domain_name: product
create_dto: true
create_exception: true
create_enum: true

# 결과: src/main/java/com/jk/amazon2/product/ 하에
# - entity/Product.java
# - dto/ProductCreateRequest.java, ProductUpdateRequest.java, ProductResponse.java
# - repository/ProductRepository.java
# - service/ProductService.java
# - controller/ProductController.java
# - exception/ProductException.java
```

#### 2. Test Generator
**목적**: JUnit5/Mockito 기반 테스트 자동 생성

**가이드**: [.claude/agents/test-generator-prompt.md](.claude/agents/test-generator-prompt.md)

**입력 파라미터**:
```yaml
domain_name: product          # 도메인명 (required)
test_type: all               # all, controller, service, repository, entity
use_testcontainers: true     # Testcontainers 사용 여부
include_integration_tests: false  # 통합 테스트 포함
```

**생성 내용** (4가지 테스트):
1. **Controller Test** (@WebMvcTest)
   - REST API 엔드포인트 검증
   - 요청/응답 데이터 검증
   - HTTP 상태 코드 검증

2. **Service Test** (@ExtendWith MockitoExtension)
   - 비즈니스 로직 검증
   - Mock 의존성 처리
   - Exception 처리 검증

3. **Repository Test** (@DataJpaTest + Testcontainers)
   - JPA 쿼리 검증
   - 실제 DB 환경 테스트
   - 커스텀 쿼리 메서드 검증

4. **Entity Test**
   - 엔티티 필드 검증
   - equals/hashCode/toString 검증

**사용 예시**:
```bash
domain_name: product
test_type: all
use_testcontainers: true

# 결과: src/test/java/com/jk/amazon2/product/ 하에
# - controller/ProductControllerTest.java
# - service/ProductServiceTest.java
# - repository/ProductRepositoryTest.java
# - entity/ProductTest.java
```

#### 3. API Documenter
**목적**: Swagger/OpenAPI 문서 자동 생성

**가이드**: [.claude/agents/api-documenter-prompt.md](.claude/agents/api-documenter-prompt.md)

**입력 파라미터**:
```yaml
domain_name: product          # 도메인명 (required)
documentation_type: both      # swagger_annotation, openapi_yaml, both
include_error_responses: true  # 에러 응답 포함
include_examples: true         # 요청/응답 예시 포함
```

**생성 내용** (2가지 방식):
1. **Swagger 어노테이션**
   - @Operation, @ApiResponse
   - @Schema (DTO)
   - @Parameter (파라미터)
   - Swagger UI: http://localhost:8080/swagger-ui.html

2. **OpenAPI YAML**
   - docs/openapi/{domain}-api.yaml
   - OpenAPI 3.0 스펙
   - 수동 API 개발시 참고용

---

### Phase 2: 검증 (아키텍처 일관성)

#### 4. Harness Consistency Checker
**목적**: 패키지 구조, 네이밍, 어노테이션, DTO, Exception 일관성 검증

**가이드**: [.claude/agents/consistency-checker-prompt.md](.claude/agents/consistency-checker-prompt.md)

**입력 파라미터**:
```yaml
domain_name: product          # 도메인명 (required)
check_type: all              # all, package, naming, annotation, dto, exception
auto_fix: false              # 자동 수정 여부
report_format: detailed       # summary, detailed, json
```

**검증 항목** (5가지):
1. **패키지 구조** (Package Structure Check)
   - 필수 패키지 존재 여부
   - 올바른 경로 (com.jk.amazon2.{domain}.*)
   - 테스트 구조 검증

2. **네이밍 컨벤션** (Naming Convention Check)
   - 클래스명: PascalCase
   - 메서드명: camelCase
   - 상수명: UPPER_SNAKE_CASE
   - DTO: {Domain}CreateRequest/UpdateRequest/Response

3. **어노테이션** (Annotation Check)
   - @Entity, @Table
   - @Service, @Transactional
   - @RestController, @RequestMapping
   - @Data, @Builder (DTO)

4. **DTO 구조** (DTO Check)
   - CreateRequest (필수 필드만)
   - UpdateRequest (선택 필드)
   - Response (모든 필드 + id, createdAt, updatedAt)

5. **Exception** (Exception Check)
   - 커스텀 Exception 존재
   - RuntimeException 상속
   - 필수 생성자

**사용 예시**:
```bash
domain_name: product
check_type: all
auto_fix: true
report_format: detailed

# 결과: 이슈 분류 및 자동 수정
# - ERROR: 빌드 실패 항목 (필수)
# - WARNING: 권장사항 (선택)
# - INFO: 개선 제안
```

#### 5. Dependency Analyzer
**목적**: 순환 의존성, 금지 의존성, 도메인 간 의존성 분석

**가이드**: [.claude/agents/dependency-analyzer-prompt.md](.claude/agents/dependency-analyzer-prompt.md)

**입력 파라미터**:
```yaml
analysis_type: all            # all, circular, forbidden, graph, cross_domain
include_transitive: true      # 전이적 의존성 포함
output_format: text           # text, json, dot, mermaid
```

**분석 항목** (3가지):
1. **순환 의존성** (Circular Dependency Detection)
   - DFS 기반 사이클 감지
   - 경로 추출 및 시각화
   - 예: A → B → C → A

2. **금지 의존성** (Forbidden Dependency Check)
   - 계층 위반: Entity → Service (E001), Repository → Controller (E002)
   - Cross-domain 역방향: category → member (E003)
   - Entity → Entity: posting.entity → category.entity (E008)

3. **의존성 그래프** (Dependency Graph Visualization)
   - DOT 형식 (Graphviz)
   - Mermaid 형식
   - JSON 형식

**Amazon2 도메인 계층**:
```
member (Level 1, 최상위)
  ↓ (의존 가능)
category (Level 2)
  ↓ (의존 가능)
posting (Level 3, 최하위)
```

**사용 예시**:
```bash
analysis_type: all
include_transitive: true
output_format: mermaid

# 결과: 의존성 그래프 시각화
# - 정상 의존성: 초록색
# - 위반 의존성: 빨강색
# - 순환 의존성: 강조 표시
```

---

### Error Management System

**목적**: 발생 가능한 모든 에러를 정의하고 관리

**에러 정의**: [.claude/errors/ERRORS.md](.claude/errors/ERRORS.md)
- 16개 에러 코드 (E001-E501)
- 심각도별 분류 (ERROR, WARNING, INFO)
- 원인, 해결책, 올바른 예시

**패턴 분석**: [.claude/errors/ERROR_PATTERNS.md](.claude/errors/ERROR_PATTERNS.md)
- **패턴 1**: 패키지 네이밍 및 구조 오류 (E100-E102)
- **패턴 2**: 크로스 도메인 의존성 오류 (E003, E008)
- **패턴 3**: 순환 의존성 오류 (E004)

**메모리 시스템**: [.claude/memory/error_patterns/agent_feedback.md](.claude/memory/error_patterns/agent_feedback.md)
- 에이전트 실행 이력
- 발견된 에러 패턴
- 학습 내용 및 개선

---

### 워크플로우 예시

#### 1. 새 도메인 추가 워크플로우

```
1단계: Domain Generator 실행
  domain_name: order
  create_dto: true
  create_exception: true

2단계: Consistency Checker 검증
  domain_name: order
  check_type: all
  auto_fix: true

3단계: Test Generator 실행
  domain_name: order
  test_type: all

4단계: API Documenter 실행
  domain_name: order
  documentation_type: both

5단계: Dependency Analyzer 최종 검증
  analysis_type: all

결과: order 도메인 완성
- 패키지 구조 ✅
- 테스트 코드 ✅
- API 문서 ✅
- 아키텍처 검증 ✅
```

#### 2. 기존 도메인 리팩토링 워크플로우

```
1단계: Consistency Checker로 현황 파악
  domain_name: product
  report_format: detailed

2단계: 이슈 분류 및 우선순위 정렬
  - ERROR (필수): 즉시 수정
  - WARNING (권장): 일정 내 수정

3단계: 이슈 수정
  - auto_fix 사용 또는 수동 수정

4단계: 재검증
  consistency_check: all
  dependency_analysis: all

5단계: 피드백 기록
  .claude/memory/error_patterns/agent_feedback.md 업데이트
```

#### 3. 정기 아키텍처 감사 워크플로우

```
월 1회 아키텍처 감사 프로세스:

1단계: 전체 의존성 분석
  Dependency Analyzer
  - analysis_type: all
  - output_format: json

2단계: 순환 의존성 확인
  - 발견시 즉시 해결

3단계: Cross-domain 의존성 검토
  - 정책 위반 확인

4단계: 보고서 작성
  - 메모리 시스템에 기록
  - 개선 계획 수립

5단계: 리팩토링 (필요시)
  - 우선순위 기반 실행
```

---

### 에이전트 파일 구조

```
.claude/
├── agents/
│   ├── domain-generator.yaml
│   ├── domain-generator-prompt.md
│   ├── test-generator.yaml
│   ├── test-generator-prompt.md
│   ├── api-documenter.yaml
│   ├── api-documenter-prompt.md
│   ├── consistency-checker.yaml
│   ├── consistency-checker-prompt.md
│   ├── dependency-analyzer.yaml
│   ├── dependency-analyzer-prompt.md
│   ├── shared-foundation.yaml
│   └── senior-code-reviewer.yaml
├── errors/
│   ├── ERRORS.md                 # 16개 에러 코드 정의
│   └── ERROR_PATTERNS.md         # 3가지 주요 패턴
├── memory/
│   └── error_patterns/
│       └── agent_feedback.md     # 에이전트 실행 이력 & 학습
└── settings.json                  # 에이전트 설정
```

---

### 추천 사용 시점

| 상황 | 에이전트 | 용도 |
|------|---------|------|
| 새 도메인 시작 | Domain Generator | 보일러플레이트 생성 |
| 도메인 구현 후 | Test Generator | 테스트 코드 생성 |
| API 개발 완료 | API Documenter | Swagger 문서 생성 |
| PR 전 검증 | Consistency Checker | 일관성 확인 |
| 대규모 리팩토링 | Dependency Analyzer | 아키텍처 검증 |
| 월 1회 감사 | Dependency Analyzer | 전체 시스템 검토 |
| 이슈 발생시 | 모든 에이전트 | 에러 분석 및 해결 |

---