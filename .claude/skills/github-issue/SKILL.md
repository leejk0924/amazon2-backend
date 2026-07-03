---
name: github-issue
description: GitHub 이슈 조회, 생성, 이슈 기반 PR 생성 등 GitHub MCP 도구를 사용하는 작업에 사용합니다. 사용자가 이슈 번호(#42 등), "이슈 조회/해결/구현/생성해줘", "PR 만들어줘"라고 요청할 때 활성화하세요.
---

# GitHub 이슈/PR 자동화

Amazon2 프로젝트의 GitHub MCP Server(`amazon2-github-mcp`)를 통해 이슈 조회, 생성, PR 생성을 자동화합니다.

## 사용 가능한 도구

- `get_issue_details(issueNumber)` — 이슈 상세 조회 (title, body, labels, state)
- `create_issue_from_claude(title, body, issueType)` — 이슈 생성 (issueType: feature/bug/docs/refactor)
- `create_pr_from_issue(issueNumber, currentBranch)` — 이슈 기반 PR 생성, `Closes #XX` 자동 포함
- `parse_contributing_md()` — 커밋/브랜치/PR 템플릿 규칙 조회

## 자동 동작 규칙

1. **이슈 번호 감지 시 자동 조회** — "#14", "이슈 14" 언급 시 `get_issue_details` 자동 호출. 이미 조회해 제시한 이슈는 재조회 생략.
2. **작업 시작 시 워크플로우 제시** — 이슈 조회 후 "해결해줘/구현해줘/수정해줘"라고 하면:
   - Feature 브랜치명 제안: `feature/#[이슈번호]-[한글설명]`
   - Git worktree 사용 제안 (상세 규칙은 `git-workflow` skill 참고)
   - 작업 범위 명시 후 사용자 승인 대기 — 승인 전 작업 시작 금지
3. **이슈 생성 시 브랜치 자동 제안** — `create_issue_from_claude` 성공 시 생성된 이슈번호로 브랜치명 자동 제안
4. **이슈 조회 시 CONTRIBUTING.md 검증** — 이슈 본문이 타입별 템플릿(파일 경로, 구현 코드, 라벨)을 따르는지 확인, 미비 시 개선 권고
5. **PR 생성/검토 시 체크리스트 검증**:
   - 제목에 `#XX` 이슈번호 포함
   - 본문에 `Closes #XX` 포함
   - 체크리스트: 단위 테스트 작성, CI 테스트 통과, 코드 리뷰 요청, 문서 업데이트, 리그레션 확인

## 주의사항

- ❌ 사용자 확인 없이 이슈 생성/수정 금지
- ❌ Repository에 직접 push 금지 (항상 PR 사용)
- ❌ 여러 이슈를 한 브랜치에서 처리 금지
- MCP 연결 설정은 프로젝트 루트 `.mcp.json`에서 관리 (환경변수 `${GITHUB_TOKEN}`, `${REPOSITORY}` 참조)

## 참고 자료

초기 설정(Docker, 환경변수, `.mcp.json`), 연결 문제 진단, 전체 작업 흐름 예시가 필요하면 같은 디렉토리의 `reference.md`를 읽으세요.
