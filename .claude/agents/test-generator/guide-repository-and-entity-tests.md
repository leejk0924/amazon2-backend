# Test Generator - Repository & Entity 테스트 가이드

## Repository 테스트

### 파일명

`{Domain}RepositoryTest.java`

### 테스트 클래스 구조

```java
@DataJpaTest
@Testcontainers
class {Domain}RepositoryTest {
    
    @Container
    static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0");
    
    @Autowired
    private {Domain}Repository repository;
    
    // 테스트 메서드들...
}
```

### 테스트 케이스 1: 저장 성공

```java
@Test
void shouldSaveSuccessfully() {
    // Given
    {Domain} entity = {Domain}.builder()
        .name("Test {Domain}")
        .description("Test Description")
        .createdAt(LocalDateTime.now())
        .updatedAt(LocalDateTime.now())
        .build();
    
    // When
    {Domain} saved = repository.save(entity);
    
    // Then
    assertThat(saved.getId()).isNotNull();
    assertThat(saved.getName()).isEqualTo("Test {Domain}");
}
```

### 테스트 케이스 2: ID로 조회 성공

```java
@Test
void shouldFindByIdSuccessfully() {
    // Given
    {Domain} entity = {Domain}.builder()
        .name("Test")
        .description("Description")
        .createdAt(LocalDateTime.now())
        .updatedAt(LocalDateTime.now())
        .build();
    {Domain} saved = repository.save(entity);
    
    // When
    Optional<{Domain}> found = repository.findById(saved.getId());
    
    // Then
    assertThat(found).isPresent();
    assertThat(found.get().getName()).isEqualTo("Test");
}
```

### 테스트 케이스 3: 전체 조회 성공

```java
@Test
void shouldFindAllSuccessfully() {
    // Given
    {Domain} entity1 = {Domain}.builder()
        .name("Test 1")
        .description("Desc 1")
        .createdAt(LocalDateTime.now())
        .updatedAt(LocalDateTime.now())
        .build();
    
    {Domain} entity2 = {Domain}.builder()
        .name("Test 2")
        .description("Desc 2")
        .createdAt(LocalDateTime.now())
        .updatedAt(LocalDateTime.now())
        .build();
    
    repository.save(entity1);
    repository.save(entity2);
    
    // When
    List<{Domain}> all = repository.findAll();
    
    // Then
    assertThat(all).hasSize(2);
}
```

### 테스트 케이스 4: 수정 성공

```java
@Test
void shouldUpdateSuccessfully() {
    // Given
    {Domain} entity = {Domain}.builder()
        .name("Original")
        .description("Original Description")
        .createdAt(LocalDateTime.now())
        .updatedAt(LocalDateTime.now())
        .build();
    {Domain} saved = repository.save(entity);
    
    // When
    saved.setName("Updated");
    saved.setUpdatedAt(LocalDateTime.now());
    {Domain} updated = repository.save(saved);
    
    // Then
    assertThat(updated.getName()).isEqualTo("Updated");
}
```

### 테스트 케이스 5: 삭제 성공

```java
@Test
void shouldDeleteSuccessfully() {
    // Given
    {Domain} entity = {Domain}.builder()
        .name("To Delete")
        .description("Desc")
        .createdAt(LocalDateTime.now())
        .updatedAt(LocalDateTime.now())
        .build();
    {Domain} saved = repository.save(entity);
    
    // When
    repository.deleteById(saved.getId());
    
    // Then
    Optional<{Domain}> deleted = repository.findById(saved.getId());
    assertThat(deleted).isEmpty();
}
```

### 테스트 케이스 6: 커스텀 쿼리 메서드

```java
@Test
void shouldFindByNameSuccessfully() {
    // Given
    {Domain} entity = {Domain}.builder()
        .name("Specific Name")
        .description("Desc")
        .createdAt(LocalDateTime.now())
        .updatedAt(LocalDateTime.now())
        .build();
    repository.save(entity);
    
    // When
    Optional<{Domain}> found = repository.findByName("Specific Name");
    
    // Then
    assertThat(found).isPresent();
    assertThat(found.get().getName()).isEqualTo("Specific Name");
}
```

---

## Entity 테스트

### 파일명

`{Domain}Test.java`

### 테스트 클래스 구조

```java
class {Domain}Test {
    
    // 테스트 메서드들...
}
```

### 테스트 케이스 1: 엔티티 생성

```java
@Test
void shouldCreateEntitySuccessfully() {
    // Given & When
    {Domain} entity = {Domain}.builder()
        .id(1L)
        .name("Test")
        .description("Description")
        .createdAt(LocalDateTime.now())
        .updatedAt(LocalDateTime.now())
        .build();
    
    // Then
    assertThat(entity.getId()).isEqualTo(1L);
    assertThat(entity.getName()).isEqualTo("Test");
    assertThat(entity.getDescription()).isEqualTo("Description");
}
```

### 테스트 케이스 2: Equals & HashCode

```java
@Test
void shouldEqualsAndHashcodeWork() {
    // Given
    LocalDateTime now = LocalDateTime.now();
    {Domain} entity1 = {Domain}.builder()
        .id(1L)
        .name("Test")
        .description("Description")
        .createdAt(now)
        .updatedAt(now)
        .build();
    
    {Domain} entity2 = {Domain}.builder()
        .id(1L)
        .name("Test")
        .description("Description")
        .createdAt(now)
        .updatedAt(now)
        .build();
    
    // Then
    assertThat(entity1).isEqualTo(entity2);
    assertThat(entity1.hashCode()).isEqualTo(entity2.hashCode());
}
```

### 테스트 케이스 3: ToString

```java
@Test
void shouldToStringWork() {
    // Given
    {Domain} entity = {Domain}.builder()
        .id(1L)
        .name("Test")
        .description("Description")
        .build();
    
    // Then
    String toString = entity.toString();
    assertThat(toString).contains("1", "Test", "Description");
}
```

---

## 주의사항 (Repository)

- `@DataJpaTest`로 JPA 관련 설정만 로드
- `@Testcontainers`로 Docker 기반 MySQL 실행
- `@Container` static 필드로 컨테이너 정의
- 실제 DB에 대한 쿼리 검증 가능
- H2 같은 In-Memory DB 대신 실제 DB 사용

## 주의사항 (Entity)

- `@DataJpaTest`나 DB가 필요 없음 (단위 테스트)
- Builder 패턴 동작 검증
- Lombok 어노테이션 검증 (@Data, @Builder)
- Equals, HashCode 구현 검증
