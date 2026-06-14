# Git 워크플로우 (필수)

## 커밋 전 반드시 사용자 확인

모든 git commit 전에 **반드시** 사용자에게 확인받으세요:

```
내: "다음 파일들을 커밋하려고 합니다:
    - CLAUDE.md 수정
    - .env.example 추가
    
    커밋 메시지: 'docs: 설정 추가'
    
    이 내용으로 진행해도 될까요?"
    
당신: "네" 또는 "아니요, 이것만..." → 그 다음 커밋 실행
```

⚠️ **자동 커밋 금지!** 사용자 명시적 승인만

---

## Feature 브랜칭 전략

모든 기능 추가/버그 수정:

```bash
# 1️⃣ GitHub 이슈 생성
#    예: Issue #42 "포스팅 관련 API 구현"

# 2️⃣ Feature 브랜치 생성
git checkout -b feature/#42-포스팅-관련-api-구현

# 3️⃣ 작업 & 커밋
git add ...
git commit -m "feat: 포스팅 API 구현"

# 4️⃣ PR 생성
gh pr create --title "#42: 포스팅 관련 API 구현" \
  --body "Closes #42"
```

**규칙:**
- 형식: `feature/#<이슈번호>-<한글설명>`
- ❌ main/master 직접 수정 금지
- ✅ Git worktree 추천 (병렬 작업 안전성)
