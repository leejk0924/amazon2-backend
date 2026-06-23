# CLAUDE.md

> **⚠️ 이 파일은 최대 200줄입니다. 상세 규칙은 `.claude/` 디렉토리를 참고하세요.**

---

## ⚠️ CRITICAL: 이 파일의 모든 규칙은 필수입니다

이 CLAUDE.md에 작성된 모든 규칙, 워크플로우, 에이전트 사용법은:
- ❌ 선택사항이 아닙니다
- ✅ 반드시 따라야 하는 필수 규칙입니다
- 🔴 위반 시 작업 품질 저하 및 버그 발생 위험

특히 아래 3가지는 매번 확인하세요:
1. **자동 커밋 금지** — 모든 git commit 전에 사용자 확인 필수!
2. **Feature 브랜칭** — `feature/#<이슈>-<한글설명>` + git worktree 필수
3. **GitHub MCP 활용** — 이슈 조회 → 작업 흐름 자동화 (이미 설정됨)

---

Amazon2 - 네이버 블로그 모임 관리 서비스 | Java 21, Spring Boot 4.0.0, MySQL 8.x

---

## ⚠️ 필수 3가지 규칙 (매번 확인!)

> 이전 실수를 방지하기 위한 최우선 규칙입니다.

1. **✅ 자동 커밋 금지** — 모든 git commit 전에 사용자 확인 필수!
   - 파일 변경사항 → 사용자 승인 → 커밋 실행
   - `.claude/rules/git-workflow.md` 참고

2. **✅ Feature 브랜칭** — `feature/#<이슈>-<한글설명>` + git worktree 필수
   - Main 직접 수정 금지
   - 병렬 작업 안전성 확보

3. **✅ 메모리 우선** — 규칙은 `.claude/` 메모리에 저장, 구현 결정은 논의 후
   - 프로젝트 메모리: `.claude/projects/.../memory/`
   - GitHub 자동화: `.claude/rules/github-mcp.md`

---

## 🚀 빠른 시작

```bash
cp src/main/resources/application-local.yml.example src/main/resources/application-local.yml
./gradlew bootRun --args='--spring.profiles.active=local'
./gradlew test
./gradlew clean build
```

---

## 📁 프로젝트 구조

```
src/main/java/com/jk/amazon2/
├── member/      # 회원 모듈
├── category/    # 카테고리 모듈
├── posting/     # 포스팅 모듈
├── common/      # 공통 코드
└── config/      # Spring 설정
```

---

## 🔗 핵심 규칙 (상세는 링크 참고)

| 항목 | 설명 | 상세 위치 |
|------|------|---------|
| **커밋 규칙** | 자동 커밋 금지, 반드시 사용자 확인 | `.claude/rules/git-workflow.md` |
| **브랜칭** | feature/#이슈-설명 + git worktree | `.claude/rules/git-workflow.md` |
| **GitHub MCP** | 이슈 조회 → 작업 흐름 자동화 | `.claude/rules/github-mcp.md` |
| **의존성** | posting → category → member | [harnesses/README.md](harnesses/README.md) |
| **기여 규칙** | 커밋 메시지 형식 (Feat/Fix/Docs) | [docs/CONTRIBUTING.md](docs/CONTRIBUTING.md) |
| **에러 관리** | 프로젝트별 에러 코드 | `.claude/errors/ERRORS.md` |
| **프로젝트 메모리** | 대화 간 지속되는 컨텍스트 | `.claude/projects/...memory/` |
| **코드 리뷰 출력** | 리뷰 결과는 Notion에 저장 (로컬 파일 금지) | `.claude/rules/code-review-output.md` |

---

## 📋 필수: 6개 Claude 에이전트 시스템

### Phase 1: 자동화 에이전트

#### 🏗️ Domain Generator
**언제 사용:** 새로운 도메인(entity, service, controller, repository) 생성 시
```bash
# 예: "comment 도메인 생성해줘" → 자동으로:
# - Comment.java (엔티티)
# - CommentDTO.java (DTO)
# - CommentRepository.java (리포지토리)
# - CommentService.java (서비스)
# - CommentController.java (컨트롤러)
```

#### 🧪 Test Generator
**언제 사용:** Controller, Service 구현 완료 후 테스트 작성 필요 시
```bash
# 예: "MonitoringService/Controller에 대한 테스트 생성해줘"
# → 자동으로:
# - MonitoringServiceTest.java (@ExtendWith(MockitoExtension.class))
# - MonitoringControllerTest.java (@WebMvcTest)
```

#### 📚 API Documenter
**언제 사용:** REST API 엔드포인트에 Swagger 문서 필요 시
```bash
# 예: "PostingController API 문서 자동 생성해줘"
# → 자동으로:
# - @Operation, @ApiResponse 애노테이션 추가
# - DTO에 @Schema 추가
# - OpenAPI YAML 생성
```

### Phase 2: 검증 에이전트

#### ✅ Consistency Checker
**언제 사용:** 새로운 도메인 또는 리팩토링 후 규칙 준수 확인 시
```bash
# 예: "posting 도메인이 하네스 규칙을 따르는지 확인해줘"
# → 패키지 구조, 네이밍, 애노테이션 검증
```

#### 🔗 Dependency Analyzer
**언제 사용:** 의존성 구조 확인 또는 순환 참조 검사 시
```bash
# 예: "posting → category → member 의존성이 맞는지 확인해줘"
# → 의존성 그래프 생성, 순환 참조 검사
```

---

### 🚀 사용 패턴

```
1단계: 기능 요청
당신: "comment 도메인 만들어줘"

2단계: Domain Generator 자동 실행
Claude: "Domain Generator를 사용해서 comment 도메인을 생성하겠습니다."
→ 엔티티, DTO, Repository, Service, Controller 자동 생성

3단계: 테스트 작성
당신: "테스트도 작성해줘"

4단계: Test Generator 자동 실행
Claude: "Test Generator를 사용해서 테스트를 생성하겠습니다."
→ ServiceTest, ControllerTest 자동 생성

5단계: 검증
당신: "하네스 규칙 확인해줄래?"

6단계: Consistency Checker 자동 실행
Claude: "Consistency Checker로 패키지 구조를 검증하겠습니다."
→ 규칙 위반 사항 보고 및 개선안 제시
```

---

### 📖 상세 문서
- [에이전트 사용 가이드](./.claude/rules/agent-usage.md)
- [Domain Generator](./.claude/agents/domain-generator/main-prompt.md)
- [Test Generator](./.claude/agents/test-generator/main-prompt.md)
- [API Documenter](./.claude/agents/api-documenter/main-prompt.md)
- [Consistency Checker](./.claude/agents/consistency-checker/main-prompt.md)
- [Dependency Analyzer](./.claude/agents/dependency-analyzer/main-prompt.md)

---

## ⚠️ 필수 확인사항

기능 추가 전 반드시 이것들을 확인하세요:

1. **Git 워크플로우** - [`.claude/rules/git-workflow.md`](./.claude/rules/git-workflow.md) 읽기
2. **프로젝트 메모리** - [feature_branch_workflow.md](https://github.com/leejk0924/amazon2-backend) 참고
3. **커밋 확인** - 자동 커밋 금지, 반드시 사용자 승인 후 실행

---

**마지막 업데이트**: 2026-06-14  
**구조**: 프로젝트 루트 CLAUDE.md (이 파일) ← `.claude/` 참고 → 규칙 상세 관리
