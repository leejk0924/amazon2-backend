# Git MCP Server 사용 가이드

Amazon2 백엔드 프로젝트를 위한 GitHub MCP 서버 활용 방법입니다.

## 📋 목차

1. [빠른 시작](#빠른-시작)
2. [Claude Code 설정](#claude-code-설정)
3. [MCP 도구 사용법](#mcp-도구-사용법)
4. [워크플로우 예시](#워크플로우-예시)
5. [트러블슈팅](#트러블슈팅)
6. [FAQ](#faq)

---

## 빠른 시작

### 1단계: 환경 설정

**.env 파일 위치:** 📍 **프로젝트 루트** (`/Users/jk/.../amazon2-backend/.env`)

```bash
# 프로젝트 루트에서 .env 생성
cat > .env << 'EOF'
# GitHub API 토큰
GITHUB_TOKEN=ghp_xxxxxxxxxxxxxxxxxxxx

# 저장소 정보
GITHUB_OWNER=leejk0924
GITHUB_REPO=amazon2-backend
REPOSITORY=leejk0924/amazon2-backend

# MCP 서버 설정
MCP_PORT=3000
LOG_LEVEL=info
NODE_ENV=development
EOF
```

**파일 구조:**
```
amazon2-backend/                    ← 프로젝트 루트
├── .env                            ← ⭐ 여기에 생성 (민감 정보)
├── docker-compose.yml              ← .env를 읽음
├── mcp-wrapper/
│   ├── package.json
│   └── src/
└── docs/
    └── CONTRIBUTING.md
```

**GitHub Token 생성:**
1. GitHub 계정 → Settings → Developer settings → Personal access tokens
2. Generate new token (classic)
3. 권한 선택:
   - ✅ `repo` - 저장소 접근
   - ✅ `issues` - 이슈 관리
   - ✅ `pull_requests` - PR 관리
4. Token 복사 → `.env` 파일의 `GITHUB_TOKEN` 값에 붙여넣기

⚠️ **.env는 Git에 커밋되지 않음** (`.gitignore`에 추가됨)

### 2단계: MCP 서버 시작

```bash
# 프로젝트 루트에서
docker-compose up -d

# 확인
docker-compose ps

# 로그 확인
docker-compose logs -f github-mcp-wrapper
```

**예상 출력:**
```
amazon2-git-mcp-wrapper | [INFO] ... - Amazon2 Git MCP Server 시작
amazon2-git-mcp-wrapper | [INFO] ... - 저장소: leejk0924/amazon2-backend
amazon2-git-mcp-wrapper | [INFO] ... - 등록된 도구: 4개
```

**Docker 구성 상세:**

```yaml
# docker-compose.yml의 MCP 서버 설정
github-mcp-wrapper:
  build: ./mcp-wrapper
  stdin_open: true    # ← stdio 기반 MCP 프로토콜 지원 (CRITICAL)
  tty: true           # ← 프로세스 지속 실행 (프롬프트 입력 대기)
  environment:
    GITHUB_TOKEN: ${GITHUB_TOKEN}
    REPOSITORY: ${REPOSITORY}
    LOG_LEVEL: info
    CONTRIBUTING_MD_PATH: /app/docs/CONTRIBUTING.md
  volumes:
    - ./docs/CONTRIBUTING.md:/app/docs/CONTRIBUTING.md:ro  # 읽기 전용 마운트
```

**설정 의미:**

| 설정 | 목적 | 이유 |
|------|------|------|
| `stdin_open: true` | Docker 컨테이너 stdin 열기 | MCP는 stdio 기반 프로토콜. stdin 없으면 즉시 종료됨 |
| `tty: true` | TTY(터미널) 할당 | 프로세스가 입력 대기 상태 유지. 일반적 프로세스 종료 방지 |
| `volumes` | CONTRIBUTING.md 마운트 | 호스트의 규칙 파일을 컨테이너에서 실시간 읽음 |
| `depends_on` | github-mcp 의존성 | MCP Wrapper가 GitHub MCP를 통해 API 호출 |

### 3단계: Claude Code 설정

**Claude Code (claude.ai/code):**

1. Settings → MCP Servers
2. Add MCP Server
3. 다음 정보 입력:
   - **Name**: Amazon2 Git MCP
   - **Type**: Stdio
   - **Command**: `curl http://localhost:3000`
   - (또는 Docker 컨테이너 직접 연결)

**Claude Code CLI:**
```bash
# ~/.claude/settings.json 또는 ~/.claude/settings.local.json에 추가:
{
  "mcpServers": {
    "amazon2-git": {
      "command": "docker",
      "args": ["exec", "amazon2-git-mcp-wrapper", "npm", "start"],
      "disabled": false
    }
  }
}
```

---

## Claude Code 설정

### MCP 서버 연결 확인

Claude Code에서:

```
당신: MCP 도구들을 확인해줄 수 있나?
Claude: [MCP Tool] parse_contributing_md
→ 성공하면 CONTRIBUTING.md 규칙 반환
```

### 사용 가능한 도구

```
parse_contributing_md        - CONTRIBUTING.md 규칙 조회
create_pr_from_issue         - 이슈 기반 PR 생성
create_issue_from_claude     - Claude Code에서 이슈 생성
get_issue_details            - 이슈 상세 정보 조회
```

---

## MCP 도구 사용법

### 1️⃣ `create_pr_from_issue` - 이슈 기반 PR 생성

**목적**: 이슈 번호를 입력하면 CONTRIBUTING.md 규칙을 자동 적용하여 PR을 생성합니다.

**사용 예시:**

```
당신: 이슈 #42를 기반으로 PR을 생성해줘. 현재 main 브랜치야.

Claude: [MCP Tool] create_pr_from_issue
  issueNumber: 42
  currentBranch: main

→ 응답:
{
  "prNumber": 125,
  "prUrl": "https://github.com/jk/amazon2-backend/pull/125",
  "branchName": "feature/#42-사용자-인증-구현",
  "title": "#42: 사용자 인증 구현"
}
```

**자동 생성되는 내용:**

- **Branch명**: `feature/#42-사용자-인증-구현`
  - 규칙: `<type>/#<issue>-<description>` (CONTRIBUTING.md 준수)
  
- **PR 제목**: `#42: 사용자 인증 구현`
  
- **PR 설명**:
  ```
  ## 개요
  [이슈 본문이 자동으로 들어옴]
  
  ## 변경사항
  - 이슈 #42의 구현 내용
  
  ## 테스트 방법
  - 해당 기능을 테스트했습니다
  
  ## 관련 이슈
  Closes #42
  ```

### 2️⃣ `create_issue_from_claude` - Claude Code에서 이슈 생성

**목적**: 개발 중 발견한 버그나 필요한 기능을 자동으로 GitHub 이슈로 등록합니다.

**사용 예시:**

```
당신: 데이터 검증 로직이 빠진 것 같아. 이슈로 등록해줄래?

Claude: [MCP Tool] create_issue_from_claude
  title: "데이터 검증 로직 추가"
  body: "회원 정보 저장 시 필수 필드 검증이 필요합니다."
  issueType: "bug"

→ 응답:
{
  "issueNumber": 99,
  "issueUrl": "https://github.com/jk/amazon2-backend/issues/99",
  "labels": ["claude-code-generated", "bug"]
}
```

**자동으로 추가되는 라벨:**

- `claude-code-generated` - Claude Code에서 자동 생성됨 (구분 목적)
- `bug` / `feature` / `docs` / `refactor` - 지정한 issueType

**issueType 선택 가이드:**

| Type | 사용 예 |
|------|--------|
| `feature` | 새 기능 필요 |
| `bug` | 버그 발견 |
| `docs` | 문서화 필요 |
| `refactor` | 코드 개선 필요 |

### 3️⃣ `get_issue_details` - 이슈 정보 조회

**목적**: 특정 이슈의 상세 정보를 조회합니다.

**사용 예시:**

```
당신: 이슈 #42의 상세 정보를 보여줄 수 있나?

Claude: [MCP Tool] get_issue_details
  issueNumber: 42

→ 응답:
{
  "number": 42,
  "title": "사용자 인증 구현",
  "body": "회원가입 및 로그인 기능 구현 필요...",
  "labels": ["feature", "priority-high"],
  "state": "open",
  "created_at": "2026-06-10T10:30:00Z",
  "updated_at": "2026-06-13T15:45:00Z",
  "html_url": "https://github.com/jk/amazon2-backend/issues/42"
}
```

### 4️⃣ `parse_contributing_md` - 프로젝트 규칙 조회

**목적**: 현재 프로젝트의 CONTRIBUTING.md 규칙을 조회합니다.

**사용 예시:**

```
당신: 프로젝트의 커밋 규칙이 뭐야?

Claude: [MCP Tool] parse_contributing_md

→ 응답:
{
  "commitTypes": [
    { "type": "Feat", "description": "새 기능 추가", "example": "Feat: 예시" },
    { "type": "Fix", "description": "버그 수정", "example": "Fix: 예시" },
    ...
  ],
  "branchPattern": {
    "format": "feature/#<issue>-<description>",
    "types": ["feature", "fix", "docs", "refactor"]
  },
  "prTemplate": "[PR 템플릿이 들어옴]"
}
```

---

## 워크플로우 예시

### 시나리오 1: 이슈 기반 개발

```
1️⃣ GitHub에서 이슈 생성
   → #42: 사용자 인증 구현

2️⃣ Claude Code에서 작업 시작
   당신: "이슈 #42를 기반으로 PR을 생성해줘. 현재 main 이야."
   Claude: [MCP Tool] create_pr_from_issue
   
3️⃣ 자동으로 생성된 내용
   - Branch: feature/#42-사용자-인증-구현
   - PR #125 생성
   - PR 제목/설명 자동 작성

4️⃣ 개발 완료 후
   - 로컬에서 feature/#42-... 브랜치에서 개발
   - 커밋 규칙 준수 (CONTRIBUTING.md 참고)
   - Git push → PR 리뷰
   - Merge → 이슈 자동 종료 (Closes #42)
```

### 시나리오 2: 개발 중 버그 발견

```
1️⃣ 개발 중 문제 발견
   당신: "데이터 검증이 없는 것 같아. 이슈로 등록해줄래?"

2️⃣ Claude Code가 이슈 생성
   Claude: [MCP Tool] create_issue_from_claude
   → 이슈 #100 생성 (라벨: claude-code-generated, bug)

3️⃣ 우선순위 설정 및 개발
   - GitHub에서 이슈 #100에 라벨 추가 (priority 등)
   - 다른 개발자가 처리하거나 나중에 처리

4️⃣ 완료 후
   - 이슈 #100 기반 PR 생성
   - 머지 시 Closes #100으로 자동 종료
```

### 시나리오 3: PR 생성 워크플로우

```
당신: 
  "이슈 #42를 구현했어. 지금 feature/#42-... 브랜치에 커밋이 있어. 
   main으로 PR을 생성해줄 수 있나?"

Claude:
  1. [MCP Tool] get_issue_details(42) → 이슈 정보 조회
  2. [MCP Tool] create_pr_from_issue(42, main) → PR 생성
  
결과:
  ✅ PR #125 생성
  - Branch: feature/#42-사용자-인증-구현
  - 제목: #42: 사용자 인증 구현
  - 설명: CONTRIBUTING.md 템플릿 자동 적용
  - 라벨: feature #42 관련 라벨 자동 포함
  
다음 단계:
  → GitHub에서 PR 검토
  → Merge 후 이슈 자동 종료
```

---

## 트러블슈팅

### MCP 서버가 시작되지 않음

**확인 사항:**

```bash
# 1. 환경 변수 확인
cat .env | grep GITHUB_TOKEN
# → "ghp_"로 시작하는 유효한 토큰이 있는가?

# 2. Docker 상태 확인
docker-compose ps
# → github-mcp-wrapper가 "Up" 상태인가?

# 3. 로그 확인
docker-compose logs github-mcp-wrapper | tail -30
# → 에러 메시지가 있는가?
```

**해결 방법:**

| 에러 | 원인 | 해결책 |
|------|------|--------|
| `필수 환경 변수 누락` | GITHUB_TOKEN 또는 REPOSITORY 미설정 | .env 파일 확인 및 설정 |
| `GITHUB_TOKEN을 읽을 수 없습니다` | Token 형식 오류 | `ghp_`로 시작하는지 확인 |
| `401 Unauthorized` | Token 권한 부족 | GitHub에서 Token 권한 확인 |
| `Cannot find module` | 의존성 누락 | `docker-compose build --no-cache` 실행 |
| 컨테이너가 즉시 종료됨 (Exit 0) | `stdin_open`, `tty` 미설정 | docker-compose.yml에서 두 설정 추가 확인 |

**Docker 설정 확인:**

```bash
# docker-compose.yml에서 다음이 있는지 확인
github-mcp-wrapper:
  stdin_open: true   # ← 반드시 필요
  tty: true          # ← 반드시 필요
```

⚠️ 이 두 설정이 없으면 MCP stdio 프로토콜이 EOF를 받아 즉시 종료됩니다.

### Claude Code에서 도구가 보이지 않음

```bash
# 1. MCP 서버 상태 확인
curl http://localhost:3000/tools
# → 도구 목록이 반환되는가?

# 2. Claude Code MCP 설정 재확인
# Settings → MCP Servers → 올바른 주소 확인

# 3. 재시작
docker-compose down && docker-compose up -d
```

### PR 생성 실패

**에러: "이슈를 찾을 수 없습니다"**
```
→ issueNumber를 확인하세요 (GitHub의 실제 이슈 번호)
```

**에러: "이미 존재하는 branch"**
```
→ 다른 branch 이름을 사용하거나 기존 branch 삭제
```

**에러: "Token 권한 부족"**
```bash
# GitHub Token의 권한 확인
# Settings → Personal access tokens → [토큰명]
# ✅ repo (또는 public_repo)
# ✅ issues
# ✅ pull_requests
```

---

## FAQ

### Q1: 기존 이슈를 기반으로 PR을 만들면 어떻게 되나?

**A:** 다음이 자동으로 생성됩니다:

1. **Branch명** - CONTRIBUTING.md 규칙 적용
   - 예: `feature/#42-사용자-인증-구현`
   
2. **PR 제목** - `#이슈번호: 이슈제목`
   - 예: `#42: 사용자 인증 구현`
   
3. **PR 설명** - 템플릿 + 이슈 정보
   ```
   ## 개요
   [이슈 본문]
   
   ## 변경사항
   - 이슈 #42의 구현 내용
   
   ...
   
   ## 관련 이슈
   Closes #42  ← 머지 시 자동 종료!
   ```

### Q2: 여러 이슈를 한 번에 처리할 수 있나?

**A:** 아니요, 한 PR은 한 이슈를 기반으로 생성됩니다. 
- 여러 이슈를 처리하려면 각각 PR을 생성하세요
- 또는 별도 PR에서 여러 이슈 참고 가능: `Closes #42, fixes #100`

### Q3: Claude Code에서 생성한 이슈의 라벨은?

**A:** 두 개가 자동 추가됩니다:

1. **`claude-code-generated`** - Claude Code에서 생성됨을 표시
   - 필터링: `label:claude-code-generated`
   - 다른 자동화와 구분 가능

2. **`<issueType>`** - 지정한 타입에 따라
   - `bug`, `feature`, `docs`, `refactor` 중 하나

### Q4: 매번 CONTRIBUTING.md 규칙을 확인해야 하나?

**A:** 아니요! MCP 서버가 자동으로 적용합니다:

```
당신: 프로젝트 규칙이 뭐야?
Claude: [MCP Tool] parse_contributing_md
→ 현재 CONTRIBUTING.md의 규칙 자동 반환
```

커밋 규칙, branch 패턴 등이 변경되면 자동으로 반영됩니다.

### Q5: MCP 서버를 멈추려면?

```bash
# 종료
docker-compose down

# 로그 제거
docker-compose down -v

# 재시작
docker-compose up -d
```

### Q6: GitHub Token이 만료되면?

**A:** 새 Token 생성 후 `.env` 파일 업데이트:

```bash
# 1. GitHub에서 새 Token 생성
# 2. .env 파일 업데이트
GITHUB_TOKEN=ghp_newtoken...

# 3. 서버 재시작
docker-compose restart github-mcp-wrapper
```

### Q7: 실수로 이슈나 PR을 생성했으면?

**A:** GitHub에서 직접 삭제/수정하세요:

```
GitHub → [이슈/PR] → 우측 메뉴 → Delete draft / Close
```

MCP 서버는 생성만 하고 삭제는 지원하지 않습니다.

### Q8: 여러 저장소를 관리하려면?

**A:** 현재는 한 저장소만 지원합니다.

다른 저장소의 경우:
1. `.env` 파일에서 `REPOSITORY` 변경
2. 서버 재시작
```bash
REPOSITORY=other/repository
docker-compose restart github-mcp-wrapper
```

---

## 추가 참고자료

- [CONTRIBUTING.md](../CONTRIBUTING.md) - 프로젝트 규칙
- [mcp-wrapper/README.md](../mcp-wrapper/README.md) - MCP Wrapper 기술 문서
- [MCP 설계 문서](./superpowers/specs/2026-06-13-git-mcp-server-design.md) - 아키텍처
- [GitHub API 문서](https://docs.github.com/en/rest) - GitHub REST API

---

**마지막 업데이트**: 2026-06-13  
**MCP Server 버전**: 1.0.0
