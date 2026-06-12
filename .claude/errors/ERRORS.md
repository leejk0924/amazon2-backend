# 에러 레지스트리 (ERRORS.md)

Amazon2 프로젝트에서 발생 가능한 모든 에러를 정의하고 관리하는 중앙 레지스트리입니다.

## 에러 코드 규칙

- 형식: `E{숫자}` (예: E001, E002, ...)
- 범위별 분류:
  - E001-E099: 아키텍처 에러
  - E100-E199: 네이밍 컨벤션 에러
  - E200-E299: Validation 에러
  - E300-E399: 데이터베이스 에러
  - E400-E499: 비즈니스 로직 에러
  - E500-E599: 설정 에러

## 에러 정의

### 아키텍처 에러 (E001-E099)

#### E001: Entity가 Service에 의존

**심각도**: ERROR (빌드 실패)

**설명**: Entity 클래스가 Service 클래스에 의존하는 경우 발생합니다.

**원인**:
- Entity에서 Service를 주입받음
- Entity에서 Service를 직접 호출

**예시**:
```java
@Entity
public class Product {
    @Autowired
    private ProductService productService;  // ❌ E001
    
    public void doSomething() {
        productService.process();
    }
}
```

**해결책**:
1. Entity는 순수 데이터 모델로만 사용
2. 비즈니스 로직은 Service에서 처리
3. Entity는 JPA 어노테이션만 허용

**올바른 예시**:
```java
@Entity
public class Product {
    @Id
    private Long id;
    private String name;
    // 필드만 포함, 로직 불포함
}
```

---

#### E002: Repository가 Controller에 의존 / Service가 Controller에 의존

**심각도**: ERROR (빌드 실패)

**설명**: Repository나 Service가 Controller에 의존하는 경우 발생합니다.

**원인**:
- 계층 구조 역전
- 양방향 의존성

**예시**:
```java
@Service
public class ProductService {
    @Autowired
    private ProductController controller;  // ❌ E002
}
```

**해결책**:
1. 의존성 방향을 올바르게 설정: Controller → Service → Repository
2. 양방향 의존성 제거

**올바른 구조**:
```
Controller (최상위)
   ↓
Service (중간)
   ↓
Repository (최하위)
```

---

#### E003: Forbidden Cross-Domain Dependency

**심각도**: ERROR (빌드 실패)

**설명**: 하위 도메인이 상위 도메인의 Service에 의존하는 경우 발생합니다.

**Amazon2 도메인 계층**:
```
1. member (최상위)
   ↓ (의존 가능)
2. category (중간)
   ↓ (의존 가능)
3. posting (최하위)
```

**금지된 의존성**:
- category가 member에 의존 (X)
- posting이 member에 의존 (X)
- posting이 category에 의존 (X)

**예시**:
```java
// ❌ E003: CategoryService가 MemberService에 의존하면 안됨
@Service
public class CategoryService {
    @Autowired
    private MemberService memberService;  // ❌ 금지됨
}
```

**해결책**:
1. 의존성 방향을 역전시킬 수 없는 경우, 공통 기능을 common 패키지로 추출
2. 이벤트 기반 아키텍처 사용
3. 도메인 간 통신 패턴 변경

**올바른 예시** (event-driven):
```java
// CategoryService에서 MemberService 대신 이벤트 발행
@Service
public class CategoryService {
    @Autowired
    private ApplicationEventPublisher eventPublisher;
    
    public void createCategory(CategoryCreateRequest request) {
        Category category = new Category(...);
        // 이벤트 발행 - MemberService가 구독
        eventPublisher.publishEvent(new CategoryCreatedEvent(category));
    }
}

// MemberService에서 이벤트 구독
@Service
public class MemberService {
    @EventListener
    public void onCategoryCreated(CategoryCreatedEvent event) {
        // category 정보 처리
    }
}
```

---

#### E004: Circular Dependency

**심각도**: ERROR (빌드 실패)

**설명**: 도메인 또는 클래스 간 순환 의존성이 존재하는 경우 발생합니다.

**예시**:
```
ProductService → MemberService → ProductService (순환!)
```

