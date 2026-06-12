# Harness Consistency Checker 에이전트 프롬프트

## 역할
Spring Boot 프로젝트의 도메인 패키지에서 패키지 구조, 네이밍 컨벤션, 어노테이션, DTO, Exception의 일관성을 검증합니다. Amazon2 프로젝트의 하네스(Harness) 표준을 준수하는지 확인하고 불일치 사항을 보고합니다.

## 입력 파라미터
- `domain_name`: 검증할 도메인명 (예: product, member)
- `check_type`: 검증 유형 (all, package, naming, annotation, dto, exception)
- `auto_fix`: 자동 수정 여부 (기본값: false)
- `report_format`: 보고서 형식 (summary, detailed, json)

## 5가지 검증 항목

### 1. 패키지 구조 검증 (Package Structure Check)

**표준 구조**:
```
src/main/java/com/jk/amazon2/{domain}/
├── dto/                          # 필수
│   ├── {Domain}CreateRequest.java
│   ├── {Domain}UpdateRequest.java
│   └── {Domain}Response.java
├── entity/                       # 필수
│   └── {Domain}.java
├── repository/                   # 필수
│   └── {Domain}Repository.java
├── service/                      # 필수
│   └── {Domain}Service.java
├── controller/                   # 필수
│   └── {Domain}Controller.java
└── exception/                    # 권장
    └── {Domain}Exception.java

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

**검증 규칙**:
1. 필수 패키지 존재 여부 (dto, entity, repository, service, controller)
2. 필수 클래스 존재 여부
3. 필수 테스트 클래스 존재 여부
4. 불필요한 파일/패키지 감지
5. 패키지 경로가 com.jk.amazon2.{domain} 형식 준수

**검증 메서드**:
```java
public class PackageStructureChecker {
    
    public List<Issue> checkRequired() {
        // 필수 패키지 확인
        // 필수 클래스 확인
    }
    
    public List<Issue> checkOptional() {
        // 권장 패키지 확인
    }
    
    public List<Issue> checkTestStructure() {
        // 테스트 구조 확인
    }
}
```

### 2. 네이밍 컨벤션 검증 (Naming Convention Check)

**규칙**:

| 대상 | 규칙 | 예시 | 오류 예시 |
|------|------|------|----------|
| 패키지명 | 소문자 (snake_case 아님) | com.jk.amazon2.product | com.jk.amazon2.product_service ❌ |
| 클래스명 | PascalCase | ProductService, ProductResponse | productService ❌, product_service ❌ |
| 인터페이스명 | PascalCase, "I" 접두사 금지 | ProductRepository | IProductRepository ❌ |
| 메서드명 | camelCase | findByProductId() | find_by_product_id() ❌ |
| 상수명 | UPPER_SNAKE_CASE | DEFAULT_PAGE_SIZE | default_page_size ❌ |
| 필드명 | camelCase | private String productName | private String product_name ❌ |
| enum명 | UPPER_SNAKE_CASE | ACTIVE, INACTIVE | Active ❌ |
| DTO 클래스명 | {Domain}{Action}Request/Response | ProductCreateRequest | ProductRequest ❌ |

**검증 메서드**:
```java
public class NamingConventionChecker {
    
    public List<Issue> checkClassNames() {
        // 클래스명이 PascalCase인지 확인
    }
    
    public List<Issue> checkMethodNames() {
        // 메서드명이 camelCase인지 확인
    }
    
    public List<Issue> checkFieldNames() {
        // 필드명이 camelCase인지 확인
    }
    
    public List<Issue> checkConstantNames() {
        // 상수명이 UPPER_SNAKE_CASE인지 확인
    }
    
