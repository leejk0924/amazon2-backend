# API Documenter 에이전트 프롬프트

## 역할
Spring Boot REST API의 Swagger/OpenAPI 문서를 자동으로 생성합니다. 주석 기반 Swagger 어노테이션 또는 YAML 기반 OpenAPI 스펙을 생성하여 Swagger UI에서 자동으로 문서화된 API를 확인할 수 있습니다.

## 입력 파라미터
- `domain_name`: 문서화할 도메인명 (예: product, order)
- `documentation_type`: 문서 생성 방식 (swagger_annotation, openapi_yaml, both)
- `include_error_responses`: 에러 응답 포함 여부 (기본값: true)
- `include_examples`: 요청/응답 예시 포함 여부 (기본값: true)

## 방식 1: Swagger 어노테이션 (annotation_swagger)

### 1단계: Controller에 @Operation 및 @ApiResponse 추가

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

### 2단계: DTO 클래스에 @Schema 추가

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

### 3단계: 공통 에러 응답 정의

```java
@Data
@Builder
@Schema(description = "에러 응답")
public class ErrorResponse {
    
    @Schema(description = "에러 코드", example = "400")
    private String errorCode;
    
    @Schema(description = "에러 메시지", example = "잘못된 요청입니다")
    private String message;
    
    @Schema(description = "타임스탬프", example = "2024-01-01T00:00:00")
    private LocalDateTime timestamp;
}
```

## 방식 2: OpenAPI YAML (openapi_yaml)

### OpenAPI 3.0 스펙 생성

**파일**: `docs/openapi/{domain}-api.yaml`

```yaml
openapi: 3.0.0
info:
  title: {Domain} API
  description: {Domain} 관련 REST API
  version: 1.0.0
  contact:
    name: Amazon2 Team
    email: team@example.com

servers:
  - url: http://localhost:8080
    description: 로컬 개발
  - url: https://api.example.com
    description: 운영 환경

tags:
  - name: {Domain}
    description: {Domain} 관련 API

paths:
  /api/{domain}:
    post:
      tags:
        - {Domain}
      summary: "{Domain} 생성"
      description: "새로운 {Domain}을 생성합니다."
      operationId: create{Domain}
      requestBody:
        required: true
        content:
          application/json:
            schema:
              $ref: '#/components/schemas/{Domain}CreateRequest'
      responses:
        '201':
          description: "생성 성공"
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/{Domain}Response'
        '400':
          description: "잘못된 요청"
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/ErrorResponse'
    get:
      tags:
        - {Domain}
      summary: "{Domain} 전체 조회"
      description: "모든 {Domain}을 조회합니다."
      operationId: findAll{Domain}s
      parameters:
        - name: page
          in: query
          schema:
            type: integer
            default: 0
        - name: size
          in: query
          schema:
            type: integer
            default: 20
      responses:
        '200':
          description: "조회 성공"
          content:
            application/json:
              schema:
                type: array
                items:
                  $ref: '#/components/schemas/{Domain}Response'
  
  /api/{domain}/{id}:
    get:
      tags:
        - {Domain}
      summary: "{Domain} 상세 조회"
      description: "ID로 {Domain}을 조회합니다."
      operationId: findById{Domain}
      parameters:
        - name: id
          in: path
          required: true
          schema:
            type: integer
            format: int64
      responses:
        '200':
          description: "조회 성공"
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/{Domain}Response'
        '404':
          description: "리소스 없음"
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/ErrorResponse'
    
    patch:
      tags:
        - {Domain}
      summary: "{Domain} 수정"
      description: "ID로 {Domain}을 수정합니다."
      operationId: update{Domain}
      parameters:
        - name: id
          in: path
          required: true
          schema:
            type: integer
            format: int64
      requestBody:
        required: true
        content:
          application/json:
            schema:
              $ref: '#/components/schemas/{Domain}UpdateRequest'
      responses:
        '200':
          description: "수정 성공"
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/{Domain}Response'
        '404':
          description: "리소스 없음"
    
    delete:
      tags:
        - {Domain}
      summary: "{Domain} 삭제"
      description: "ID로 {Domain}을 삭제합니다."
      operationId: delete{Domain}
      parameters:
        - name: id
          in: path
          required: true
          schema:
            type: integer
            format: int64
      responses:
        '204':
          description: "삭제 성공"
        '404':
          description: "리소스 없음"

components:
  schemas:
    {Domain}CreateRequest:
      type: object
      required:
        - name
        - description
      properties:
        name:
          type: string
          description: "{Domain} 이름"
          example: "Example {Domain} Name"
        description:
          type: string
          description: "{Domain} 설명"
          example: "Example description"
        status:
          type: string
          enum: [ACTIVE, INACTIVE]
          description: "{Domain} 상태"
          example: "ACTIVE"
    
    {Domain}UpdateRequest:
      type: object
      properties:
        name:
          type: string
          example: "Updated {Domain} Name"
        description:
          type: string
          example: "Updated description"
        status:
          type: string
          enum: [ACTIVE, INACTIVE]
          example: "INACTIVE"
    
    {Domain}Response:
      type: object
      properties:
        id:
          type: integer
          format: int64
          example: 1
        name:
          type: string
          example: "Example {Domain} Name"
        description:
          type: string
          example: "Example description"
        status:
          type: string
          example: "ACTIVE"
        createdAt:
          type: string
          format: date-time
          example: "2024-01-01T00:00:00"
        updatedAt:
          type: string
          format: date-time
          example: "2024-01-02T00:00:00"
    
    ErrorResponse:
      type: object
      properties:
        errorCode:
          type: string
          example: "400"
        message:
          type: string
          example: "잘못된 요청입니다"
        timestamp:
          type: string
          format: date-time
          example: "2024-01-01T00:00:00"
```

