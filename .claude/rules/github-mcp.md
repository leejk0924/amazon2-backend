# GitHub MCP 자동화 규칙

GitHub MCP Server를 통한 이슈 조회 및 자동화 워크플로우입니다.

---

## 🔄 자동 워크플로우

사용자가 이슈 관련 질문을 하면 다음과 같이 자동 동작합니다:

### 예시 1: 이슈 조회 요청

```
사용자: "이슈 #14 해결해줘"
↓
Claude (자동):
  1. GitHub API로 이슈 #14 조회
  2. 이슈 제목, 본문, 라벨, 상태 확인
  3. 사용자에게 이슈 내용 제시
  4. 작업 시작 여부 확인
```

### 예시 2: 이슈 생성 요청

```
사용자: "회원 검증 로직 버그를 이슈로 등록해줘"
↓
Claude (자동):
  1. GitHub MCP의 create_issue_from_claude 도구 사용
  2. 이슈 생성 (제목, 설명, 라벨)
  3. 생성된 이슈 번호 반환
  4. feature/#[생성된번호]-... 브랜치 생성 제안
```

---

## 📋 사용 가능한 GitHub MCP 도구

### 1. `get_issue_details` - 이슈 조회
```
입력: issueNumber (예: 14)
출력: { number, title, body, labels, state, ... }
```
**사용 시기**: "이슈 #14가 뭐야?", "이슈 #42 보여줘" 등

### 2. `create_issue_from_claude` - 이슈 생성
```
입력: title, body, issueType (feature/bug/docs/refactor)
출력: { issueNumber, issueUrl, labels }
```
**사용 시기**: "이 버그를 이슈로 등록해줄래?", "기능 개선 이슈 만들어줘" 등

### 3. `create_pr_from_issue` - 이슈 기반 PR 생성
```
입력: issueNumber, currentBranch
출력: { prNumber, prUrl, branchName, title }
```
**사용 시기**: "이슈 #14 기반으로 PR 생성해줘", "PR 만들어줄래?" 등

### 4. `parse_contributing_md` - 프로젝트 규칙 조회
```
출력: { commitTypes, branchPattern, prTemplate }
```
**사용 시기**: "커밋 규칙이 뭐야?", "브랜치 이름 규칙 확인해줘" 등

---

## ✅ 자동 동작 규칙

### Rule 1: 이슈 번호 감지 시 자동 조회
```
트리거: 사용자가 "#14", "이슈 14", "issue 14" 등을 언급
동작: 자동으로 get_issue_details 호출
예외: 이미 이슈 내용을 제시한 경우는 생략
```

### Rule 2: 작업 시작 시 자동 workflow 제시
```
트리거: 이슈 조회 후 "해결해줘", "구현해줘", "수정해줘" 등
동작:
  1. Feature 브랜치 이름 제안: feature/#[이슈번호]-[한글설명]
  2. Git worktree 사용 제안
  3. 작업 범위 명시
  4. 사용자 확인 후 시작
```

### Rule 3: 이슈 생성 시 자동 브랜치 제안
```
트리거: GitHub MCP로 이슈 생성 성공
동작:
  1. 생성된 이슈 번호 확인
  2. feature/#[이슈번호]-... 브랜치명 자동 생성
  3. Git worktree 설정 제안
```

### Rule 4: 이슈 조회 시 CONTRIBUTING.md 검증
```
트리거: 이슈 조회 후 내용 제시
동작:
  1. 이슈가 CONTRIBUTING.md#3 템플릿 준수 확인
  2. 문제 발견 시 지적:
     - ❌ 파일 경로 명시되었는가?
     - ❌ 구현 코드가 상세히 작성되었는가?
     - ❌ 타입별 템플릿을 따랐는가?
     - ❌ 라벨이 추가되었는가?
  3. 개선 권고사항 제시
```

