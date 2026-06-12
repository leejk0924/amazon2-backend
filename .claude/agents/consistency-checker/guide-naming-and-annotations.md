# Harness Consistency Checker - 네이밍 & 어노테이션 검증 가이드

## 1. 네이밍 컨벤션 검증

### 패키지명

**규칙**: 소문자, snake_case 금지

```
✅ 올바른: com.jk.amazon2.product
❌ 잘못된: com.jk.amazon2.product_service
```

**위반 시**: WARNING - 코드 가독성 저하

---

### 클래스명

**규칙**: PascalCase

| 대상 | 규칙 | 예시 | 오류 예시 |
|------|------|------|----------|
| Entity | {DomainName} | Product, Member | product ❌ |
| Repository | {DomainName}Repository | ProductRepository | productRepository ❌ |
| Service | {DomainName}Service | ProductService | productService ❌ |
| Controller | {DomainName}Controller | ProductController | productController ❌ |
| DTO | {Domain}{Request\|Response} | ProductCreateRequest | ProductRequest ❌ |
| Exception | {DomainName}Exception | ProductException | productException ❌ |
| Interface | {Name}Repository (I 접두사 금지) | ProductRepository | IProductRepository ❌ |

**위반 시**: WARNING - 코드 가독성 저하

---

### 메서드명

**규칙**: camelCase, 동사+명사 조합

```
✅ 올바른:
- findByProductId()
- createProduct()
- updateProduct()

❌ 잘못된:
- find_by_product_id() (snake_case)
- FindByProductId() (PascalCase)
```

**위반 시**: WARNING - 코드 가독성 저하

---

### 상수명

**규칙**: UPPER_SNAKE_CASE

```
✅ 올바른: DEFAULT_PAGE_SIZE, MAX_LENGTH
❌ 잘못된: default_page_size, MaxLength
```

**위반 시**: WARNING - 코드 가독성 저하

---

### 필드명

**규칙**: camelCase, 의도 명확

```
✅ 올바른: productName, createdAt, isActive
❌ 잘못된: product_name, created_at, p_name
```

**위반 시**: WARNING - 코드 가독성 저하

---

### Enum명

**규칙**: UPPER_SNAKE_CASE

```
✅ 올바른: ACTIVE, INACTIVE, PENDING
❌ 잘못된: Active, inactive, pending
```

**위반 시**: WARNING - 코드 가독성 저하

---

## 2. 어노테이션 검증

### Entity 클래스

**필수 어노테이션**:

```java
@Entity              // ✅ 필수
@Table(name="...")   // ✅ 필수
@Data               // ✅ 권장 (Lombok)
@Builder            // ✅ 권장 (Lombok)
```

**검증 항목**:
- [ ] @Entity 존재
- [ ] @Table 존재 및 name 지정
- [ ] @Id @GeneratedValue 존재
- [ ] createdAt, updatedAt 필드

**위반 시**: ERROR - Entity 구조 오류

---

### Repository 인터페이스

**필수 어노테이션**:

```java
public interface {Domain}Repository extends JpaRepository<{Domain}, Long> {
    // @Repository는 자동
}
```

**검증 항목**:
- [ ] JpaRepository 상속
- [ ] 제네릭 타입: <{Domain}, Long>

**위반 시**: ERROR - Repository 구조 오류

---

### Service 클래스

**필수 어노테이션**:

```java
@Service                    // ✅ 필수
@Transactional              // ✅ 필수
@RequiredArgsConstructor    // ✅ 권장 (Lombok)
```

**검증 항목**:
- [ ] @Service 존재
- [ ] @Transactional 존재
- [ ] readOnly 메서드에 @Transactional(readOnly=true)

**위반 시**: ERROR - Service 구조 오류

---

### Controller 클래스

**필수 어노테이션**:

```java
@RestController                     // ✅ 필수
@RequestMapping("/api/{domain}")   // ✅ 필수
@Tag(name="...", description="...") // ✅ 권장
@RequiredArgsConstructor            // ✅ 권장
```

**검증 항목**:
- [ ] @RestController 존재
- [ ] @RequestMapping 존재
- [ ] @Tag (Swagger) 존재

**위반 시**: WARNING - Controller 문서화 미흡

---

### DTO 클래스

**필수 어노테이션**:

```java
@Data    // ✅ 필수 (getter/setter 자동 생성)
@Builder // ✅ 권장 (Builder 패턴)
```

**CreateRequest 추가**:
```java
@NotNull
@NotBlank
@Size(min=1, max=100)
```

**위반 시**: WARNING - DTO 구조 불완전

---

## 중복 및 모순 어노테이션

### ❌ 잘못된 예시

```java
@Data               // 이미 getter/setter 생성
@Getter             // 중복
@Setter             // 중복
public class Product { ... }
```

**위반 시**: WARNING - 중복 어노테이션

---

## 자동 수정 규칙 (auto_fix=true)

### 수정 가능

```
- @Data 어노테이션 추가
- @Builder 어노테이션 추가
- @Service, @Transactional 추가
- getter/setter 생성
- 필드명 camelCase 변환
- 클래스명 PascalCase 변환
```

### 수정 불가

```
- 클래스명 전체 변경 (의존성 영향)
- 패키지명 변경 (의존성 영향)
- 메서드 시그니처 변경 (호출 변경 필요)
```

---

## 심각도

| 항목 | 심각도 | 설명 |
|------|-------|------|
| 필수 어노테이션 누락 | ERROR | 구조 오류 |
| 클래스명 PascalCase 위반 | WARNING | 가독성 저하 |
| 메서드명 camelCase 위반 | WARNING | 가독성 저하 |
| 중복 어노테이션 | WARNING | 불필요한 코드 |
| 상수명 UPPER_SNAKE_CASE 위반 | INFO | 스타일 미흡 |
