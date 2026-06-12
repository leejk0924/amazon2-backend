# Test Generator - 테스트 작성 규칙

## Given-When-Then 패턴

모든 테스트는 다음 구조를 따릅니다:

```java
@Test
void shouldBehaviorWhenCondition() {
    // Given: 테스트 준비 (초기 상태, Mock 설정, 데이터 생성)
    String input = "test";
    
    // When: 테스트 실행 (메서드 호출)
    String result = functionUnderTest(input);
    
    // Then: 결과 검증 (assert)
    assertThat(result).isEqualTo("expected");
}
```

---

## Assertion 라이브러리

### JUnit5

```java
assertThat(value).isEqualTo(expected);
assertThat(value).isNotNull();
assertThat(value).isTrue();
assertThat(list).hasSize(2);
```

### AssertJ

```java
assertThat(value).isEqualTo(expected).isNotNull();
assertThat(list).hasSize(2).contains(item1, item2);
assertThat(value).isInstanceOf(String.class);
```

### Exception Assertions

```java
assertThrows(NullPointerException.class, () -> {
    method(null);
});

assertThatThrownBy(() -> method(null))
    .isInstanceOf(NullPointerException.class)
    .hasMessage("value is null");
```

---

## Mock 설정

### Mockito 주요 메서드

```java
// Mock 객체 생성
@Mock
private SomeClass mockObject;

// Injection
@InjectMocks
private ServiceUnderTest service;

// 동작 설정
when(mockObject.method(arg)).thenReturn(value);
when(mockObject.method(any())).thenReturn(value);

// 예외 설정
when(mockObject.method()).thenThrow(new Exception());

// 동작 없음
doNothing().when(mockObject).method();

// 메서드 호출 검증
verify(mockObject, times(1)).method(arg);
verify(mockObject, never()).method(arg);
verify(mockObject, atLeastOnce()).method(arg);

// Argument Matcher
when(service.create(any(Request.class))).thenReturn(response);
when(service.delete(eq(1L))).thenReturn(true);
```

---

## 테스트 메서드 명명 규칙

### 패턴

```
should{ExpectedBehavior}When{Condition}()
```

### 예시

| 테스트 목적 | 메서드명 |
|-----------|---------|
| 정상 생성 | `shouldCreateSuccessfully()` |
| 정상 조회 | `shouldFindByIdSuccessfully()` |
| 예외 발생 | `shouldThrowExceptionWhenNotFound()` |
| 예외 발생 | `shouldThrowExceptionWhenIdIsNull()` |
| 값 비교 | `shouldReturnCorrectValue()` |
| 검증 | `shouldValidateInputCorrectly()` |
| 호출 검증 | `shouldCallRepositoryOnce()` |

---

## 테스트 데이터 구성

### Builder 패턴 활용

```java
{Domain} entity = {Domain}.builder()
    .id(1L)
    .name("Test Name")
    .description("Test Description")
    .createdAt(LocalDateTime.now())
    .updatedAt(LocalDateTime.now())
    .build();
```

### Fixture 또는 Helper 메서드

```java
private {Domain} create{Domain}(String name) {
    return {Domain}.builder()
        .name(name)
        .description("Test")
        .createdAt(LocalDateTime.now())
        .updatedAt(LocalDateTime.now())
        .build();
}
```

---

## Controller 테스트 주요 메서드

### MockMvc

```java
// 요청 수행
mockMvc.perform(post("/api/path")
    .contentType(MediaType.APPLICATION_JSON)
    .content(objectMapper.writeValueAsString(data)))

// 응답 검증
.andExpect(status().isOk())
.andExpect(jsonPath("$.id").value(1))
.andExpect(jsonPath("$", hasSize(2)))

// 응답 출력 (디버깅)
.andDo(print());
```

### 주요 HTTP 상태 코드

| 메서드 | 예상 상태 |
|-------|---------|
| POST (생성) | 201 Created |
| GET | 200 OK |
| PATCH (수정) | 200 OK |
| DELETE | 204 No Content |
| 검증 실패 | 400 Bad Request |
| 리소스 없음 | 404 Not Found |

---

## Service 테스트 주요 메서드

### Repository Mock 설정

```java
// 단일 엔티티 반환
when(repository.findById(1L))
    .thenReturn(Optional.of(entity));

// 리스트 반환
when(repository.findAll())
    .thenReturn(Arrays.asList(entity1, entity2));

// 저장
when(repository.save(any(Entity.class)))
    .thenReturn(savedEntity);

// 예외 발생
when(repository.findById(999L))
    .thenThrow(new EntityNotFoundException());
```

---

## Repository 테스트 설정

### Testcontainers

```java
@Container
static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0")
    .withDatabaseName("testdb")
    .withUsername("root")
    .withPassword("password");

// 또는 PostgreSQL
@Container
static PostgreSQLContainer<?> postgres = 
    new PostgreSQLContainer<>("postgres:latest");
```

---

## 테스트 실행 커맨드

### 전체 테스트

```bash
./gradlew test
```

### 도메인별 테스트

```bash
./gradlew test --tests "com.jk.amazon2.{domain}.*"
```

### 특정 테스트 클래스

```bash
./gradlew test --tests "com.jk.amazon2.{domain}.service.{Domain}ServiceTest"
```

### 특정 테스트 메서드

```bash
./gradlew test --tests "com.jk.amazon2.{domain}.service.{Domain}ServiceTest.shouldCreateSuccessfully"
```

### 테스트 커버리지

```bash
./gradlew jacocoTestReport
```

---

## 테스트 커버리지 목표

| 계층 | 커버리지 | 목표 |
|------|--------|------|
| Entity | 60% | 기본 필드, Builder 검증 |
| Repository | 70% | CRUD, 커스텀 쿼리 |
| Service | 80% | CRUD, 예외 처리, 비즈니스 로직 |
| Controller | 70% | API 엔드포인트, 검증, 예외 |
| **전체** | **70%** | 목표 달성 |

---

## 주의사항

- 테스트는 독립적이어야 함 (순서 무관)
- 테스트 데이터는 각 테스트마다 새로 생성
- Mock은 과도하지 않게 사용 (통합 테스트 필요 시 주의)
- 테스트명은 명확하고 의도를 드러내야 함
- Given-When-Then 패턴 준수
- 한 테스트는 하나의 동작만 검증
- Assertion은 최소한 하나 이상 필요
