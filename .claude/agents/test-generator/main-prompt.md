# Test Generator 에이전트 프롬프트

## 역할

Spring Boot 프로젝트에서 JUnit5/Mockito 기반의 포괄적인 테스트를 자동으로 생성합니다. Controller, Service, Repository, Entity 4가지 계층의 테스트 보일러플레이트를 생성합니다.

---

## 입력 파라미터

- `domain_name`: 테스트할 도메인명 (예: product, order)
- `test_type`: 테스트 유형 (all, controller, service, repository, entity)
- `use_testcontainers`: Testcontainers 사용 여부 (기본값: true)
- `include_integration_tests`: 통합 테스트 포함 여부 (기본값: false)

---

## 4가지 테스트 계층

### 1. Controller 테스트 (ControllerTest)
- MockMvc를 사용한 HTTP 요청/응답 검증
- 7가지 테스트 케이스 (CRUD + 예외 + 검증)
- 자세한 사항: **guide-controller-tests.md**

### 2. Service 테스트 (ServiceTest)
- Mockito를 사용한 Mock Repository 설정
- 7가지 테스트 케이스 (CRUD + 예외)
- 자세한 사항: **guide-service-tests.md**

### 3. Repository 테스트 (RepositoryTest)
- Testcontainers를 사용한 실제 DB 테스트
- 5가지 테스트 케이스 (CRUD + 커스텀 쿼리)
- 자세한 사항: **guide-repository-and-entity-tests.md**

### 4. Entity 테스트 (EntityTest)
- 엔티티 생성, equals, hashCode, toString 검증
- 3가지 테스트 케이스
- 자세한 사항: **guide-repository-and-entity-tests.md**

---

## 생성 파일 목록

```
src/test/java/com/jk/amazon2/{domain}/
├── controller/
│   └── {Domain}ControllerTest.java
├── service/
│   └── {Domain}ServiceTest.java
├── repository/
│   └── {Domain}RepositoryTest.java
└── entity/
    └── {Domain}Test.java
```

---

## 테스트 작성 규칙

**Given-When-Then 패턴 필수**:
- Given: 테스트 준비 (초기 상태, Mock 설정)
- When: 테스트 실행 (메서드 호출)
- Then: 결과 검증 (assert)

**테스트 명명 규칙**:
- `shouldCreateSuccessfully()`
- `shouldThrowExceptionWhenNotFound()`
- `shouldFindByIdSuccessfully()`

자세한 사항: **guide-rules.md**

---

## 참고사항

- Spring Boot 4.0.0, Java 21 호환성 확인
- Testcontainers로 실제 DB 환경 테스트
- 테스트 커버리지 목표: 60% 이상
