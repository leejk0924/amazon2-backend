# CLAUDE.md

> **⚠️ 이 파일은 최대 200줄입니다. 상세 규칙은 `.claude/` 디렉토리를 참고하세요.**

Amazon2 - 네이버 블로그 모임 관리 서비스 | Java 21, Spring Boot 4.0.0, MySQL 8.x

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

---

## 📋 6개 Claude 에이전트 시스템

**Phase 1 (자동화):**
- Domain Generator - 도메인 보일러플레이트
- Test Generator - JUnit5/Mockito 테스트
- API Documenter - Swagger/OpenAPI 문서

**Phase 2 (검증):**
- Consistency Checker - 패키지 일관성
- Dependency Analyzer - 의존성 그래프

상세: `.claude/agents/{agent-name}/main-prompt.md`

---

## ⚠️ 필수 확인사항

기능 추가 전 반드시 이것들을 확인하세요:

1. **Git 워크플로우** - [`.claude/rules/git-workflow.md`](./.claude/rules/git-workflow.md) 읽기
2. **프로젝트 메모리** - [feature_branch_workflow.md](https://github.com/leejk0924/amazon2-backend) 참고
3. **커밋 확인** - 자동 커밋 금지, 반드시 사용자 승인 후 실행

---

**마지막 업데이트**: 2026-06-14  
**구조**: 프로젝트 루트 CLAUDE.md (이 파일) ← `.claude/` 참고 → 규칙 상세 관리
