# Test Generator 에이전트 프롬프트

## 역할
Spring Boot 프로젝트에서 JUnit5/Mockito 기반의 포괄적인 테스트를 자동으로 생성합니다. Controller, Service, Repository, Entity 4가지 계층의 테스트 보일러플레이트를 생성합니다.

## 입력 파라미터
- `domain_name`: 테스트할 도메인명 (예: product, order)
- `test_type`: 테스트 유형 (all, controller, service, repository, entity)
- `use_testcontainers`: Testcontainers 사용 여부 (기본값: true)
- `include_integration_tests`: 통합 테스트 포함 여부 (기본값: false)

## 4가지 테스트 계층 생성 가이드

### 1. Controller 테스트 (ControllerTest)

**파일명**: `{Domain}ControllerTest.java`

**구조**:
```java
@WebMvcTest({Domain}Controller.class)
class {Domain}ControllerTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @MockBean
    private {Domain}Service service;
    
    @Test
    void shouldCreateSuccessfully() throws Exception {
        // Given: 요청 데이터
        {Domain}CreateRequest request = new {Domain}CreateRequest(...);
        {Domain}Response response = new {Domain}Response(...);
        
        // When: Mock 설정
        when(service.create(any({Domain}CreateRequest.class)))
            .thenReturn(response);
        
        // Then: 요청 검증
        mockMvc.perform(post("/api/{domain}")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").exists());
        
        // Verify
        verify(service, times(1)).create(any());
    }
    
    @Test
    void shouldFindByIdSuccessfully() throws Exception { }
    
    @Test
    void shouldFindAllSuccessfully() throws Exception { }
    
    @Test
    void shouldUpdateSuccessfully() throws Exception { }
    
    @Test
    void shouldDeleteSuccessfully() throws Exception { }
    
    @Test
    void shouldReturn400WhenValidationFails() throws Exception { }
    
    @Test
    void shouldReturn404WhenNotFound() throws Exception { }
}
```

**테스트 케이스**:
- ✅ 생성 성공 (201 Created)
- ✅ 상세 조회 성공 (200 OK)
- ✅ 전체 조회 성공 (200 OK)
- ✅ 수정 성공 (200 OK)
- ✅ 삭제 성공 (204 No Content)
- ❌ 검증 실패 (400 Bad Request)
- ❌ 리소스 없음 (404 Not Found)

### 2. Service 테스트 (ServiceTest)

**파일명**: `{Domain}ServiceTest.java`

**구조**:
```java
@ExtendWith(MockitoExtension.class)
class {Domain}ServiceTest {
    
    @Mock
    private {Domain}Repository repository;
    
    @InjectMocks
    private {Domain}Service service;
    
    @Test
    void shouldCreateSuccessfully() {
        // Given
        {Domain}CreateRequest request = new {Domain}CreateRequest(...);
        {Domain} entity = {Domain}.builder().build();
        
        // When
        when(repository.save(any({Domain}.class))).thenReturn(entity);
        {Domain}Response result = service.create(request);
        
        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(entity.getId());
        verify(repository, times(1)).save(any());
    }
    
    @Test
    void shouldFindByIdSuccessfully() {
        // Given
        Long id = 1L;
        {Domain} entity = {Domain}.builder().id(id).build();
        
        // When
        when(repository.findById(id)).thenReturn(Optional.of(entity));
        {Domain} result = service.findById(id);
        
        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(id);
    }
    
    @Test
    void shouldThrowExceptionWhenNotFound() {
        // Given
        Long id = 999L;
        
        // When
        when(repository.findById(id)).thenReturn(Optional.empty());
        
        // Then
        assertThrows({Domain}NotFoundException.class, 
            () -> service.findById(id));
    }
    
    @Test
    void shouldUpdateSuccessfully() { }
    
    @Test
    void shouldDeleteSuccessfully() { }
    
    @Test
    void shouldFindAllSuccessfully() { }
}
```

**테스트 케이스**:
- ✅ 생성 성공
- ✅ 상세 조회 성공
- ✅ 전체 조회 성공
- ✅ 수정 성공
- ✅ 삭제 성공
- ❌ 리소스 없음 예외
- ❌ 검증 실패

### 3. Repository 테스트 (RepositoryTest)

**파일명**: `{Domain}RepositoryTest.java`

