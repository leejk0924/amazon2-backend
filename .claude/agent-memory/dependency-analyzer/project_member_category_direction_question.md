---
name: project-member-category-direction-question
description: Open question whether member -> CategoryValidationPort (implemented by category) is an intentional reversal of the documented member/category dependency direction
metadata:
  type: project
---

2026-07-04 분석에서 발견: `member.service.MemberService`가 `common.port.CategoryValidationPort`를 통해 `categoryCode` 존재 여부를 검증하는데, 이 인터페이스의 유일한 구현체는 `category.adapter.CategoryValidationAdapter`다. 컴파일 타임 순환이나 직접 import는 없지만(DIP로 회피), 런타임에는 Member가 Category 도메인의 존재에 의존한다.

**Why:** harnesses/README와 dependency-analyzer 규칙은 "member는 category/posting에 의존하면 안 됨, category → member만 허용"이라고 명시하지만, 실제로는 Member.categoryCode 컬럼의 무결성을 지키기 위해 member가 category 검증 로직에 의존한다. 이게 의도된 예외(DIP로 완화된 필요악)인지, 아니면 실제로 고쳐야 할 설계 문제인지 코드만 봐서는 판단 불가 — 팀 확인이 필요한 사항.

**How to apply:** 다음 dependency-analyzer 실행 시 이 질문이 아직 미해결이면 다시 WARNING으로 보고하되, 팀이 "의도된 설계"라고 확인했다면 이 메모를 업데이트하고 harnesses/README.md에 예외로 문서화되었는지 확인할 것. 만약 `CategoryValidationPort` 관련 코드가 리팩토링되었다면(예: 삭제/이름변경) 먼저 grep으로 현재도 존재하는지 확인 후 언급할 것.
