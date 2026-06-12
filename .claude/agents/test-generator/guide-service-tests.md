# Test Generator - Service 테스트 가이드

## 파일명

`{Domain}ServiceTest.java`

---

## 테스트 클래스 구조

```java
@ExtendWith(MockitoExtension.class)
class {Domain}ServiceTest {
    
    @Mock
    private {Domain}Repository repository;
    
    @InjectMocks
    private {Domain}Service service;
    
    // 테스트 메서드들...
}
```

---

## 테스트 케이스 1: 생성 성공

```java
@Test
void shouldCreateSuccessfully() {
    // Given
    {Domain}CreateRequest request = new {Domain}CreateRequest();
    request.setName("Test Name");
    request.setDescription("Test Description");
    
    {Domain} entity = {Domain}.builder()
        .id(1L)
        .name("Test Name")
        .description("Test Description")
        .build();
    
    // When
    when(repository.save(any({Domain}.class)))
        .thenReturn(entity);
    
    {Domain}Response result = service.create(request);
    
    // Then
    assertThat(result).isNotNull();
    assertThat(result.getId()).isEqualTo(1L);
    assertThat(result.getName()).isEqualTo("Test Name");
    verify(repository, times(1)).save(any());
}
```

---

## 테스트 케이스 2: ID로 조회 성공

```java
@Test
void shouldFindByIdSuccessfully() {
    // Given
    Long id = 1L;
    {Domain} entity = {Domain}.builder()
        .id(id)
        .name("Test Name")
        .description("Test Description")
        .build();
    
    // When
    when(repository.findById(id))
        .thenReturn(Optional.of(entity));
    
    {Domain}Response result = service.findById(id);
    
    // Then
    assertThat(result).isNotNull();
    assertThat(result.getId()).isEqualTo(id);
    assertThat(result.getName()).isEqualTo("Test Name");
}
```

---

## 테스트 케이스 3: 조회 실패 (예외 발생)

```java
@Test
void shouldThrowExceptionWhenNotFound() {
    // Given
    Long id = 999L;
    
    // When
    when(repository.findById(id))
        .thenReturn(Optional.empty());
    
    // Then
    assertThrows({Domain}NotFoundException.class, 
        () -> service.findById(id));
    
    verify(repository, times(1)).findById(id);
}
```

---

## 테스트 케이스 4: 전체 조회 성공

```java
@Test
void shouldFindAllSuccessfully() {
    // Given
    {Domain} entity1 = {Domain}.builder().id(1L).name("Test 1").build();
    {Domain} entity2 = {Domain}.builder().id(2L).name("Test 2").build();
    
    List<{Domain}> entities = Arrays.asList(entity1, entity2);
    
    // When
    when(repository.findAll()).thenReturn(entities);
    
    List<{Domain}Response> results = service.findAll();
    
    // Then
    assertThat(results).hasSize(2);
    assertThat(results.get(0).getId()).isEqualTo(1L);
    assertThat(results.get(1).getId()).isEqualTo(2L);
}
```

---

## 테스트 케이스 5: 수정 성공

```java
@Test
void shouldUpdateSuccessfully() {
    // Given
    Long id = 1L;
    {Domain}UpdateRequest request = new {Domain}UpdateRequest();
    request.setName("Updated Name");
    
    {Domain} entity = {Domain}.builder()
        .id(id)
        .name("Old Name")
        .build();
    
    {Domain} updatedEntity = {Domain}.builder()
        .id(id)
        .name("Updated Name")
        .build();
    
    // When
    when(repository.findById(id)).thenReturn(Optional.of(entity));
    when(repository.save(any({Domain}.class))).thenReturn(updatedEntity);
    
    {Domain}Response result = service.update(id, request);
    
    // Then
    assertThat(result.getName()).isEqualTo("Updated Name");
    verify(repository, times(1)).findById(id);
    verify(repository, times(1)).save(any());
}
```

---

## 테스트 케이스 6: 수정 시 엔티티 없음

```java
@Test
void shouldThrowExceptionWhenUpdatingNonExistent() {
    // Given
    Long id = 999L;
    {Domain}UpdateRequest request = new {Domain}UpdateRequest();
    
    // When
    when(repository.findById(id))
        .thenReturn(Optional.empty());
    
    // Then
    assertThrows({Domain}NotFoundException.class,
        () -> service.update(id, request));
}
```

---

## 테스트 케이스 7: 삭제 성공

```java
@Test
void shouldDeleteSuccessfully() {
    // Given
    Long id = 1L;
    
    // When
    doNothing().when(repository).deleteById(id);
    service.delete(id);
    
    // Then
    verify(repository, times(1)).deleteById(id);
}
```

---

## 주의사항

- `@ExtendWith(MockitoExtension.class)`로 Mockito 초기화
- `@Mock`으로 Repository를 Mock 처리
- `@InjectMocks`로 Service에 Mock 주입
- `when(...).thenReturn(...)`으로 Mock 동작 설정
- `assertThat()` (AssertJ) 사용으로 가독성 향상
- `verify()`로 Mock 메서드 호출 확인
- `assertThrows()`로 예외 발생 검증
