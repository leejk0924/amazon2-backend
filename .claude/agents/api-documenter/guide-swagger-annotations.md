# API Documenter - Swagger 어노테이션 가이드

## 1단계: Controller에 @Operation 및 @ApiResponse 추가

```java
@RestController
@RequestMapping("/api/{domain}")
@Tag(name = "{Domain}", description = "{Domain} 관련 API")
public class {Domain}Controller {
    
    @PostMapping
    @Operation(
        summary = "{Domain} 생성",
        description = "새로운 {Domain}을 생성합니다.",
        tags = {"{Domain}"}
    )
    @ApiResponse(
        responseCode = "201",
        description = "생성 성공",
        content = @Content(
            mediaType = "application/json",
            schema = @Schema(implementation = {Domain}Response.class)
        )
    )
    @ApiResponse(
        responseCode = "400",
        description = "잘못된 요청",
        content = @Content(
            mediaType = "application/json",
            schema = @Schema(implementation = ErrorResponse.class)
        )
    )
    public ResponseEntity<{Domain}Response> create(
        @RequestBody @Valid {Domain}CreateRequest request
    ) {
        // ...
    }
    
    @GetMapping("/{id}")
    @Operation(
        summary = "{Domain} 상세 조회",
        description = "ID로 {Domain}을 조회합니다."
    )
    @ApiResponse(
        responseCode = "200",
        description = "조회 성공",
        content = @Content(schema = @Schema(implementation = {Domain}Response.class))
    )
    @ApiResponse(
        responseCode = "404",
        description = "리소스 없음",
        content = @Content(schema = @Schema(implementation = ErrorResponse.class))
    )
    public ResponseEntity<{Domain}Response> findById(
        @PathVariable @Parameter(description = "{Domain} ID") Long id
    ) {
        // ...
    }
    
    @GetMapping
    @Operation(
        summary = "{Domain} 전체 조회",
        description = "모든 {Domain}을 조회합니다."
    )
    @ApiResponse(
        responseCode = "200",
        description = "조회 성공",
        content = @Content(schema = @Schema(implementation = {Domain}Response[].class))
    )
    public ResponseEntity<List<{Domain}Response>> findAll(
        @RequestParam(required = false) @Parameter(description = "페이지") Integer page,
        @RequestParam(required = false) @Parameter(description = "크기") Integer size
    ) {
        // ...
    }
    
    @PatchMapping("/{id}")
    @Operation(
        summary = "{Domain} 수정",
        description = "ID로 {Domain}을 수정합니다."
    )
    @ApiResponse(responseCode = "200", description = "수정 성공")
    @ApiResponse(responseCode = "404", description = "리소스 없음")
    public ResponseEntity<{Domain}Response> update(
        @PathVariable Long id,
        @RequestBody @Valid {Domain}UpdateRequest request
    ) {
        // ...
    }
    
    @DeleteMapping("/{id}")
    @Operation(
        summary = "{Domain} 삭제",
        description = "ID로 {Domain}을 삭제합니다."
    )
    @ApiResponse(responseCode = "204", description = "삭제 성공")
    @ApiResponse(responseCode = "404", description = "리소스 없음")
    public ResponseEntity<Void> delete(
        @PathVariable Long id
    ) {
        // ...
    }
}
```

---

## 2단계: DTO 클래스에 @Schema 추가

### {Domain}CreateRequest.java

```java
@Data
@Builder
@Schema(description = "{Domain} 생성 요청")
public class {Domain}CreateRequest {
    
    @NotBlank(message = "이름은 필수입니다")
    @Schema(
        description = "{Domain} 이름",
        example = "Example {Domain} Name",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    private String name;
    
    @NotNull(message = "설명은 필수입니다")
    @Schema(
        description = "{Domain} 설명",
        example = "Example description",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    private String description;
    
    @Schema(
        description = "{Domain} 상태",
        example = "ACTIVE",
        allowableValues = {"ACTIVE", "INACTIVE"}
    )
    private String status;
}
```

### {Domain}UpdateRequest.java

```java
@Data
@Builder
@Schema(description = "{Domain} 수정 요청")
public class {Domain}UpdateRequest {
    
    @Schema(
        description = "{Domain} 이름",
        example = "Updated {Domain} Name"
    )
    private String name;
    
    @Schema(
        description = "{Domain} 설명",
        example = "Updated description"
    )
    private String description;
    
    @Schema(
        description = "{Domain} 상태",
        example = "ACTIVE",
        allowableValues = {"ACTIVE", "INACTIVE"}
    )
    private String status;
}
```

### {Domain}Response.java

```java
@Data
@Builder
@Schema(description = "{Domain} 응답")
public class {Domain}Response {
    
    @Schema(description = "{Domain} ID", example = "1")
    private Long id;
    
    @Schema(description = "{Domain} 이름", example = "Example {Domain} Name")
    private String name;
    
    @Schema(description = "{Domain} 설명", example = "Example description")
    private String description;
    
    @Schema(description = "{Domain} 상태", example = "ACTIVE")
    private String status;
    
    @Schema(description = "생성일시", example = "2024-01-01T00:00:00")
    private LocalDateTime createdAt;
    
    @Schema(description = "수정일시", example = "2024-01-02T00:00:00")
    private LocalDateTime updatedAt;
}
```

---

## 3단계: 공통 에러 응답 정의

```java
@Data
@Builder
@Schema(description = "에러 응답")
public class ErrorResponse {
    
    @Schema(description = "에러 코드", example = "400")
    private String errorCode;
    
    @Schema(description = "에러 메시지", example = "잘못된 요청입니다")
    private String message;
    
    @Schema(description = "타임스탐프", example = "2024-01-01T00:00:00")
    private LocalDateTime timestamp;
}
```

---

## 주요 어노테이션

| 어노테이션 | 설명 | 사용 위치 |
|----------|------|---------|
| `@Tag` | API 그룹화 | Controller 클래스 |
| `@Operation` | API 엔드포인트 설명 | 메서드 |
| `@ApiResponse` | HTTP 응답 정의 | 메서드 |
| `@Parameter` | 파라미터 설명 | 메서드 파라미터 |
| `@Schema` | 데이터 모델 설명 | DTO 클래스 및 필드 |
| `@RequestBody` | 요청 바디 | 메서드 파라미터 |
| `@Valid` | 검증 수행 | 메서드 파라미터 |

---

## 주의사항

- @Tag 안의 이름과 @Operation의 tags 값을 일치시키기
- @Schema의 description과 example은 명확하게
- 모든 응답 코드에 @ApiResponse 추가
- ErrorResponse 클래스로 통일된 에러 응답 사용