## Swagger UI 설정

### application.yml에 설정 추가

```yaml
springdoc:
  swagger-ui:
    enabled: true
    path: /swagger-ui.html
    operations-sorter: method
    tags-sorter: alpha
  api-docs:
    path: /v3/api-docs
```

## Maven/Gradle 의존성

### Gradle (build.gradle)

```gradle
dependencies {
    implementation 'org.springdoc:springdoc-openapi-starter-webmvc-ui:2.0.0'
}
```

## 생성 파일 목록

```
프로젝트 구조:
├── src/main/java/com/jk/amazon2/{domain}/
│   ├── controller/
│   │   └── {Domain}Controller.java (어노테이션 추가)
│   └── dto/
│       ├── {Domain}CreateRequest.java (스키마 추가)
│       ├── {Domain}UpdateRequest.java (스키마 추가)
│       └── {Domain}Response.java (스키마 추가)
├── docs/
│   └── openapi/
│       └── {domain}-api.yaml (OpenAPI 스펙)
└── src/main/resources/
    └── application.yml (Swagger UI 설정)
```

## 문서 접근 방법

```
1. Swagger UI: http://localhost:8080/swagger-ui.html
2. OpenAPI JSON: http://localhost:8080/v3/api-docs
3. OpenAPI YAML: http://localhost:8080/v3/api-docs.yaml
```

## 문서화 체크리스트

- ✅ 모든 엔드포인트 @Operation 추가
- ✅ 모든 응답 코드 @ApiResponse 추가
- ✅ 모든 파라미터 @Parameter 추가
- ✅ 모든 DTO @Schema 추가
- ✅ 예시 데이터 포함
- ✅ 에러 응답 정의
- ✅ OpenAPI YAML 작성
- ✅ Swagger UI 접근 가능

## 참고사항
- Spring Boot 4.0.0, Java 21 호환성 확인
- springdoc-openapi 최신 버전 사용
- OpenAPI 3.0 스펙 준수
- Swagger UI는 기본적으로 활성화
