# Amazon2 Git MCP Server

GitHub MCP 서버를 통합한 amazon2-backend용 PR/이슈 자동화 도구입니다.

## 기능

- **PR 자동 생성** - 이슈를 기반으로 CONTRIBUTING.md 규칙 자동 적용
- **이슈 자동 생성** - Claude Code에서 발견한 버그/기능을 이슈로 등록
- **규칙 자동 반영** - CONTRIBUTING.md 규칙이 MCP에 자동 반영

## 빠른 시작

```bash
# 저장소 루트에서 실행
docker-compose up -d

# Claude Code에서 MCP 서버 설정
# URL: http://localhost:3000
```

## 환경 변수

| 변수 | 설명 |
|------|------|
| `GITHUB_TOKEN` | GitHub PAT (필수) |
| `REPOSITORY` | 대상 저장소 (필수) |
| `LOG_LEVEL` | 로그 레벨 |

## MCP 도구

- `create_pr_from_issue` - 이슈→PR 자동 생성
- `create_issue_from_claude` - Claude Code 이슈 생성
- `get_issue_details` - 이슈 조회
- `parse_contributing_md` - 규칙 파싱
