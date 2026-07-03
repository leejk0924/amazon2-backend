# GitHub MCP 설정 상세 (github-issue skill 참고 자료)

`github-issue` skill의 도구/자동 동작 규칙 외에, 초기 설정이나 연결 문제 진단이 필요할 때 참고합니다.

## 필수 설정

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

### 3. Claude Code 설정 (`.mcp.json`)

MCP 서버 설정은 `.mcp.json` (프로젝트 루트)에서 관리합니다.
`${GITHUB_TOKEN}`은 shell 환경변수로 해석되므로 Claude가 `.env`를 직접 읽지 않습니다.

```json
// .mcp.json (프로젝트 루트)
{
  "mcpServers": {
    "amazon2-github-mcp": {
      "command": "docker",
      "args": ["exec", "-i", "amazon2-git-mcp-wrapper", "node", "dist/index.js"],
      "env": {
        "GITHUB_TOKEN": "${GITHUB_TOKEN}",
        "REPOSITORY": "${REPOSITORY}"
      }
    }
  }
}
```

> ⚠️ `settings.json`에 `mcpServers`를 넣지 마세요. `.mcp.json`이 MCP 전용 파일입니다.

## 작업 흐름 예시

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
   - 기능별 분리 커밋

5️⃣ 완료 후 PR 생성
   당신: "완료했어, PR 만들어줄래?"
   Claude: [create_pr_from_issue 호출]
   → PR #[번호] 생성
   → Closes #14 자동 포함
```

## 관련 문서

- [CONTRIBUTING.md](../../../docs/CONTRIBUTING.md) - 커밋/이슈/PR 작성 규칙
  - Section 3: 이슈 작성 (타입별 템플릿)
  - Section 4: PR 작성 (체크리스트)
- [프로젝트 메모리: GitHub 이슈/PR 가이드](../../projects/amazon2-backend/memory/github_issue_pr_guide.md)

---

**마지막 업데이트**: 2026-07-04
