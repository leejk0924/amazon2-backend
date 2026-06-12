# Test Generator - Controller 테스트 가이드

## 파일명

`{Domain}ControllerTest.java`

---

## 테스트 클래스 구조

```java
@WebMvcTest({Domain}Controller.class)
class {Domain}ControllerTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    @MockBean
    private {Domain}Service service;
    
    // 테스트 메서드들...
}
```

---

## 테스트 케이스 1: 생성 성공 (201 Created)

```java
@Test
void shouldCreateSuccessfully() throws Exception {
    // Given: 요청 데이터
    {Domain}CreateRequest request = new {Domain}CreateRequest();
    request.setName("Test Name");
    request.setDescription("Test Description");
    
    {Domain}Response response = new {Domain}Response();
    response.setId(1L);
    response.setName("Test Name");
    response.setDescription("Test Description");
    
    // When: Mock 설정
    when(service.create(any({Domain}CreateRequest.class)))
        .thenReturn(response);
    
    // Then: 요청 검증
    mockMvc.perform(post("/api/{domain}")
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.id").value(1))
        .andExpect(jsonPath("$.name").value("Test Name"));
    
    // Verify
    verify(service, times(1)).create(any());
}
```

---

## 테스트 케이스 2: 상세 조회 성공 (200 OK)

```java
@Test
void shouldFindByIdSuccessfully() throws Exception {
    // Given
    Long id = 1L;
    {Domain}Response response = new {Domain}Response();
    response.setId(id);
    response.setName("Test Name");
    
    // When
    when(service.findById(id)).thenReturn(response);
    
    // Then
    mockMvc.perform(get("/api/{domain}/" + id)
        .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(id))
        .andExpect(jsonPath("$.name").value("Test Name"));
    
    verify(service, times(1)).findById(id);
}
```

---

## 테스트 케이스 3: 전체 조회 성공 (200 OK)

```java
@Test
void shouldFindAllSuccessfully() throws Exception {
    // Given
    {Domain}Response response1 = new {Domain}Response();
    response1.setId(1L);
    response1.setName("Test 1");
    
    {Domain}Response response2 = new {Domain}Response();
    response2.setId(2L);
    response2.setName("Test 2");
    
    List<{Domain}Response> responses = Arrays.asList(response1, response2);
    
    // When
    when(service.findAll()).thenReturn(responses);
    
    // Then
    mockMvc.perform(get("/api/{domain}")
        .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$", hasSize(2)))
        .andExpect(jsonPath("$[0].id").value(1))
        .andExpect(jsonPath("$[1].id").value(2));
    
    verify(service, times(1)).findAll();
}
```

---

## 테스트 케이스 4: 수정 성공 (200 OK)

```java
@Test
void shouldUpdateSuccessfully() throws Exception {
    // Given
    Long id = 1L;
    {Domain}UpdateRequest request = new {Domain}UpdateRequest();
    request.setName("Updated Name");
    
    {Domain}Response response = new {Domain}Response();
    response.setId(id);
    response.setName("Updated Name");
    
    // When
    when(service.update(eq(id), any({Domain}UpdateRequest.class)))
        .thenReturn(response);
    
    // Then
    mockMvc.perform(patch("/api/{domain}/" + id)
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.name").value("Updated Name"));
    
    verify(service, times(1)).update(eq(id), any());
}
```

---

## 테스트 케이스 5: 삭제 성공 (204 No Content)

```java
@Test
void shouldDeleteSuccessfully() throws Exception {
    // Given
    Long id = 1L;
    
    // When
    doNothing().when(service).delete(id);
    
    // Then
    mockMvc.perform(delete("/api/{domain}/" + id))
        .andExpect(status().isNoContent());
    
    verify(service, times(1)).delete(id);
}
```

---

## 테스트 케이스 6: 검증 실패 (400 Bad Request)

```java
@Test
void shouldReturn400WhenValidationFails() throws Exception {
    // Given: 유효하지 않은 요청 데이터 (name 필드 없음)
    String invalidRequest = "{ \"description\": \"Test\" }";
    
    // Then
    mockMvc.perform(post("/api/{domain}")
        .contentType(MediaType.APPLICATION_JSON)
        .content(invalidRequest))
        .andExpect(status().isBadRequest());
    
    verify(service, never()).create(any());
}
```

---

## 테스트 케이스 7: 리소스 없음 (404 Not Found)

```java
@Test
void shouldReturn404WhenNotFound() throws Exception {
    // Given
    Long id = 999L;
    
    // When
    when(service.findById(id))
        .thenThrow(new {Domain}NotFoundException("Not found"));
    
    // Then
    mockMvc.perform(get("/api/{domain}/" + id))
        .andExpect(status().isNotFound());
    
    verify(service, times(1)).findById(id);
}
```

---

## 주의사항

- `@WebMvcTest`는 Controller만 로드 (빠른 테스트)
- `@MockBean`으로 Service를 Mock 처리
- `MockMvc`로 HTTP 요청/응답 검증
- `objectMapper`로 JSON 직렬화/역직렬화
- 모든 테스트에서 `verify()` 호출로 Mock 사용 확인