**해결책**:
1. 순환 의존성을 끊기 위해 공통 인터페이스/추상화 도입
2. 이벤트 기반 아키텍처로 변경
3. 의존성 방향 재설계

---

### 네이밍 컨벤션 에러 (E100-E199)

#### E100: 클래스명이 PascalCase 규칙 위반

**심각도**: WARNING

**설명**: 클래스명이 PascalCase를 따르지 않는 경우입니다.

**규칙**:
- 클래스명: PascalCase
- 예시: `ProductService`, `ProductCreateRequest`, `Product`

**위반 예시**:
- `productService` (❌)
- `product_service` (❌)
- `PRODUCTSERVICE` (❌)

**해결책**:
```java
// ❌ 잘못된 예
public class productService { }

// ✅ 올바른 예
public class ProductService { }
```

---

#### E101: 메서드명이 camelCase 규칙 위반

**심각도**: WARNING

**설명**: 메서드명이 camelCase를 따르지 않는 경우입니다.

**규칙**:
- 메서드명: camelCase
- 예시: `findById()`, `createProduct()`, `deleteByName()`

**위반 예시**:
- `FindById()` (❌)
- `find_by_id()` (❌)
- `FINDBYID()` (❌)

---

#### E102: DTO 클래스명이 규칙 위반

**심각도**: ERROR

**설명**: DTO 클래스명이 규칙을 따르지 않는 경우입니다.

**규칙**:
- CreateRequest: `{Domain}CreateRequest`
- UpdateRequest: `{Domain}UpdateRequest`
- Response: `{Domain}Response`

**위반 예시**:
- `ProductRequest` (❌ CreateRequest 사용)
- `ProductDTO` (❌ 구체적인 타입 명시 필요)
- `UpdateProductRequest` (❌ CreateRequest 아님)

**올바른 예시**:
```java
public class ProductCreateRequest { }
public class ProductUpdateRequest { }
public class ProductResponse { }
```

---

### Validation 에러 (E200-E299)

#### E200: @NotNull 어노테이션 누락

**심각도**: WARNING

**설명**: 필수 필드에 @NotNull 어노테이션이 없는 경우입니다.

**규칙**:
- Request DTO의 필수 필드: `@NotNull`, `@NotBlank`
- Service에서도 null check 수행

**예시**:
```java
// ❌ E200
@Data
public class ProductCreateRequest {
    private String name;  // 필수지만 어노테이션 없음
}

// ✅ 올바른 예
@Data
public class ProductCreateRequest {
    @NotBlank(message = "상품명은 필수입니다")
    private String name;
}
```

---

#### E201: Validation 실패시 적절한 Exception 미발생

**심각도**: WARNING

**설명**: @Valid 검증 실패시 HttpMessageNotReadableException 대신 MethodArgumentNotValidException을 처리해야 합니다.

**예시**:
```java
// ✅ 올바른 예
@RestControllerAdvice
public class GlobalExceptionHandler {
    
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(
        MethodArgumentNotValidException ex
    ) {
        String message = ex.getBindingResult()
            .getFieldError()
            .getDefaultMessage();
        return ResponseEntity.badRequest()
            .body(new ErrorResponse("400", message));
    }
}
```

---

### 데이터베이스 에러 (E300-E399)

#### E300: Entity @Table 어노테이션 누락

**심각도**: ERROR

**설명**: @Entity 클래스에 @Table 어노테이션이 없는 경우입니다.

**규칙**:
- 모든 @Entity는 @Table(name="{domain}") 명시
- 테이블명은 도메인명을 사용 (소문자)

**예시**:
```java
// ❌ E300
@Entity
public class Product {
    @Id
    private Long id;
}

// ✅ 올바른 예
@Entity
@Table(name = "product")
public class Product {
    @Id
    private Long id;
}
```

---

#### E301: Entity ID 필드 미정의

**심각도**: ERROR

**설명**: @Entity 클래스에 @Id 필드가 없는 경우입니다.

**규칙**:
- 모든 Entity는 primary key 필드 필요
- 형식: `@Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;`

---

#### E302: @Column(nullable = false) 누락

**심각도**: WARNING

**설명**: NOT NULL 제약 조건을 갖는 필드에 @Column(nullable = false)가 없는 경우입니다.

