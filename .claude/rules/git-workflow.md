# Git 워크플로우 (필수)

## 커밋 작성 원칙

사용자 승인 없이 커밋을 진행할 수 있습니다. 대신 다음 기준을 반드시 지키세요:

- **기능별로 분리 커밋** — 서로 다른 목적의 변경은 하나로 묶지 말고 별도 커밋으로 작성
  - 예: 기능 구현과 문서/설정 수정은 별개 커밋
  - 예: 버그 수정과 리팩토링은 별개 커밋
- 커밋 메시지는 CONTRIBUTING.md 타입 규칙 준수 (Feat/Fix/Refactor/Docs/Chore 등)
- 커밋 직후 변경 내역(파일 목록 + 커밋 메시지)을 사용자에게 요약 보고
- 민감 정보(토큰, `.env` 등)가 포함된 파일은 절대 커밋하지 않음 — 의심되면 먼저 확인

---

## Feature 브랜칭 전략

모든 기능 추가/버그 수정:

```bash
# 1️⃣ GitHub 이슈 생성
#    예: Issue #42 "포스팅 관련 API 구현"

# 2️⃣ Feature 브랜치 생성
git checkout -b feature/#42-포스팅-관련-api-구현

# 3️⃣ 작업 & 기능별 분리 커밋
git add ...
git commit -m "feat: 포스팅 API 구현"

# 4️⃣ PR 생성
gh pr create --title "#42: 포스팅 관련 API 구현" \
  --body "Closes #42"
```

**규칙:**
- 형식: `feature/#<이슈번호>-<한글설명>`
- ❌ main/master 직접 수정 금지
- ✅ Git worktree 필수 (병렬 작업 안전성)
- ⚠️ **worktree는 반드시 프로젝트 외부에 생성** — 프로젝트 내부에 생성 시 Gradle이 워크트리 build 디렉토리를 스캔해 중복 클래스 오류 발생

```bash
# ✅ 올바른 예 — 프로젝트 외부
git worktree add ~/worktrees/amazon2/feature/#42-포스팅-api -b feature/#42-포스팅-api

# ❌ 잘못된 예 — 프로젝트 내부 (.worktrees/)
git worktree add .worktrees/feature/#42-포스팅-api -b feature/#42-포스팅-api
```
