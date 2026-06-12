# 에러 패턴 메모리 시스템 (Agent Feedback)

에이전트 실행 결과, 에러 감지, 수정 사항 및 학습 내용을 기록하는 메모리 시스템입니다.

## 목적

1. 각 에이전트 실행 이력 추적
2. 발견된 에러 패턴 분석
3. 반복되는 문제 조기 감지
4. 프로젝트 아키텍처 개선

## 기록 구조

### 세션 기록 (Session Record)

```markdown
## [날짜] [에이전트명] - [도메인명]

**상태**: 완료 / 진행중 / 실패

**입력 파라미터**:
- domain_name: product
- check_type: all
- auto_fix: false

**발견 사항**:
- [심각도] [에러코드]: 설명
- [심각도] [에러코드]: 설명

**수정 사항**:
- [파일명]: [변경 내용]
- [파일명]: [변경 내용]

**학습 내용**:
- 패턴 설명
- 원인 분석
- 재발 방지 방법

**다음 단계**:
- [ ] 수정 사항 검증
- [ ] 테스트 실행
- [ ] PR 생성
```

---

## 기록된 세션

### 초기 설정

**상태**: 준비 완료

**설명**: 에러 관리 시스템 초기 생성
- ERRORS.md: 16개 에러 코드 정의
- ERROR_PATTERNS.md: 3가지 주요 패턴 정의
- agent_feedback.md: 메모리 시스템 구축

**에러 코드 범주**:
- E001-E099: 아키텍처 (4개)
- E100-E199: 네이밍 (3개)
- E200-E299: Validation (2개)
- E300-E399: 데이터베이스 (3개)
- E400-E499: 비즈니스 (2개)
- E500-E599: 설정 (2개)

---

## 분석 대시보드

### 에러 분포

| 심각도 | 개수 | 비율 | 담당 에이전트 |
|-------|------|------|-------------|
| ERROR | 8개 | 50% | Consistency Checker, Dependency Analyzer |
| WARNING | 5개 | 31% | Consistency Checker |
| INFO | 3개 | 19% | Consistency Checker |

### 패턴별 빈도

| 패턴 | 에러코드 | 발생빈도 | 심각도 |
|------|---------|---------|-------|
| 패키지 네이밍 | E100-E102 | 높음 | WARNING |
| 크로스 도메인 | E003, E008 | 중간 | ERROR |
| 순환 의존성 | E004 | 낮음 | ERROR |
| 아키텍처 계층 | E001, E002 | 높음 | ERROR |
| Entity 설계 | E300-E302 | 중간 | ERROR |

---

## 에이전트별 역할 정리

### 1. Domain Generator
**목적**: 새 도메인 자동 생성
- 생성 내용: 패키지 구조, Entity, DTO, Service, Controller, Repository
- 예방 에러: E100-E102 (패키지 네이밍)
- 권장 사용: 새 도메인 추가시

### 2. Test Generator
**목적**: 테스트 코드 자동 생성
- 생성 대상: Controller, Service, Repository, Entity 테스트
- 관련 에러: 테스트 누락 (직접 코드 에러는 아님)
- 권장 사용: 도메인 구현 후

### 3. API Documenter
**목적**: API 문서 자동 생성
- 생성 내용: Swagger/OpenAPI 어노테이션, YAML
- 관련 에러: 없음 (문서화 목적)
- 권장 사용: API 개발 완료후

### 4. Consistency Checker
**목적**: 일관성 검증
- 검증 대상: 패키지, 네이밍, 어노테이션, DTO, Exception
- 감지 에러: E001-E102, E300-E302, E400-E401
- 권장 사용: 정기적 검증 (PR 전 등)

### 5. Dependency Analyzer
**목적**: 의존성 분석
- 검증 대상: 순환 의존성, 금지 의존성, 크로스 도메인
- 감지 에러: E003, E004, E008
- 권장 사용: 아키텍처 리뷰, 대규모 리팩토링

---

## 사용 시나리오

### 시나리오 1: 새 도메인 추가

```
1. Domain Generator 실행
   - domain_name: newdomain
   - create_dto: true
   - create_exception: true
   - create_enum: false

2. Consistency Checker 실행 (검증)
   - domain_name: newdomain
   - check_type: all
   - auto_fix: true

3. Test Generator 실행
   - domain_name: newdomain
   - test_type: all

4. API Documenter 실행
   - domain_name: newdomain
   - documentation_type: both

5. Dependency Analyzer 실행 (최종 검증)
   - analysis_type: all
```

### 시나리오 2: 기존 도메인 리팩토링

```
1. Consistency Checker 실행 (현상 파악)
   - domain_name: product
   - check_type: all
   - report_format: detailed

2. 이슈 분류 및 수정

3. Consistency Checker 재실행 (검증)
   - auto_fix: true

4. Dependency Analyzer 실행 (아키텍처 검증)
   - analysis_type: all
```