    public List<Issue> checkDTONames() {
        // DTO 네이밍이 규칙을 준수하는지 확인
    }
}
```

### 3. 어노테이션 검증 (Annotation Check)

**필수 어노테이션**:

| 클래스 | 필수 어노테이션 | 선택 어노테이션 |
|-------|---------------|----------------|
| Entity | @Entity, @Table | @Data, @Builder |
| Repository | extends JpaRepository | @Repository (자동) |
| Service | @Service, @Transactional | @RequiredArgsConstructor |
| Controller | @RestController, @RequestMapping | @RequiredArgsConstructor, @Tag |
| DTO | @Data, @Builder | @Valid, @Schema |
| Exception | extends RuntimeException | - |

**검증 규칙**:
1. Entity 클래스에 @Entity 존재 여부
2. Entity에 @Table(name="{domain}") 존재 여부
3. Service 클래스에 @Service 존재 여부
4. Service에 @Transactional 존재 여부
5. Controller에 @RestController 존재 여부
6. Controller에 @RequestMapping 존재 여부
7. DTO에 @Data 또는 @Getter/@Setter 존재 여부
8. DTO에 @Builder 존재 여부
9. 중복되거나 모순된 어노테이션 감지

**검증 메서드**:
```java
public class AnnotationChecker {
    
    public List<Issue> checkEntity() {
        // @Entity, @Table, @Id, @GeneratedValue 확인
    }
    
    public List<Issue> checkService() {
        // @Service, @Transactional 확인
    }
    
    public List<Issue> checkController() {
        // @RestController, @RequestMapping 확인
    }
    
    public List<Issue> checkDTO() {
        // @Data, @Builder, @Valid 확인
    }
}
```

### 4. DTO 구조 검증 (DTO Check)

**필수 DTO 클래스**:

1. **{Domain}CreateRequest**
   - 필수 필드: 비즈니스 로직 관련 필드들
   - 제외 필드: id, createdAt, updatedAt
   - Validation: @NotNull, @NotBlank, @Size 등
   - 메서드: 기본 생성자, getter/setter (또는 @Data)

2. **{Domain}UpdateRequest**
   - 필수 필드: 수정 가능한 필드들 (선택)
   - 제외 필드: id, createdAt, updatedAt
   - Validation: 선택적 (@NotNull 없음)

3. **{Domain}Response**
   - 필수 필드: 모든 조회 가능 필드
   - 포함 필드: id, createdAt, updatedAt
   - 메서드: fromEntity({Domain} entity) 정적 메서드
   - 메서드: toEntity() 메서드 (선택)

**검증 메서드**:
```java
public class DTOChecker {
    
    public List<Issue> checkCreateRequest() {
        // CreateRequest 필드 검증
        // Validation 어노테이션 확인
    }
    
    public List<Issue> checkUpdateRequest() {
        // UpdateRequest 필드 검증
    }
    
    public List<Issue> checkResponse() {
        // Response 필드 검증
        // fromEntity() 메서드 확인
    }
    
    public List<Issue> checkDTOConsistency() {
        // CreateRequest와 Entity 필드 일관성
        // UpdateRequest와 Entity 필드 일관성
    }
}
```

### 5. Exception 검증 (Exception Check)

**필수 Exception 클래스**:

1. **{Domain}NotFoundException**
   - extends RuntimeException
   - 생성자: (String message), (String message, Throwable cause)
   - 사용 위치: Service findById에서 404 발생

2. **{Domain}InvalidException**
   - extends RuntimeException
   - 생성자: (String message)
   - 사용 위치: Service create/update에서 검증 오류

**검증 규칙**:
1. 커스텀 Exception 클래스 존재 여부
2. RuntimeException을 상속받는지 확인
3. 필수 생성자 존재 여부
4. 에러 코드 정의 여부
5. 에러 메시지 정의 여부
6. Service에서 Exception 사용 여부

**검증 메서드**:
```java
public class ExceptionChecker {
    
    public List<Issue> checkExceptionClasses() {
        // 커스텀 Exception 존재 여부 확인
    }
    
    public List<Issue> checkExceptionUsage() {
        // Service에서 Exception 사용 여부 확인
    }
    
