---
name: git-workflow
description: Git 커밋 작성, feature 브랜치 생성, git worktree 설정이 필요할 때 사용합니다. 코드 변경을 커밋하려고 하거나, 새 기능/버그 수정 작업을 시작할 때 활성화하세요.
---

# Git 워크플로우

## 커밋 작성 원칙

사용자 승인 없이 커밋을 진행할 수 있습니다. 대신 다음 기준을 반드시 지킵니다:

- **기능별로 분리 커밋** — 서로 다른 목적의 변경은 하나로 묶지 말고 별도 커밋으로 작성
  - 예: "Skills 3개 추가"와 "이름 충돌 수정"은 별개 커밋
  - 예: 기능 구현과 오탈자/문서 수정은 별개 커밋
- 커밋 메시지는 CONTRIBUTING.md 타입 규칙 준수 (Feat/Fix/Refactor/Docs/Chore 등)
- 커밋 직후 변경 내역을 사용자에게 요약 보고 (파일 목록 + 커밋 메시지)
- 민감 정보(토큰, `.env` 등)가 포함된 파일은 절대 커밋하지 않음 — 의심되면 먼저 확인

## Feature 브랜칭 전략

1. GitHub 이슈 생성/확인 (`github-issue` skill 참고)
2. Feature 브랜치 생성: `git checkout -b feature/#<이슈번호>-<한글설명>`
3. 작업 & 기능별 분리 커밋
4. PR 생성: `gh pr create --title "#<이슈번호>: <설명>" --body "Closes #<이슈번호>"`

**규칙:**
- 브랜치명 형식: `feature/#<이슈번호>-<한글설명>`
- ❌ main/master 직접 수정 금지
- ✅ Git worktree 필수 — 병렬 작업 안전성 확보
- ⚠️ **worktree는 반드시 프로젝트 외부에 생성** — 프로젝트 내부에 생성 시 Gradle이 워크트리 build 디렉토리를 스캔해 중복 클래스 오류 발생

```bash
# ✅ 올바른 예 — 프로젝트 외부
git worktree add ~/worktrees/amazon2/feature/#42-포스팅-api -b feature/#42-포스팅-api

# ❌ 잘못된 예 — 프로젝트 내부 (.worktrees/)
git worktree add .worktrees/feature/#42-포스팅-api -b feature/#42-포스팅-api
```