### Rule 5: PR 생성 시 체크리스트 검증
```
트리거: PR 생성 또는 검토 요청
동작:
  1. PR 제목에 이슈번호 포함 확인 (#XX 형식)
  2. PR 본문의 체크리스트 확인:
     - [ ] 단위 테스트 작성
     - [ ] CI 테스트 통과
     - [ ] 코드 리뷰 요청
     - [ ] 문서 업데이트
     - [ ] 리그레션 확인
  3. "Closes #XX" 문법 포함 확인
  4. 미충족 항목 지적 및 완료 유도
```

---

## 🛠️ 필수 설정

### 1. 환경 변수 (.env)
```bash
GITHUB_TOKEN=ghp_your_token_here
REPOSITORY=leejk0924/amazon2-backend
GITHUB_OWNER=leejk0924
GITHUB_REPO=amazon2-backend
```

### 2. Docker Compose
```bash
# GitHub MCP 서버 실행
docker-compose up -d github-mcp github-mcp-wrapper

# 상태 확인
docker-compose ps
docker-compose logs github-mcp-wrapper
```

### 3. Claude Code 설정
```json
// .claude/settings.local.json 참고
{
  "mcpServers": {
    "amazon2-github-mcp": {
      "command": "docker",
      "args": ["exec", "amazon2-github-mcp", "mcp"],
      "disabled": false
    }
  }
}
```

---

## 📝 작업 흐름 예시

### 시나리오: 이슈 #14 해결

```
1️⃣ 사용자 요청
   당신: "이슈 #14 해결해줘"

2️⃣ Claude 자동 조회
   [GitHub API 호출]
   → 이슈 제목: "[Architecture] MonitoringController..."
   → 문제점, 해결 방안 제시

3️⃣ 작업 계획 제시
   "다음과 같이 진행할까요?
   - 브랜치: feature/#14-monitoring-controller-service-분리
   - Git worktree 사용
   - MonitoringService 생성 → MonitoringController 수정
   
   진행할까요?"

4️⃣ 사용자 승인 후 시작
   당신: "네"
   [작업 시작]
   - Git worktree 생성
   - Feature 브랜치 생성
   - 코드 구현
   - 테스트 작성
   - 커밋 (사용자 확인 후)

5️⃣ 완료 후 PR 생성
   당신: "완료했어, PR 만들어줄래?"
   Claude: [create_pr_from_issue 호출]
   → PR #[번호] 생성
   → Closes #14 자동 포함
```

---

## 🚨 주의사항

### ❌ 하지 말 것
1. 사용자 확인 없이 이슈 생성/수정
2. 커밋 메시지 규칙 무시
3. Repository에 직접 push (항상 PR 사용)
4. 여러 이슈를 한 branch에서 처리

### ✅ 반드시 할 것
1. 이슈 조회 후 요약 제시
2. 작업 시작 전 사용자 승인
3. Git worktree로 독립 환경 구성
4. 커밋 전 반드시 사용자 확인
5. 브랜치명: `feature/#[이슈번호]-[한글설명]`

---

## 🔗 관련 문서

- [Git 워크플로우](./git-workflow.md) - Feature 브랜칭 규칙
- [CONTRIBUTING.md](../../docs/CONTRIBUTING.md) - 커밋/이슈/PR 작성 규칙
  - Section 3: 이슈 작성 (타입별 템플릿)
  - Section 4: PR 작성 (체크리스트)
- [프로젝트 메모리: GitHub 이슈/PR 가이드](./../projects/amazon2-backend/memory/github_issue_pr_guide.md)

---

**마지막 업데이트**: 2026-06-15

### 최근 변경사항

- ✅ Rule 4: 이슈 조회 시 CONTRIBUTING.md 검증 추가
- ✅ Rule 5: PR 생성 시 체크리스트 검증 추가
- ✅ CONTRIBUTING.md 섹션 3, 4 개선 완료
- ✅ 프로젝트 메모리 github_issue_pr_guide.md 추가