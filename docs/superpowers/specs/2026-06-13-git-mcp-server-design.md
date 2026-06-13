# Git MCP Server 설계 문서

**작성일:** 2026-06-13  
**상태:** 설계 승인됨  
**대상:** amazon2-backend 저장소  

---

## 1. 개요

GitHub의 공식 MCP 서버(github-mcp-server)를 기반으로, CONTRIBUTING.md 규칙을 자동 적용하는 Node.js Wrapper를 amazon2-backend 프로젝트에 **통합**하여 구축합니다.

### 특징
- **완전 통합**: `docker-compose up` 한 번으로 모든 개발 환경 준비
- **자동 규칙 적용**: CONTRIBUTING.md 규칙이 MCP에 자동 반영
- **프로젝트 자급자족**: 모든 개발 도구가 프로젝트에 포함

### 기능
Claude Code에서 다음이 가능해집니다:
- PR 자동 생성 (이슈 기반)
- 이슈 자동 생성 (Claude Code 라벨 포함)
- 이슈 조회
- CONTRIBUTING.md 규칙 자동 적용 (branch명, PR 제목/설명)

**핵심 목표:** amazon2-backend 개발 워크플로우 자동화 (통합 MCP 환경)

---

## 2. 아키텍처

### 2.1 시스템 구성

```
Claude Code
    ↓ (MCP 요청)
┌────────────────────────────────────────────┐
│  amazon2-backend Docker Compose            │
├────────────────────────────────────────────┤
│ • Spring Boot App (port 8080)              │
│ • MySQL (port 3306)                        │
│ • Node.js MCP Wrapper (port 3000) ✨ 신규  │
│ • GitHub MCP Server (port 3001) ✨ 신규    │
└────────────┬───────────────────────────────┘
             ↓ (GitHub API)
         GitHub API
```

### 2.2 컴포넌트 역할