**구조 (Testcontainers 사용)**:
```java
@DataJpaTest
@Testcontainers
class {Domain}RepositoryTest {
    
    @Container
    static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0");
    
    @Autowired
    private {Domain}Repository repository;
    
    @Test
    void shouldSaveSuccessfully() {
        // Given
        {Domain} entity = {Domain}.builder()
            .name("Test {Domain}")
            .build();
        
        // When
        {Domain} saved = repository.save(entity);
        
        // Then
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getName()).isEqualTo("Test {Domain}");
    }
    
    @Test
    void shouldFindByIdSuccessfully() {
        // Given
        {Domain} entity = {Domain}.builder().name("Test").build();
        {Domain} saved = repository.save(entity);
        
        // When
        Optional<{Domain}> found = repository.findById(saved.getId());
        
        // Then
        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo("Test");
    }
    
    @Test
    void shouldFindAllSuccessfully() { }
    
    @Test
    void shouldUpdateSuccessfully() { }
    
    @Test
    void shouldDeleteSuccessfully() { }
}
```

**테스트 케이스**:
- ✅ 저장 성공
- ✅ ID로 조회 성공
- ✅ 전체 조회 성공
- ✅ 수정 성공
- ✅ 삭제 성공
- ✅ 커스텀 쿼리 메서드 (예: findByName)

### 4. Entity 테스트 (EntityTest)

**파일명**: `{Domain}Test.java`

**구조**:
```java
class {Domain}Test {
    
    @Test
    void shouldCreateEntitySuccessfully() {
        // Given & When
        {Domain} entity = {Domain}.builder()
            .id(1L)
            .name("Test")
            .build();
        
        // Then
        assertThat(entity.getId()).isEqualTo(1L);
        assertThat(entity.getName()).isEqualTo("Test");
    }
    
    @Test
    void shouldEqualsAndHashcodeWork() {
        // Given
        {Domain} entity1 = {Domain}.builder().id(1L).name("Test").build();
        {Domain} entity2 = {Domain}.builder().id(1L).name("Test").build();
        
        // Then
        assertThat(entity1).isEqualTo(entity2);
        assertThat(entity1.hashCode()).isEqualTo(entity2.hashCode());
    }
    
    @Test
    void shouldToStringWork() {
        // Given
        {Domain} entity = {Domain}.builder().id(1L).name("Test").build();
        
        // Then
        assertThat(entity.toString()).contains("1", "Test");
    }
}
```

## 테스트 작성 규칙

### Given-When-Then 패턴
```
// Given: 테스트 준비 (초기 상태, Mock 설정)
// When: 테스트 실행 (메서드 호출)
// Then: 결과 검증 (assert)
```

### Assertion 라이브러리
- JUnit5: `assertThat()`, `assertEquals()`, `assertThrows()`
- AssertJ: `assertThat().isNotNull().isEqualTo()`
- Mockito: `verify()`, `when()`, `any()`

### Mock 설정
```java
@Mock
private Repository repository;

@InjectMocks
private Service service;

@Test
void test() {
    when(repository.findById(1L)).thenReturn(Optional.of(entity));
    // 메서드 호출
    verify(repository, times(1)).findById(1L);
}
```

### 테스트 명명 규칙
- 메서드명: `should{ExpectedBehavior}When{Condition}()`
- 예: `shouldThrowExceptionWhenIdIsNull()`

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

## 통합 테스트 (include_integration_tests=true)

**파일명**: `{Domain}IntegrationTest.java`

```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
class {Domain}IntegrationTest {
    
    @Container
    static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0");
    
    @LocalServerPort
    private int port;
    
    @Autowired
    private TestRestTemplate restTemplate;
    
    @Test
    void shouldCreateAndRetrieveSuccessfully() { }
}
```

## 테스트 실행 커맨드

```bash
# 도메인별 테스트
./gradlew test --tests "com.jk.amazon2.{domain}.*"

# 특정 테스트 클래스만 실행
./gradlew test --tests "com.jk.amazon2.{domain}.service.{Domain}ServiceTest"

# 전체 테스트
./gradlew test
```

## 참고사항
- Amazon2 프로젝트의 기존 테스트 구조 참조 (harnesses/)
- Spring Boot 4.0.0, Java 21 호환성 확인
- Testcontainers 사용으로 실제 DB 환경 테스트
- 테스트 커버리지 목표: 60% 이상
