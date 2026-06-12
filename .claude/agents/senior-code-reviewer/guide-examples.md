# Senior Code Reviewer - 사용 예시

## 예시 1: 패키지 구조 위반

```
## 검토 결과: MemberService 구현

### 규칙 위반 (Architecture)
- [ERROR] PACKAGE_NAMING: 클래스가 com.jk.amazon2.service.MemberService에 있음 
  → 올바른 위치: com.jk.amazon2.member.service.MemberService

### Best Practices 개선사항
- [WARNING] NULL_SAFETY: memberRepository.findById()의 반환값을 그대로 사용
  → 개선: findById().orElseThrow(() -> new MemberException(...))

### 최종 평가
**통과 여부**: MAJOR_ISSUE
**핵심 요약**: 패키지 구조 재설정 필수, NULL 안전성 개선 필요
```

---

## 예시 2: 순환 의존성 감지

```
## 검토 결과: CategoryService 구현

### 규칙 위반 (Architecture)
- [ERROR] CIRCULAR_DEPENDENCY: CategoryService → PostingService → CategoryService
  의존성 발견 (PostingService에서 Category 조회 로직)

### Best Practices 개선사항
- [WARNING] PERFORMANCE: PostingService에서 Category 정보를 매번 새로 조회
  → 개선: 공유 DTO (PostingWithCategoryDto) 사용 또는 조인 쿼리

### 최종 평가
**통과 여부**: MAJOR_ISSUE
**핵심 요약**: 도메인 간 순환 의존성 제거 필수, 공유 DTO 패턴 적용 권장
```

---

## 예시 3: Best Practices 위반

```
## 검토 결과: MemberRepository 테스트

### 규칙 위반 (Architecture)
(없음)

### Best Practices 개선사항
- [WARNING] TEST_COVERAGE: Service의 핵심 메서드 중 40% 미만 테스트됨
  → 필수: createMember(), updateMember(), deleteMember() 테스트 추가
- [WARNING] ERROR_HANDLING: MemberException 발생 시나리오 테스트 부족
  → 개선: 각 ErrorCode별 예외 테스트 추가

### 최종 평가
**통과 여부**: MINOR_ISSUE
**핵심 요약**: 테스트 커버리지 개선으로 품질 보증 강화 필요
```

---

## 검토 기준 요약 표

| 항목 | 통과 조건 | 실패 조건 |
|------|---------|---------|
| 패키지 네이밍 | com.jk.amazon2.{domain}.{layer} | 다른 형식 |
| 계층 의존성 | 상향만 허용 | 하향 또는 순환 |
| 순환 의존성 | 없음 | 1개 이상 |
| 도메인 격리 | API 계층만 허용 | 서비스 계층 간 의존 |
| 클래스 네이밍 | 규칙 준수 | 규칙 미준수 |
| NULL 안전성 | null check/Optional | 무방비 접근 |
| 테스트 커버리지 | 70% 이상 | 70% 미만 |
| 오류 처리 | ErrorCode 사용 | 임의 Exception |
| 성능 | N+1 없음, 페이징 | N+1, 대량 조회 |
| 코드 가독성 | SRP, 의도 명확 | 복잡, 의도 불명확 |
| 보안 | 파라미터 바인딩, 권한 | SQL injection, 검증 부족 |
| 문서화 | JavaDoc, 설명 주석 | 문서화 부족 |