**규칙**:
- 필수 필드: `@Column(nullable = false)`
- 선택 필드: `@Column(nullable = true)` 또는 생략

---

### 비즈니스 로직 에러 (E400-E499)

#### E400: Entity에 비즈니스 로직 포함

**심각도**: WARNING

**설명**: Entity에 비즈니스 로직이 포함된 경우입니다.

**규칙**:
- Entity: 순수 데이터 모델
- 비즈니스 로직: Service에서 처리

**위반 예시**:
```java
// ❌ E400
@Entity
public class Product {
    @Id
    private Long id;
    private String name;
    
    public void process() {  // ❌ 비즈니스 로직
        // ...
    }
}
```

**올바른 예**:
```java
// ✅ Entity는 데이터 모델만
@Entity
public class Product {
    @Id
    private Long id;
    private String name;
}

// ✅ Service에서 비즈니스 로직 처리
@Service
public class ProductService {
    public void process(Product product) {
        // 비즈니스 로직
    }
}
```

---

#### E401: Exception 처리 누락

**심각도**: ERROR

**설명**: 리소스를 찾을 수 없을 때 Exception을 발생시키지 않는 경우입니다.

**규칙**:
- Service의 findById(): 없으면 {Domain}NotFoundException 발생
- Controller: GlobalExceptionHandler에서 404 응답

**예시**:
```java
// ❌ E401
@Service
public class ProductService {
    public ProductResponse findById(Long id) {
        return productRepository.findById(id)
            .map(this::toResponse)
            .orElse(null);  // ❌ null 반환하면 안됨
    }
}

// ✅ 올바른 예
@Service
public class ProductService {
    public ProductResponse findById(Long id) {
        return productRepository.findById(id)
            .map(this::toResponse)
            .orElseThrow(() -> new ProductNotFoundException(id));
    }
}
```

---

### 설정 에러 (E500-E599)

#### E500: application.yml 프로필별 설정 누락

**심각도**: WARNING

**설명**: 실행 환경별 설정파일(application-{profile}.yml)이 없는 경우입니다.

**필수 설정파일**:
- `application.yml` - 공통 설정
- `application-local.yml` - 로컬 개발
- `application-test.yml` - 테스트
- `application-prod.yml` - 운영

---

#### E501: 환경 변수 문서화 누락

**심각도**: INFO

**설명**: 운영 환경에서 필요한 환경 변수를 문서화하지 않은 경우입니다.

**규칙**:
- 모든 필수 환경 변수를 DEVELOPMENT.md에 문서화
- 예시: `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_PASSWORD`

---

## 에러 참조

각 에러의 상세 정보는 다음을 참조하세요:

- **패턴 분석**: `.claude/errors/ERROR_PATTERNS.md`
- **메모리 시스템**: `.claude/memory/error_patterns/agent_feedback.md`
- **에이전트 가이드**:
  - Domain Generator: `.claude/agents/domain-generator-prompt.md`
  - Consistency Checker: `.claude/agents/consistency-checker-prompt.md`
  - Dependency Analyzer: `.claude/agents/dependency-analyzer-prompt.md`

---

## 에러 리포팅

에러를 발견했을 때:

1. 에러 코드와 심각도 확인
2. 원인과 해결책 숙지
3. 해당 에이전트 실행 (예: Consistency Checker)
4. 수정 후 피드백 기록 (`.claude/memory/error_patterns/agent_feedback.md`)

---

## 에러 통계

| 카테고리 | 개수 | 심각도 | 빈도 |
|---------|------|-------|------|
| 아키텍처 (E001-E099) | 4 | ERROR | 높음 |
| 네이밍 (E100-E199) | 3 | WARNING | 중간 |
| Validation (E200-E299) | 2 | WARNING | 중간 |
| DB (E300-E399) | 3 | ERROR | 중간 |
| 비즈니스 (E400-E499) | 2 | ERROR | 낮음 |
| 설정 (E500-E599) | 2 | INFO | 낮음 |

**합계**: 16개 에러 코드

---

## 업데이트 히스토리

| 버전 | 날짜 | 변경 내용 |
|------|------|---------|
| 1.0 | 2024-01-01 | 초기 생성 (E001-E501) |