**Node.js MCP Wrapper (Custom, 프로젝트 통합)**
- CONTRIBUTING.md 규칙 파싱 및 적용
- Branch명 생성 (feature/#이슈-설명)
- PR 제목/설명 자동 생성
- 이슈 자동 생성 (Claude Code 라벨)
- MCP 도구로 변환

**GitHub MCP Server (공식)**
- GitHub REST/GraphQL API 처리
- PR 생성/수정
- 이슈 생성/수정
- 저장소 정보 조회

**Spring Boot & MySQL**
- 기존 amazon2-backend 서비스
- Wrapper와 함께 실행

---

## 3. MCP 도구 정의

### 3.1 Wrapper에서 제공하는 도구

#### `create_pr_from_issue`
이슈 번호를 입력받아 PR을 자동 생성합니다.

```
입력:
- issueNumber: 이슈 번호 (예: 5)
- currentBranch: 현재 branch (예: main)

처리:
1. GitHub에서 이슈 정보 조회
2. CONTRIBUTING.md 규칙 적용
   - branch명: feature/#5-회원-조회-api-구현
   - PR 제목: #5: 회원 조회 API 구현
   - PR 설명: [PR 템플릿 자동 생성]
3. github-mcp-server를 통해 PR 생성

출력:
- PR 번호
- PR URL
- Branch명 (생성 가능한 상태)
```

#### `create_issue_from_claude`
Claude Code에서 발견한 버그/기능을 이슈로 생성합니다.

```
입력:
- title: 이슈 제목
- body: 이슈 본문
- issueType: "feature" | "bug" | "docs" | "refactor"

처리:
1. 제목 길이 검증 (50자 이내)
2. Claude Code 라벨 자동 추가
3. 이슈 타입별 기본 레이블 추가
4. github-mcp-server를 통해 이슈 생성

출력:
- 이슈 번호
- 이슈 URL
- 추가된 라벨
```

#### `get_issue_details`
이슈 번호로 상세 정보를 조회합니다.

```
입력:
- issueNumber: 이슈 번호

출력:
- 이슈 제목, 본문, 라벨, 마일스톤
- 상태 (open/closed)
- 생성자, 담당자
```

#### `parse_contributing_md`
CONTRIBUTING.md에서 규칙을 파싱합니다.

```
출력:
- 커밋 타입 목록
- Branch 네이밍 패턴
- PR 제목 형식
- PR 설명 템플릿
```

### 3.2 GitHub MCP Server 도구 (그대로 사용)

- `create_pull_request()`
- `update_pull_request()`
- `create_issue()`
- `update_issue()`
- `list_issues()`
- (등 100+ 도구)

---

## 4. 데이터 흐름

### 4.1 PR 생성 흐름 (이슈 기반)

```
Claude Code:
  "이슈 #5를 기반으로 PR을 생성해줘"
    ↓
Node.js Wrapper - create_pr_from_issue:
  1. get_issue_details(#5) 호출
     → {title: "회원 조회 API 구현", body: "...", labels: [...]}
  
  2. CONTRIBUTING.md 규칙 적용
     - branch명: feature/#5-회원-조회-api-구현
     - PR 제목: #5: 회원 조회 API 구현
     - PR 설명: [템플릿 생성]
       ```
       ## 개요
       이슈 #5의 구현 내용입니다.
       
       ## 변경사항
       - [이슈 #5 본문에서 추출]
       
       ## 관련 이슈
       Closes #5
       ```
  
  3. github-mcp-server의 create_pull_request() 호출
     → PR #N 생성
    ↓
GitHub:
  PR #N 생성 완료
  (claude code에서 생성된 PR로 표시 가능)
```

### 4.2 이슈 생성 흐름 (Claude Code 감지)

```
Claude Code:
  "데이터 검증 로직이 누락된 것 같아. 이슈로 등록해줘"
    ↓
Node.js Wrapper - create_issue_from_claude:
  1. 제목 검증
  2. 라벨 설정
     - "claude-code-generated" (필수)
     - "bug" 또는 "feature" (타입별)
  3. github-mcp-server의 create_issue() 호출
    ↓
GitHub:
  이슈 #N 생성 완료
  (라벨: "claude-code-generated", "bug")
```

---

## 5. Docker 구성

### 5.1 docker-compose.yml (기존 파일에 추가)

기존 `docker-compose.yml`의 `services:` 섹션에 다음 서비스 추가:

```yaml
version: '3.8'

services:
  # 기존 서비스들 (Spring Boot, MySQL 등)
  app:
    # ... 기존 설정
    depends_on:
      - db
      - github-mcp  # ✨ 추가
  
  db:
    # ... 기존 설정
  
  # ✨ 새로 추가: Node.js MCP Wrapper
  github-mcp-wrapper:
    build:
      context: ./mcp-wrapper
      dockerfile: Dockerfile
    container_name: amazon2-git-mcp-wrapper
    ports:
      - "3000:3000"
    environment:
      GITHUB_TOKEN: ${GITHUB_TOKEN}
      REPOSITORY: ${REPOSITORY}
      MCP_IMPLEMENTATION: stdio
      LOG_LEVEL: info
      CONTRIBUTING_MD_PATH: /app/docs/CONTRIBUTING.md
    volumes:
      - ./docs/CONTRIBUTING.md:/app/docs/CONTRIBUTING.md:ro
    depends_on:
      - github-mcp
    restart: unless-stopped

  # ✨ 새로 추가: GitHub MCP Server (공식)
  github-mcp:
    image: ghcr.io/github/github-mcp-server:latest
    container_name: amazon2-github-mcp
    ports:
      - "3001:3001"
    environment:
      GITHUB_PERSONAL_ACCESS_TOKEN: ${GITHUB_TOKEN}
      GITHUB_TOOLSETS: "issues,pull_requests"
      GITHUB_READ_ONLY: "false"
      LOG_LEVEL: info
    restart: unless-stopped
```

### 5.2 .env 파일 (기존 파일에 추가)

기존 `.env` 파일에 다음 항목 추가:

```bash
# GitHub MCP 설정 (신규)
GITHUB_TOKEN=ghp_xxxx...
REPOSITORY=jk/amazon2-backend
```

### 5.3 디렉토리 구조

```
amazon2-backend/
├── docker-compose.yml (기존 파일 수정)
├── .env (기존 파일 수정)
├── mcp-wrapper/  ✨ 신규 폴더
│   ├── Dockerfile
│   ├── package.json
│   ├── tsconfig.json
│   ├── src/
│   │   ├── index.ts
│   │   ├── tools/
│   │   ├── services/
│   │   └── types.ts
│   ├── tests/
│   └── .env.example
├── src/
├── docs/
│   ├── CONTRIBUTING.md (기존)
│   └── superpowers/specs/
│       └── 2026-06-13-git-mcp-server-design.md
└── ...
```

---

## 6. Node.js MCP Wrapper 구조

### 6.1 디렉토리 구조 (amazon2-backend 프로젝트 내)

```
amazon2-backend/mcp-wrapper/
├── Dockerfile
├── package.json
├── tsconfig.json
├── .env.example
├── .dockerignore
├── src/
│   ├── index.ts                    # MCP 서버 진입점
│   ├── tools/
│   │   ├── pr-tools.ts             # PR 생성 관련 도구
│   │   ├── issue-tools.ts          # 이슈 생성/조회 도구
│   │   └── contributing-tools.ts   # CONTRIBUTING.md 파싱 도구
│   ├── services/
│   │   ├── github-client.ts        # GitHub API 호출
│   │   ├── contributing-parser.ts  # CONTRIBUTING.md 파싱
│   │   └── formatter.ts            # 형식 변환 (규칙 적용)
│   ├── types.ts                    # TypeScript 타입 정의
│   └── utils/
│       └── logger.ts               # 로깅
├── tests/
│   ├── contributing-parser.test.ts
│   ├── formatter.test.ts
│   └── tools.test.ts
└── README.md                       # Wrapper 사용 가이드
```

### 6.2 핵심 서비스

**contributing-parser.ts**
- CONTRIBUTING.md 파싱 (로컬 마운트된 파일에서)
- 커밋 타입, branch 패턴, PR 템플릿 추출
- 정규표현식으로 규칙 검증

**formatter.ts**
- 이슈 정보 → branch명 생성 (feature/#5-회원-조회-api-구현)
- 이슈 정보 → PR 제목 생성 (#5: 회원 조회 API 구현)
- 이슈 정보 → PR 설명 생성 (PR 템플릿 적용)

**github-client.ts**
- 공식 github-mcp-server와 HTTP 통신
- MCP Tool 호출 (create_issue, create_pull_request 등)

**index.ts**
- MCP 서버 초기화
- 모든 도구 등록
- stdio를 통한 Claude Code와 통신

---

## 7. 사용 예시

### 7.1 PR 자동 생성

```
Claude Code 사용자:
  "이슈 #5를 기반으로 PR을 생성해줘"

Claude:
  [MCP Tool] create_pr_from_issue(issueNumber: 5, currentBranch: "main")
  
  → PR #25 생성 완료
  → branch: feature/#5-회원-조회-api-구현
  → 제목: #5: 회원 조회 API 구현
  → 설명: [자동 생성된 PR 템플릿]
```

### 7.2 이슈 자동 생성

```
Claude Code 사용자:
  "데이터 검증이 누락되었어. 이슈로 등록해줘"

Claude:
  [MCP Tool] create_issue_from_claude(
    title: "데이터 검증 로직 추가",
    body: "...",
    issueType: "feature"
  )
  
  → 이슈 #42 생성 완료
  → 라벨: claude-code-generated, feature
```

### 7.3 이슈 조회

```
Claude Code 사용자:
  "현재 오픈된 이슈들 중에 feature 라벨이 있는 것들 보여줘"

Claude:
  [MCP Tool] list_issues(labels: ["feature"], status: "open")
  
  → [이슈 #5, #10, #15, ...]
```

---

## 8. 특수 기능: Claude Code 라벨

### 8.1 목표

Claude Code에서 자동 생성한 이슈를 사용자가 작성한 이슈와 구분

### 8.2 구현

- 모든 자동 생성 이슈에 `claude-code-generated` 라벨 추가
- GitHub 라벨 설정에서 커스텀 색상 지정 (옵션)

### 8.3 필터링

```
Claude Code에서 조회:
  - 자동 생성: filter by label = "claude-code-generated"
  - 사용자 작성: exclude label = "claude-code-generated"
```

---

## 9. 에러 처리

### 9.1 예상 에러 케이스

| 상황 | 처리 방법 |
|------|---------|
| GitHub Token 만료 | 에러 메시지 + 토큰 갱신 가이드 |
| 저장소 접근 불가 | GITHUB_TOKEN 권한 검증 |
| 이슈 번호 없음 | "이슈 #N을(를) 찾을 수 없습니다" |
| PR 생성 실패 | GitHub API 에러 메시지 전달 |
| CONTRIBUTING.md 파싱 실패 | 기본 규칙 사용 + 경고 로그 |

### 9.2 로깅

- 모든 MCP Tool 호출 기록
- GitHub API 호출 결과 기록
- 에러 상세 정보 기록

---

## 10. 보안

### 10.1 GitHub Token 관리

- `GITHUB_TOKEN` 환경 변수로 관리
- `.env` 파일은 `.gitignore`에 포함
- Docker Secret으로도 관리 가능 (프로덕션)

### 10.2 접근 권한

GitHub Token 권한:
- `repo` - 저장소 접근
- `issues` - 이슈 생성/수정
- `pull_requests` - PR 생성/수정
- `read:org` - 조직 정보 조회 (옵션)

### 10.3 데이터 보안

- GitHub에 저장된 민감 정보 처리 주의
- Wrapper 로그에 Token 노출 방지

---

## 11. 배포 및 실행

### 11.1 로컬 개발 환경 (권장)

```bash
# 1. 저장소 클론 (이미 되어있음)
cd amazon2-backend

# 2. .env 파일에 GitHub Token 추가
# 기존 .env 파일에 다음 추가:
# GITHUB_TOKEN=ghp_xxxx...
# REPOSITORY=jk/amazon2-backend

# 3. Docker Compose 실행 (모든 서비스 함께)
docker-compose up -d

# 4. 로그 확인
docker-compose logs -f github-mcp-wrapper

# 5. 상태 확인
docker-compose ps

# 6. Claude Code에서 MCP 연결
# - MCP Server URL: http://localhost:3000
```

### 11.2 MCP Wrapper 빌드

```bash
# 로컬에서 빌드 (Docker 없이 개발할 때)
cd mcp-wrapper
npm install
npm run dev

# 또는 Docker로 빌드
docker-compose build github-mcp-wrapper
```

### 11.3 프로덕션 배포

- Docker 이미지 빌드: `docker build -t amazon2-git-mcp:latest ./mcp-wrapper`
- 레지스트리 푸시 (선택사항)
- Kubernetes 또는 Docker Swarm으로 관리
- GitHub Actions로 자동 배포 (선택사항)

---

## 12. 테스트 전략

### 12.1 유닛 테스트

- CONTRIBUTING.md 파싱 테스트
- Branch명/PR 제목 생성 테스트
- 정규표현식 규칙 검증 테스트

### 12.2 통합 테스트

- github-mcp-server와의 상호작용
- 실제 이슈 생성/조회 테스트
- PR 생성 완전 흐름 테스트

### 12.3 E2E 테스트

- Claude Code와의 실제 통신 테스트
- 실제 GitHub 저장소를 대상으로 한 테스트

---

## 13. 향후 확장 가능성

- [ ] 자동 코드 리뷰 (github-mcp-server의 리뷰 기능)
- [ ] PR 자동 머지 (조건 기반)
- [ ] 이슈 자동 할당 (라벨/타입 기반)
- [ ] 변경사항 요약 생성
- [ ] 테스트 결과 자동 업데이트

---

## 14. 참고 자료

- [GitHub MCP Server - 공식 가이드](https://github.com/github/github-mcp-server)
- [MCP SDK - Node.js](https://github.com/modelcontextprotocol/sdk-js)
- [CONTRIBUTING.md - 본 프로젝트](../CONTRIBUTING.md)
- [GitHub REST API](https://docs.github.com/en/rest)

