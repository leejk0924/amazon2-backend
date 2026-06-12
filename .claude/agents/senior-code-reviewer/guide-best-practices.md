# Senior Code Reviewer - Best Practices 가이드

## 1. NULL 안전성 (NULL_SAFETY)

**규칙**: NullPointerException 방지

**검사 항목**:
- [ ] 외부 입력값(parameter, DB)에 null check 또는 Optional 사용?
- [ ] Lombok의 `@NonNull` 또는 Spring의 `@NotNull` 검증 사용?
- [ ] 메서드가 null을 반환할 수 있다면 Optional 또는 명확한 문서화?

**위반 시**: WARNING - 런타임 오류 가능성

---

## 2. 테스트 커버리지 (TEST_COVERAGE)

**규칙**: 핵심 로직은 70% 이상의 테스트 커버리지 필요

**검사 항목**:
- [ ] Service/Controller에 대한 단위 테스트가 있는가?
- [ ] 비즈니스 로직의 주요 경로(Happy path, Exception case)가 테스트되는가?
- [ ] Repository 쿼리가 검증되는가?

**위반 시**: WARNING - 품질 보증 부족

---

## 3. 오류 처리 및 예외 (ERROR_HANDLING)

**규칙**: 프로젝트 정의 ErrorCode 사용, 적절한 HttpStatus 매핑

**검사 항목**:
- [ ] 도메인별 ErrorCode 인터페이스 구현? (예: MemberErrorCode)
- [ ] Controller에서 서비스 예외를 적절히 처리?
- [ ] GlobalExceptionHandler로 통일된 응답 형식?
- [ ] 비즈니스 예외에 HttpStatus 올바르게 매핑?

**위반 시**: ERROR - 일관성 없는 API 응답

---

## 4. 성능 최적화 (PERFORMANCE)

**규칙**: N+1 쿼리, 불필요한 객체 생성 제거

**검사 항목**:
- [ ] JPA `@EntityGraph`, `fetch join` 등으로 N+1 쿼리 방지?
- [ ] Stream/Loop에서 무거운 객체 반복 생성 없음?
- [ ] Lazy loading으로 인한 예상치 못한 쿼리 발생 없음?
- [ ] 대량 데이터 조회에 페이징 적용?

**위반 시**: WARNING - 성능 저하

---

## 5. 코드 가독성 (CODE_READABILITY)

**규칙**: 메서드는 단일 책임, 변수명은 의도 명확, 메서드 길이 제한

**검사 항목**:
- [ ] 메서드가 한 가지 책임만 가지는가? (SRP)
- [ ] 변수명이 의도를 명확히 하는가?
- [ ] 메서드 길이가 과도하지 않은가? (권장: 20-30줄 이하)
- [ ] 매직넘버나 매직스트링 없는가?

**위반 시**: WARNING - 유지보수 어려움

---

## 6. 보안 (SECURITY)

**규칙**: SQL injection, 권한 검증, 데이터 보호

**검사 항목**:
- [ ] JPQL/Native Query에 파라미터 바인딩 사용? (String concatenation 금지)
- [ ] API 엔드포인트에 권한 검증 (@PreAuthorize 등)?
- [ ] 민감한 정보(password, token)를 로그/응답에 포함하지 않음?
- [ ] Request validation이 충분한가? (@Validated, @Valid)

**위반 시**: ERROR - 보안 취약점

---

## 7. 문서화 (DOCUMENTATION)

**규칙**: 복잡한 로직에 주석/JavaDoc, 메서드 목적 명확

**검사 항목**:
- [ ] Public 메서드에 JavaDoc이 있는가?
- [ ] 복잡한 비즈니스 로직에 설명 주석이 있는가?
- [ ] 예외 처리 이유가 명확한가?
- [ ] 도메인 특화 로직이 문서화되어 있는가?

**위반 시**: INFO - 유지보수성 저하
