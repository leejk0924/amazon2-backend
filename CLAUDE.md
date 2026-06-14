# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

Amazon2 - 네이버 블로그 모임 관리 서비스 | Java 21, Spring Boot 4.0.0, MySQL 8.x

---

## 빠른 시작

```bash
cp src/main/resources/application-local.yml.example src/main/resources/application-local.yml
./gradlew bootRun --args='--spring.profiles.active=local'  # 로컬 개발
./gradlew test                                             # 전체 테스트
./gradlew clean build                                      # 빌드
```

---

## 프로젝트 구조

```
src/main/java/com/jk/amazon2/
├── member/      # 회원 모듈
├── category/    # 카테고리 모듈
├── posting/     # 포스팅 모듈
├── common/      # 공통 코드 (예외, 상수, DTO)
└── config/      # Spring 설정

각 도메인: controller, service, repository, entity, dto, exception
```

---

## Git 워크플로우 (⚠️ 필수)

### 브랜칭 전략

모든 기능 추가 시 다음 규칙을 **반드시** 따르세요:

```bash
# 1️⃣ GitHub에서 이슈 생성
#    예: Issue #42 "포스팅 관련 API 구현"

# 2️⃣ Feature 브랜치 생성 (main 직접 수정 금지)
git checkout -b feature/#42-포스팅-관련-api-구현

# 3️⃣ 해당 브랜치에서 작업 & 커밋
git add ...
git commit -m "feat: 포스팅 API 구현"

# 4️⃣ PR 생성 (이슈 번호와 함께)
gh pr create --title "#42: 포스팅 관련 API 구현" \
  --body "Closes #42"

# 5️⃣ 머지 후 이슈 자동 종료
```

**브랜치 명명 규칙:**
- 형식: `feature/#<이슈번호>-<한글설명>`
- 예시: `feature/#13-github-mcp-서버-구성`
- ❌ main, master 직접 수정 금지

**Worktree 사용 (선택):**
```bash
# 깨끗한 독립 작업 환경 원할 때
git worktree add -b feature/#42-포스팅-api ../amazon2-feature-42
cd ../amazon2-feature-42
# 여기서 작업
```

---

## 개발 가이드

- **전체 아키텍처**: [harnesses/README.md](harnesses/README.md)
- **기여 규칙**: [docs/CONTRIBUTING.md](docs/CONTRIBUTING.md)
- 새로운 기능은 해당 도메인 폴더 아래에서 개발

---

## 6개 Claude 에이전트 시스템

자동화(Phase 1) + 검증(Phase 2)을 통한 일관성 있는 개발 환경

### Phase 1: 자동화

**Domain Generator** — 도메인 보일러플레이트 생성
- 입력: `domain_name`, `create_dto`, `create_exception`
- 산출: Entity, DTO, Service, Controller, Repository, Exception

**Test Generator** — JUnit5/Mockito 테스트 자동 생성
- 입력: `domain_name`, `test_type` (all/controller/service/repository/entity)
- 산출: 4가지 테스트 클래스

**API Documenter** — Swagger/OpenAPI 문서 생성
- 입력: `domain_name`, `documentation_type` (swagger_annotation/openapi_yaml/both)
- 산출: @Operation 어노테이션 + OpenAPI YAML

**가이드**: `.claude/agents/{agent-name}/main-prompt.md`

### Phase 2: 검증

**Consistency Checker** — 패키지 및 코드 일관성 검증
- 입력: `domain_name`, `check_type` (all/package/naming/annotation/dto/exception)
- 산출: 이슈 리포트 (ERROR/WARNING/INFO)

**Dependency Analyzer** — 의존성 구조 분석
- 입력: `analysis_type` (all/circular/forbidden/graph/cross_domain)
- 산출: 의존성 그래프 (text/json/mermaid)

---

## 도메인 계층 (member > category > posting)

```
✅ 의존성 방향: 상향만 가능
  posting → category → member (O)

❌ 역방향 또는 순환
  member → category (X)
  category → posting → category (X)
```

---

## 워크플로우 예시

### 새 도메인 추가

```
Domain Generator → Consistency Checker (auto_fix) → Test Generator 
→ API Documenter → Dependency Analyzer (검증) → 완료
```

### 기존 도메인 검증

```
Consistency Checker (report) → 이슈 수정 → Dependency Analyzer → 완료
```

---

## 에러 관리

- **정의**: [.claude/errors/ERRORS.md](.claude/errors/ERRORS.md) (16개 코드)
- **패턴**: [.claude/errors/ERROR_PATTERNS.md](.claude/errors/ERROR_PATTERNS.md)
- **피드백**: [.claude/memory/error_patterns/agent_feedback.md](.claude/memory/error_patterns/agent_feedback.md)

---

## 환경 설정

- `application.yml` - 공통
- `application-local.yml` - 로컬 (DB 접속)
- `application-test.yml` - 테스트 (Testcontainers)
- `application-prod.yml` - 운영 (환경 변수)