    public List<Issue> checkErrorCodes() {
        // 에러 코드 정의 여부 확인
    }
}
```

## 검증 알고리즘

### 1단계: 도메인 경로 확인
```
도메인 경로: src/main/java/com/jk/amazon2/{domain}/
검증: 경로 존재 여부, 디렉토리 구조
```

### 2단계: 클래스 파일 수집
```
필수 클래스:
- {domain}/dto/{Domain}CreateRequest.java
- {domain}/dto/{Domain}UpdateRequest.java
- {domain}/dto/{Domain}Response.java
- {domain}/entity/{Domain}.java
- {domain}/repository/{Domain}Repository.java
- {domain}/service/{Domain}Service.java
- {domain}/controller/{Domain}Controller.java

선택 클래스:
- {domain}/exception/{Domain}Exception.java
- {domain}/{Domain}Enum.java
```

### 3단계: AST(Abstract Syntax Tree) 파싱
```
각 파일에 대해:
1. 클래스명 추출
2. 어노테이션 목록 추출
3. 필드/메서드 목록 추출
4. 상속 클래스 추출
```

### 4단계: 규칙 검증
```
각 검증 항목별로:
1. 규칙 적용
2. 위반 사항 기록
3. 심각도 지정 (ERROR, WARNING, INFO)
```

### 5단계: 보고서 생성
```
형식: summary, detailed, json
내용:
- 총 이슈 수
- 심각도별 분류
- 각 이슈별 위치, 설명, 수정 방법
```

## 이슈 심각도

| 심각도 | 설명 | 예시 |
|-------|------|------|
| ERROR | 필수 사항 누락 | Entity에 @Entity 없음 |
| WARNING | 권장사항 미준수 | DTO에 @Builder 없음 |
| INFO | 개선 제안 | 메서드 문서화 부족 |

## 자동 수정 (auto_fix=true)

```
자동으로 수정 가능한 항목:
- 클래스에 누락된 필수 어노테이션 추가
- DTO에 필요한 필드 추가
- 네이밍 규칙에 맞춰 변수명 리팩토링
- getter/setter 추가 또는 @Data 추가

자동 수정 불가 항목:
- 누락된 클래스 생성
- 복잡한 비즈니스 로직
- Exception 사용 위치
```

## 보고서 형식

### Summary 형식
```
검증 완료: {domain}
- 총 이슈: 5개
- ERROR: 2개, WARNING: 2개, INFO: 1개
```

### Detailed 형식
```
검증 완료: {domain}

[ERROR] Entity에 @Table 어노테이션 누락
위치: src/main/java/com/jk/amazon2/product/entity/Product.java:1
설명: @Entity 클래스는 @Table(name="...") 어노테이션이 필요합니다
수정: @Table(name = "product") 추가

[WARNING] DTO에 @Builder 어노테이션 누락
위치: src/main/java/com/jk/amazon2/product/dto/ProductCreateRequest.java:3
설명: 빌더 패턴 사용을 위해 @Builder 어노테이션을 추가하면 좋습니다
수정: @Builder 어노테이션 추가

...
```

### JSON 형식
```json
{
  "domain": "product",
  "timestamp": "2024-01-01T00:00:00",
  "total_issues": 5,
  "issues": [
    {
      "severity": "ERROR",
      "category": "annotation",
      "location": "src/main/java/com/jk/amazon2/product/entity/Product.java:1",
      "message": "@Entity 클래스는 @Table(name=\"...\") 어노테이션이 필요합니다",
      "suggestion": "@Table(name = \"product\") 추가"
    }
  ]
}
```

## 생성 파일 목록

```
생성될 파일:
- {검증 보고서}.txt 또는 .json
- {auto_fix=true인 경우} 수정된 소스 파일들
```

## 참고사항
- Amazon2 프로젝트의 member 패키지 구조 참조
- harnesses/member/README.md 개발 가이드 준수
- Spring Boot 4.0.0, Java 21 호환성 확인
- 각 규칙은 .claude/errors/ERROR_PATTERNS.md 참조
