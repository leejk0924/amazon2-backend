# 코드 리뷰 결과 저장 규칙

## 핵심 규칙

코드 리뷰어(`senior-code-reviewer` 에이전트)가 생성하는 **모든 리뷰 결과 문서는 Notion에 저장**한다.

- ❌ 로컬 파일(`.claude/agent-memory/`) 저장 금지
- ✅ Notion `Amazon2-backend > 코드 리뷰` 페이지 하위에 저장

---

## Notion 저장 위치

```
Amazon2-backend
├── 설계 문서      (page_id: 38572899-25bd-807d-862f-c5d83e92e870)
└── 코드 리뷰      ← 리뷰 결과 저장 위치
    └── YYYY-MM-DD #이슈번호 <기능명> 코드 리뷰
```

> `코드 리뷰` 페이지가 없으면 `Amazon2-backend` 하위에 먼저 생성할 것.

---

## 저장 시점

- `senior-code-reviewer` 에이전트가 리뷰를 완료한 직후
- 사용자가 별도로 지시하지 않아도 **자동으로** Notion에 저장

---

## 저장 형식

**페이지 제목:** `YYYY-MM-DD #이슈번호 <기능명> 코드 리뷰`
예: `2026-06-23 #50 주간 통계 조회 API 코드 리뷰`

**페이지 내용:**
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
통과 여부: PASS / MINOR_ISSUE / NEEDS_REWORK
```

---

## Notion 저장 절차

1. `notion-search("코드 리뷰")` → 코드 리뷰 페이지 ID 조회
2. 없으면 `Amazon2-backend` 하위에 `코드 리뷰` 페이지 생성
3. `notion-create-pages(parent_id=코드리뷰_page_id, title=..., content=...)` 로 리뷰 페이지 생성
4. 생성된 Notion URL 사용자에게 반환

---

**마지막 업데이트:** 2026-06-23
