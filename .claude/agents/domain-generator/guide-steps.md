# Domain Generator - 10단계 생성 가이드

## 1단계: 패키지 구조 생성

```
src/main/java/com/jk/amazon2/{domain_name}/
├── dto/
├── entity/
├── repository/
├── service/
├── controller/
└── exception/

src/test/java/com/jk/amazon2/{domain_name}/
├── controller/
├── service/
├── repository/
└── entity/
```

각 디렉토리 생성 확인.

---

## 2단계: Entity 생성

```java
@Entity
@Table(name = "{domain}")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class {Domain} {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private String name;
    
    @Column(nullable = false)
    private String description;
    
    @Column(nullable = false)
    private LocalDateTime createdAt;
    
    @Column(nullable = false)
    private LocalDateTime updatedAt;
}
```

- JPA @Entity, @Table 어노테이션 필수
- id, createdAt, updatedAt 필수 필드
- @Column(nullable = false) 제약 추가

---

## 3단계: DTO 클래스 생성

**{Domain}CreateRequest.java**:
```java
@Data
@Builder
public class {Domain}CreateRequest {
    
    @NotBlank(message = "이름은 필수입니다")
    private String name;
    
    @NotBlank(message = "설명은 필수입니다")
    private String description;
}
```

**{Domain}UpdateRequest.java**:
```java
@Data
@Builder
public class {Domain}UpdateRequest {
    
    private String name;
    private String description;
}
```

**{Domain}Response.java**:
```java
@Data
@Builder
public class {Domain}Response {
    
    private Long id;
    private String name;
    private String description;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    public static {Domain}Response fromEntity({Domain} entity) {
        return {Domain}Response.builder()
            .id(entity.getId())
            .name(entity.getName())
            .description(entity.getDescription())
            .createdAt(entity.getCreatedAt())
            .updatedAt(entity.getUpdatedAt())
            .build();
    }
}
```

---

## 4단계: Repository 생성

```java
public interface {Domain}Repository extends JpaRepository<{Domain}, Long> {
    
    Optional<{Domain}> findByName(String name);
    
    List<{Domain}> findByNameContaining(String name);
}
```

- JpaRepository<{Domain}, Long> 상속
- 필요한 커스텀 쿼리 메서드 정의

---

## 5단계: Service 클래스 생성

```java
@Service
@Transactional
@RequiredArgsConstructor
public class {Domain}Service {
    
    private final {Domain}Repository repository;
    
    public {Domain}Response create({Domain}CreateRequest request) {
        {Domain} entity = {Domain}.builder()
            .name(request.getName())
            .description(request.getDescription())
            .createdAt(LocalDateTime.now())
            .updatedAt(LocalDateTime.now())
            .build();
        return {Domain}Response.fromEntity(repository.save(entity));
    }
    
    @Transactional(readOnly = true)
    public {Domain}Response findById(Long id) {
        {Domain} entity = repository.findById(id)
            .orElseThrow(() -> new {Domain}NotFoundException("Not found: " + id));
        return {Domain}Response.fromEntity(entity);
    }
    
    @Transactional(readOnly = true)
    public List<{Domain}Response> findAll() {
        return repository.findAll().stream()
            .map({Domain}Response::fromEntity)
            .collect(Collectors.toList());
    }
    
    public {Domain}Response update(Long id, {Domain}UpdateRequest request) {
        {Domain} entity = repository.findById(id)
            .orElseThrow(() -> new {Domain}NotFoundException("Not found: " + id));
        
        if (request.getName() != null) entity.setName(request.getName());
        if (request.getDescription() != null) entity.setDescription(request.getDescription());
        
        entity.setUpdatedAt(LocalDateTime.now());
        return {Domain}Response.fromEntity(repository.save(entity));
    }
    
    public void delete(Long id) {
        repository.deleteById(id);
    }
}
```

---

## 6단계: Controller 생성

```java
@RestController
@RequestMapping("/api/{domain}")
@RequiredArgsConstructor
@Tag(name = "{Domain}")
public class {Domain}Controller {
    
    private final {Domain}Service service;
    
    @PostMapping
    @Operation(summary = "{Domain} 생성")
    public ResponseEntity<{Domain}Response> create(
        @RequestBody @Valid {Domain}CreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(service.create(request));
    }
    
    @GetMapping("/{id}")
    @Operation(summary = "{Domain} 상세 조회")
    public ResponseEntity<{Domain}Response> findById(@PathVariable Long id) {
        return ResponseEntity.ok(service.findById(id));
    }
    
    @GetMapping
    @Operation(summary = "{Domain} 전체 조회")
    public ResponseEntity<List<{Domain}Response>> findAll() {
        return ResponseEntity.ok(service.findAll());
    }
    
    @PatchMapping("/{id}")
    @Operation(summary = "{Domain} 수정")
    public ResponseEntity<{Domain}Response> update(
        @PathVariable Long id,
        @RequestBody @Valid {Domain}UpdateRequest request) {
        return ResponseEntity.ok(service.update(id, request));
    }
    
    @DeleteMapping("/{id}")
    @Operation(summary = "{Domain} 삭제")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
```

---

## 7단계: Exception 클래스 생성

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

## 8단계: Enum 클래스 생성 (선택)

```java
public enum {Domain}Status {
    
    ACTIVE("활성"),
    INACTIVE("비활성"),
    DELETED("삭제");
    
    private final String description;
    
    {Domain}Status(String description) {
        this.description = description;
    }
    
    public String getDescription() {
        return description;
    }
}
```

Entity에 추가:
```java
@Enumerated(EnumType.STRING)
@Column(nullable = false)
private {Domain}Status status;
```

---

## 9단계: 테스트 클래스 스켈레톤 생성

테스트 파일들의 기본 구조 생성:
- {Domain}ControllerTest.java
- {Domain}ServiceTest.java
- {Domain}RepositoryTest.java
- {Domain}Test.java

자세한 사항은 Test Generator 에이전트 참조.

---

## 10단계: 설정 및 검증

- 패키지 경로가 `com.jk.amazon2.{domain_name}` 형식인지 확인
- 클래스명이 PascalCase인지 확인
- 기존 도메인 구조와 일관성 확인
- application.yml에 필요한 설정 추가
