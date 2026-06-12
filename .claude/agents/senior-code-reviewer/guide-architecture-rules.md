# Senior Code Reviewer - 아키텍처 규칙 가이드

## 1. 패키지 네이밍 규칙 (PACKAGE_NAMING)

**규칙**: `com.jk.amazon2.{domain}.{layer}` 형식 준수

**검사 항목**:
- [ ] 패키지명이 정확한 도메인(`member`, `category`, `posting`)을 포함하는가?
- [ ] 계층(`entity`, `repository`, `service`, `dto`, `controller`, `exception`)이 올바른가?
- [ ] 공통 계층(`common`, `config`)의 패키지가 올바른가?

**위반 시**: ERROR - 패키지 구조 재설정 필요

---

## 2. 계층 간 의존성 규칙 (LAYER_DEPENDENCY)

**규칙**: 상향 의존성만 허용 (Controller → Service → Repository → Entity)

**검사 항목**:
- [ ] Controller가 Service를 통해서만 비즈니스 로직에 접근하는가?
- [ ] Service가 Repository와 Entity만 의존하는가?
- [ ] Repository가 Entity와 common에만 의존하는가?
- [ ] DTO가 다른 계층을 의존하지 않는가?

**위반 시**: ERROR - 순환 구조 발생 가능

---

## 3. 순환 의존성 금지 (CIRCULAR_DEPENDENCY)

**규칙**: A → B → C 구조는 가능하지만, A → B → A 구조는 금지

**검사 항목**:
- [ ] 클래스 간 양방향 참조가 없는가?
- [ ] 도메인 간 순환 의존성이 없는가?
- [ ] 인터페이스 분리로 순환 의존성을 피했는가?

**위반 시**: ERROR - 코드 재설계 필수

---

## 4. 도메인 격리 규칙 (DOMAIN_ISOLATION)

**규칙**: 도메인 간 의존성 최소화 (API 계층/common 제외)

**검사 항목**:
- [ ] Member 도메인이 Category/Posting 서비스에 직접 의존하지 않는가?
- [ ] 도메인 간 의존성이 발생하면 공통 DTO/인터페이스를 사용하는가?
- [ ] 도메인별 Exception(ErrorCode)이 분리되어 있는가?

**위반 시**: WARNING - 도메인 간결성 저하

---

## 5. 클래스 네이밍 규칙 (CLASS_NAMING)

**규칙**: 도메인과 계층을 명확히 반영하는 네이밍

**검사 항목**:
- [ ] Entity: `{DomainName}` (예: Member, Category, Posting)
- [ ] Repository: `{DomainName}Repository` (예: MemberRepository)
- [ ] Service: `{DomainName}{CommandOrQuery}Service` (예: MemberCommandService, MemberQueryService)
- [ ] Controller: `{DomainName}Controller` (예: MemberController)
- [ ] DTO: `{DomainName}{Request|Response|Dto}` (예: MemberCreateRequest)
- [ ] Exception: `{DomainName}ErrorCode` (예: MemberErrorCode)

**위반 시**: WARNING - 코드 가독성 저하
