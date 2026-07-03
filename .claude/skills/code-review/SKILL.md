---
name: code-review
description: 코드 리뷰, PR 리뷰 요청 시 사용합니다. senior-code-reviewer 에이전트 실행 및 결과를 Notion에 저장하는 절차를 안내합니다. "코드 리뷰해줘", "이 코드 검토해줄래", "PR 리뷰해줘" 등에 활성화하세요.
---

# 코드 리뷰 절차

## 실행

`senior-code-reviewer` 에이전트로 아키텍처 규칙(50%)과 Best Practices(50%)를 검토합니다. 상세 검토 기준은 `.claude/agents/senior-code-reviewer.md` 참고.

## 결과 저장 (필수)

리뷰 완료 직후 **자동으로** Notion에 저장합니다. 사용자가 별도 지시하지 않아도 실행할 것.

- ❌ 로컬 파일(`.claude/agent-memory/` 등) 저장 금지
- ✅ Notion `Amazon2-backend > 코드 리뷰` 페이지 하위에만 저장

**저장 절차:**
1. `notion-search("코드 리뷰")` → `Amazon2-backend > 코드 리뷰` 페이지 ID 조회
2. 페이지가 없으면 `Amazon2-backend` 하위에 `코드 리뷰` 페이지 먼저 생성
3. `notion-create-pages(parent_id=코드리뷰_page_id, title="YYYY-MM-DD #이슈번호 <기능명> 코드 리뷰", content=...)`
4. 생성된 Notion URL을 사용자에게 반환

**페이지 내용 형식:**
```
## 개요
- 이슈: #번호
- 브랜치: feature/#번호-설명
- 리뷰어: senior-code-reviewer

## 좋은 점
...

## 아키텍처 규칙 위반
...

## Best Practices 개선사항
...

## 최종 평가
통과 여부: PASS / MINOR_ISSUE / MAJOR_ISSUE
```
