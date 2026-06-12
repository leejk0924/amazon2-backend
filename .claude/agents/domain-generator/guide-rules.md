# Domain Generator - 생성 규칙

## 네이밍 컨벤션

| 대상 | 규칙 | 예시 |
|------|------|------|
| 패키지명 | 소문자 | com.jk.amazon2.product |
| 클래스명 | PascalCase | ProductService |
| 메서드명 | camelCase | findByProductId |
| 상수명 | UPPER_SNAKE_CASE | DEFAULT_PAGE_SIZE |
| 필드명 | camelCase | productName |
| Enum명 | UPPER_SNAKE_CASE | ACTIVE, INACTIVE |
| DTO 클래스명 | {Domain}{Action}Request/Response | ProductCreateRequest |

---

## 클래스별 필수 요소

### Entity 클래스

**필수 어노테이션**:
```java
@Entity
@Table(name = "{domain}")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
```

**필수 필드**:
- `Long id` with `@Id @GeneratedValue(strategy = GenerationType.IDENTITY)`
- `LocalDateTime createdAt`
- `LocalDateTime updatedAt`

**필수 제약**:
```java
@Column(nullable = false)
```

---

### Service 클래스

**필수 어노테이션**:
```java
@Service
@Transactional
@RequiredArgsConstructor
```

**필수 메서드**:
- `create({Domain}CreateRequest request): {Domain}Response`
- `findById(Long id): {Domain}Response`
- `findAll(): List<{Domain}Response>`
- `update(Long id, {Domain}UpdateRequest request): {Domain}Response`
- `delete(Long id): void`

**필수 특성**:
- `@Transactional(readOnly = true)` on read methods
- 엔티티→DTO 변환 메서드 포함

---

### Controller 클래스

**필수 어노테이션**:
```java
@RestController
@RequestMapping("/api/{domain}")
@RequiredArgsConstructor
@Tag(name = "{Domain}")
```

**필수 메서드 및 HTTP 상태**:
- `POST /` → 201 Created
- `GET /{id}` → 200 OK
- `GET` → 200 OK
- `PATCH /{id}` → 200 OK
- `DELETE /{id}` → 204 No Content

**필수 어노테이션**:
- `@RequestBody`, `@PathVariable`, `@RequestParam` 사용
- `@Valid` on request bodies
- `@Operation` on methods

---

### DTO 클래스

**{Domain}CreateRequest**:
- 제외 필드: id, createdAt, updatedAt
- Validation: @NotNull, @NotBlank, @Size 등
- 어노테이션: @Data, @Builder

**{Domain}UpdateRequest**:
- 수정 가능 필드만 포함
- Validation: 선택적 (@NotNull 없음)

**{Domain}Response**:
- 모든 조회 가능 필드 포함
- 포함 필드: id, createdAt, updatedAt
- 메서드: `fromEntity({Domain} entity)` 정적 메서드

---

### Exception 클래스

**필수 구현**:
```java
public class {Domain}NotFoundException extends RuntimeException {
    
    public {Domain}NotFoundException(String message) {
        super(message);
    }
    
    public {Domain}NotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
```

---

## Validation 규칙

### Entity 필드
```java
@Column(nullable = false)  // 필수 필드
```

### DTO 필드
```java
@NotNull(message = "필드명은 필수입니다")
@NotBlank(message = "필드명은 비어있을 수 없습니다")
@Size(min = 1, max = 100, message = "1-100자 범위")
```

### Service 검증
```java
// 서비스에서 비즈니스 로직 검증
if (request.getName() == null) {
    throw new {Domain}InvalidException("Name is required");
}
```

---

## 주석 스타일

```java
/**
 * {Domain} 엔티티
 * 
 * @description {설명}
 */
@Entity
@Table(name = "{domain}")
public class {Domain} {
    // ...
}
```

---

## 에러 처리 패턴

### 404 Not Found
```java
repository.findById(id)
    .orElseThrow(() -> new {Domain}NotFoundException(
        String.format("{Domain} not found: %d", id)
    ));
```

### 검증 오류
```java
if (request.getName() == null || request.getName().isBlank()) {
    throw new {Domain}InvalidException("Name cannot be blank");
}
```

### GlobalExceptionHandler에서 처리
```java
@ExceptionHandler({Domain}NotFoundException.class)
public ResponseEntity<ErrorResponse> handle{Domain}NotFound(
    {Domain}NotFoundException e) {
    return ResponseEntity.status(HttpStatus.NOT_FOUND)
        .body(new ErrorResponse("404", e.getMessage()));
}
```