### 시나리오 3: 정기 아키텍처 감사

```
1. Dependency Analyzer 전체 실행
   - analysis_type: all
   - output_format: json

2. 순환 의존성 감지 및 분석

3. Cross-domain 의존성 검토

4. 개선 계획 수립 및 실행
```

---

## 성공/실패 사례

### 성공 사례 (예시)

**케이스 1: Domain Generator로 Product 도메인 생성**

```
날짜: 2024-01-15
에이전트: Domain Generator
도메인: product
상태: 성공

생성된 파일:
- com/jk/amazon2/product/entity/Product.java
- com/jk/amazon2/product/dto/ProductCreateRequest.java
- com/jk/amazon2/product/dto/ProductUpdateRequest.java
- com/jk/amazon2/product/dto/ProductResponse.java
- com/jk/amazon2/product/repository/ProductRepository.java
- com/jk/amazon2/product/service/ProductService.java
- com/jk/amazon2/product/controller/ProductController.java

Consistency Checker 검증: ✅ 패스
- 패키지 구조: OK
- 네이밍 규칙: OK
- 어노테이션: OK
- DTO 일관성: OK

Dependency Analyzer 검증: ✅ 패스
- 순환 의존성: 없음
- 금지 의존성: 없음
- Cross-domain: OK
```

### 실패 사례 (예시)

**케이스 2: 수동으로 생성한 도메인에서 일관성 오류 감지**

```
날짜: 2024-01-20
에이전트: Consistency Checker
도메인: order (수동 생성)
상태: 오류 감지

발견된 이슈:
- [ERROR] E100: 패키지 구조 불일치
  - order/ 폴더에 직접 Java 파일 존재
  - entity/, dto/, service/ 서브패키지 없음

- [WARNING] E101: 메서드 네이밍 오류
  - OrderService에 Find_By_Id() 메서드 (camelCase 위반)

- [WARNING] E102: DTO 네이밍 오류
  - OrderDTO.java (규칙: OrderCreateRequest, OrderResponse)

- [ERROR] E300: @Table 어노테이션 누락
  - Order.java에 @Entity는 있으나 @Table 없음

조치:
- Domain Generator 다시 실행으로 올바른 구조 생성
- 기존 코드 병합

결과: ✅ 이슈 모두 해결
```

---

## 개선 사항 추적

### 발견된 반복 패턴

**패턴**: Entity 클래스에 @Table 어노테이션 자주 누락

```
발생 횟수: 3회
영향도: 중간 (JPA 기본값으로 작동하지만 규칙 위반)
근본 원인: 개발자가 수동으로 Entity 작성시 누락

해결책:
1. Domain Generator에 자동 @Table 추가 기능 포함 ✅
2. Consistency Checker auto_fix로 자동 추가 ✅
3. IDE 템플릿 업데이트 (선택)
```

**패턴**: DTO 네이밍 규칙 이해 부족

```
발생 횟수: 2회
영향도: 낮음 (코드 동작에는 영향 없음)
근본 원인: 신입 개발자의 규칙 미숙지

해결책:
1. DEVELOPING.md에 DTO 네이밍 규칙 추가 ✅
2. harnesses/README.md 강화 ✅
3. 코드 리뷰 체크리스트에 추가
```

---

## 학습 기록

### 에러 패턴 학습

1. **패키지 네이밍 패턴**
   - 학습: 명확한 서브패키지 구조의 중요성
   - 개선: 자동화로 90% 이상의 오류 사전 방지

2. **크로스 도메인 의존성 패턴**
   - 학습: 도메인 계층을 명확히 정의해야 함
   - 개선: Event-driven 아키텍처 선택

3. **순환 의존성 패턴**
   - 학습: 빌드 타임에 감지 불가능한 경우 존재
   - 개선: 정기적인 Dependency Analyzer 실행

---

## 향후 개선 계획

### 단기 (1-2주)

- [ ] 모든 기존 도메인에 Consistency Checker 실행
- [ ] 발견된 이슈 수정
- [ ] member, category 도메인 검증 완료

### 중기 (1개월)

- [ ] Dependency Analyzer로 전체 아키텍처 평가
- [ ] Cross-domain 의존성 최적화
- [ ] Test Generator로 테스트 커버리지 80% 이상 달성

### 장기 (3개월+)

- [ ] CI/CD에 Consistency Checker 통합
- [ ] 정기적 아키텍처 감사 자동화
- [ ] 에이전트 기반 개발 워크플로우 정립

---

## 레퍼런스

- 에러 정의: `.claude/errors/ERRORS.md`
- 패턴 분석: `.claude/errors/ERROR_PATTERNS.md`
- 에이전트 목록: `.claude/agents/`
  - domain-generator-prompt.md
  - test-generator-prompt.md
  - api-documenter-prompt.md
  - consistency-checker-prompt.md
  - dependency-analyzer-prompt.md
