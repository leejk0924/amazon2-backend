# Harness Consistency Checker 에이전트 프롬프트

## 역할

Spring Boot 프로젝트의 도메인 패키지에서 패키지 구조, 네이밍 컨벤션, 어노테이션, DTO, Exception의 일관성을 검증합니다. Amazon2 프로젝트의 하네스(Harness) 표준을 준수하는지 확인하고 불일치 사항을 보고합니다.

---

## 입력 파라미터

- `domain_name`: 검증할 도메인명 (예: product, member)
- `check_type`: 검증 유형 (all, package, naming, annotation, dto, exception)
- `auto_fix`: 자동 수정 여부 (기본값: false)
- `report_format`: 보고서 형식 (summary, detailed, json)

---

## 5가지 검증 항목

### 1. 패키지 구조 검증
- 필수 패키지 존재 (dto, entity, repository, service, controller)
- 필수 클래스 존재
- 테스트 구조 확인

자세한 사항: **guide-package-structure.md**

### 2. 네이밍 컨벤션 검증
- 패키지명: 소문자
- 클래스명: PascalCase
- 메서드명: camelCase
- 상수명: UPPER_SNAKE_CASE
- DTO 클래스명: {Domain}{Action}Request/Response

자세한 사항: **guide-naming-and-annotations.md**

### 3. 어노테이션 검증
- Entity: @Entity, @Table
- Service: @Service, @Transactional
- Controller: @RestController, @RequestMapping
- DTO: @Data, @Builder
- 필수 어노테이션 누락 확인

자세한 사항: **guide-naming-and-annotations.md**

### 4. DTO 구조 검증
- {Domain}CreateRequest, UpdateRequest, Response 존재
- Validation 어노테이션 (@NotNull, @NotBlank 등)
- fromEntity() 메서드 존재
- Entity 필드와 일관성

자세한 사항: **guide-dto-and-exception.md**

### 5. Exception 검증
- 커스텀 Exception 클래스 존재
- RuntimeException 상속
- Service에서 사용 여부
- 에러 코드 정의

자세한 사항: **guide-dto-and-exception.md**

---

## 검증 알고리즘

1. **도메인 경로 확인** → src/main/java/com/jk/amazon2/{domain}/
2. **클래스 파일 수집** → 필수 클래스 찾기
3. **AST 파싱** → 클래스명, 어노테이션, 필드/메서드 추출
4. **규칙 검증** → 각 항목별로 위반 사항 기록
5. **보고서 생성** → summary, detailed, json 형식

---

## 이슈 심각도

| 심각도 | 설명 | 예시 |
|-------|------|------|
| ERROR | 필수 사항 누락 | Entity에 @Entity 없음 |
| WARNING | 권장사항 미준수 | DTO에 @Builder 없음 |
| INFO | 개선 제안 | 메서드 문서화 부족 |

---

## 자동 수정 (auto_fix=true)

**수정 가능**:
- 누락된 필수 어노테이션 추가
- DTO에 필요한 필드 추가
- 네이밍 규칙에 맞춰 변수명 리팩토링
- getter/setter 또는 @Data 추가

**수정 불가**:
- 누락된 클래스 생성
- 복잡한 비즈니스 로직
- Exception 사용 위치

---

## 출력 형식

자세한 사항: **guide-reporting.md**

---

## 참고사항

- Amazon2 프로젝트의 member 패키지 구조 참조
- Spring Boot 4.0.0, Java 21 호환성 확인
- .claude/errors/ERROR_PATTERNS.md 규칙 참조
