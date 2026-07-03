---
name: git-workflow
description: Git 커밋 전 사용자 확인, feature 브랜치 생성, git worktree 설정이 필요할 때 사용합니다. 코드 변경을 커밋하려고 하거나, 새 기능/버그 수정 작업을 시작할 때 활성화하세요.
---

# Git 워크플로우

## 커밋 전 사용자 확인 (필수)

모든 `git commit` 전에 **반드시** 사용자에게 확인받습니다:
- 커밋할 파일 목록 제시
- 커밋 메시지 초안 제시
- "이 내용으로 진행해도 될까요?" 승인 후에만 커밋 실행

⚠️ 자동 커밋 금지 — 사용자 명시적 승인만

## Feature 브랜칭 전략

1. GitHub 이슈 생성/확인 (`github-issue` skill 참고)
2. Feature 브랜치 생성: `git checkout -b feature/#<이슈번호>-<한글설명>`
3. 작업 & 커밋 (사용자 확인 후)
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
