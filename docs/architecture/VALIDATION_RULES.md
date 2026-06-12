# 아키텍처 검증 규칙 (Architecture Validation Rules)

Amazon2 프로젝트의 도메인 기반 아키텍처 검증 규칙을 정의합니다.
모든 개발자와 에이전트가 이 규칙을 준수하여 일관성 있는 코드 구조를 유지합니다.

---

## 목차

1. [패키지 구조](#패키지-구조)
2. [네이밍 규칙](#네이밍-규칙)
3. [계층 구조](#계층-구조)
4. [의존성 규칙](#의존성-규칙)
5. [테스트 구조](#테스트-구조)
6. [예외 처리](#예외-처리)
7. [검증 체크리스트](#검증-체크리스트)

---

## 패키지 구조

### 기본 패턴

```
com.jk.amazon2/
├── [domain]/              # 도메인별 패키지
│   ├── entity/            # JPA 엔티티 (도메인 모델)
│   ├── repository/        # DB 접근 계층
│   ├── service/           # 비즈니스 로직
│   ├── dto/               # Request/Response DTO
│   ├── controller/        # REST API 엔드포인트
│   └── exception/         # 도메인 예외 (ErrorCode)
├── common/                # 공통 유틸, 상수
├── config/                # Spring 설정
└── exception/             # 글로벌 예외 처리
```

### 도메인 목록

| 도메인 | 패키지 | 설명 |
|--------|--------|------|
| 회원 | `com.jk.amazon2.member` | 회원 관리 (조회, 등록, 수정, 삭제) |
| 카테고리 | `com.jk.amazon2.category` | 카테고리 관리 |
| 포스팅 | `com.jk.amazon2.posting` | 포스팅 관리 (조회, 통계) |

### 예시

✅ **올바른 패키지 구조:**
```
com.jk.amazon2.member.entity.Member
com.jk.amazon2.member.repository.MemberRepository
com.jk.amazon2.member.service.MemberCommandService
com.jk.amazon2.member.controller.MemberController
com.jk.amazon2.category.exception.CategoryErrorCode
```

---

## 네이밍 규칙

### Entity (엔티티)

**패턴:** `{DomainName}`

| 도메인 | 클래스명 |
|--------|---------|
| Member | `Member` |
| Category | `Category` |
| Posting | `Posting` |

**규칙:**
- PascalCase 사용
- 단수형으로 명명
- 도메인을 명확하게 표현

**예시:**
```java
@Entity
@Table(name = "members")
public class Member { ... }
```

### Repository (저장소)

**패턴:** `{DomainName}Repository`

**규칙:**
- `JpaRepository<Entity, ID>` 확장 (또는 Custom Repository)
- 도메인명 + "Repository" 접미사
- 복잡한 쿼리는 `spec` 패키지에서 QueryDSL 사용

**예시:**
```java
public interface MemberRepository extends JpaRepository<Member, Long> {
    Optional<Member> findByEmail(String email);
}
```

**복잡한 저장소:**
```
com.jk.amazon2.category.repository/
├── CategoryRepository.java
└── spec/
    ├── CategorySpec.java
    └── CategoryPredicate.java
```

### Service (서비스)

**패턴:** `{DomainName}{CommandOrQuery}Service` 또는 `{DomainName}Service`

| 분류 | 클래스명 | 책임 |
|------|---------|------|
| Command | `MemberCommandService` | Create, Update, Delete |
| Query | `MemberQueryService` | Read (조회) |
| Unified | `CategoryService` | 모든 작업 (간단한 도메인) |

**규칙:**
- 복잡한 도메인: Command/Query 분리 권장
- 간단한 도메인: 통합 서비스 가능
- 비즈니스 로직 집중

**예시:**
```java
@Service
@RequiredArgsConstructor
public class MemberCommandService {
    private final MemberRepository memberRepository;
    
    public MemberResponse create(MemberCreateRequest request) { ... }
    public void update(Long id, MemberUpdateRequest request) { ... }
}
```

### Controller (컨트롤러)

**패턴:** `{DomainName}Controller`

| 도메인 | 클래스명 |
|--------|---------|
| Member | `MemberController` |
| Category | `CategoryController` |
| Posting | `PostingController` |

**규칙:**
- REST API 리소스 중심 설계
- HTTP 메서드 기반 매핑 (`@GetMapping`, `@PostMapping` 등)
- Request/Response DTO 사용

**예시:**
```java
@RestController
@RequestMapping("/api/v1/members")
@RequiredArgsConstructor
public class MemberController {
    private final MemberQueryService memberQueryService;
    private final MemberCommandService memberCommandService;
    
    @GetMapping("/{id}")
    public ResponseEntity<MemberResponse> getMember(@PathVariable Long id) { ... }
}
```

### DTO (데이터 전송 객체)

**패턴:** `{DomainName}{Request|Response|Dto}`

| 분류 | 예시 |
|------|------|
| Request | `MemberCreateRequest`, `MemberUpdateRequest` |
| Response | `MemberResponse`, `MemberListResponse` |
| Internal | `MemberDto` |

**규칙:**
- Controller 입출력용 Request/Response 분리
- 서비스 간 데이터 전달은 별도의 내부 DTO 사용 가능
- Lombok `@Data`, `@Builder` 활용

**예시:**
```java
@Data
@Builder
public class MemberCreateRequest {
    private String email;
    private String name;
}

@Data
public class MemberResponse {
    private Long id;
    private String email;
    private String name;
}
```

### Exception (예외)

**패턴:** `{DomainName}ErrorCode`

**규칙:**
- 각 도메인에서 `ErrorCode` 인터페이스 구현
- HTTP 상태 코드 + 에러 메시지 정의
- `GlobalExceptionHandler`에서 일괄 처리

**예시:**
```java
public enum MemberErrorCode implements ErrorCode {
    MEMBER_NOT_FOUND(HttpStatus.NOT_FOUND, "M001", "회원을 찾을 수 없습니다"),
    DUPLICATE_EMAIL(HttpStatus.CONFLICT, "M002", "이미 사용 중인 이메일입니다"),
    INVALID_PASSWORD(HttpStatus.BAD_REQUEST, "M003", "비밀번호가 유효하지 않습니다");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}
```

---

## 계층 구조

### 아키텍처 계층 정의

| 계층 | 역할 | 의존성 | 예시 |
|------|------|--------|------|
| **Controller** | REST API 엔드포인트 | Service, DTO, Exception | HTTP 요청 처리 |
| **Service** | 비즈니스 로직 | Repository, Entity, DTO, Exception | 트랜잭션, 검증 |
| **Repository** | DB 접근 | Entity | 쿼리 실행, 데이터 영속성 |
| **Entity** | 도메인 모델 | Common | JPA, 연관관계 |
| **DTO** | 데이터 전송 | Common | 요청/응답 매핑 |
| **Exception** | 예외 처리 | Common | 에러 코드 정의 |

### 계층 간 의존성 흐름

```
Client (HTTP)
    ↓
Controller (API 엔드포인트)
    ↓
Service (비즈니스 로직)
    ↓
Repository (DB 접근)
    ↓
Entity (도메인 모델)
    ↓
Database
```

---

## 의존성 규칙

### 허용되는 의존성 (Allowed)

```yaml
Controller → Service, DTO, Exception
Service    → Repository, Entity, DTO, Exception, Common
Repository → Entity, Common
Entity     → Common
DTO        → Common
Exception  → Common
```

✅ **올바른 의존성:**
```java
// Controller → Service (O)
@RestController
public class MemberController {
    @Autowired
    private MemberCommandService service;
}

// Service → Repository (O)
@Service
public class MemberCommandService {
    @Autowired
    private MemberRepository repository;
}

// Service → Entity (O)
public class MemberCommandService {
    public void delete(Long id) {
        Member member = repository.findById(id)
            .orElseThrow(() -> new MemberNotFoundException());
    }
}
```

### 금지되는 의존성 (Forbidden)

```yaml
Entity        → Service, Controller, Repository
Repository    → Service, Controller
Service       → Controller
Controller    → Repository  # 반드시 Service를 거쳐야 함
```

❌ **잘못된 의존성:**
```java
// Controller → Repository (X) - Service를 거쳐야 함
@RestController
public class MemberController {
    @Autowired
    private MemberRepository repository;  // 금지!
    
    @GetMapping("/{id}")
    public MemberResponse getMember(@PathVariable Long id) {
        return repository.findById(id);  // 금지!
    }
}

// Entity → Service (X) - 양방향 의존성 생성
@Entity
public class Member {
    @Autowired
    private MemberService service;  // 금지!
}
```

### 도메인 간 의존성

**원칙:**
- 도메인 간 의존성은 최소화
- API 계층 (Controller/DTO)에서만 허용
- 비즈니스 로직 계층 (Service/Repository)에서는 금지

✅ **허용:**
```java
// Controller에서 다른 도메인 DTO 참조 (O)
@RestController
public class PostingController {
    @PostMapping
    public PostingResponse create(@RequestBody PostingCreateRequest request) {
        // MemberDto를 response에 포함 가능
    }
}
```

❌ **금지:**
```java
// Service에서 다른 도메인 Service 호출 (X)
@Service
public class PostingCommandService {
    @Autowired
    private MemberCommandService memberService;  // 금지!
}
```

---

## 테스트 구조

### 테스트 파일 위치

```
src/test/java/com/jk/amazon2/[domain]/
├── entity/
│   └── *Test.java
├── repository/
│   └── *RepositoryTest.java
├── service/
│   └── *ServiceTest.java
└── controller/
    └── *ControllerTest.java
```

### 테스트 타입별 규칙

#### 1. Entity Test

**파일명:** `{EntityName}Test.java`

**위치:** `src/test/java/com/jk/amazon2/{domain}/entity/`

**포커스:**
- JPA 어노테이션 검증
- 엔티티 필드 및 메서드 로직
- 연관관계 (OneToMany, ManyToOne 등)

**예시:**
```java
@DataJpaTest
class MemberTest {
    @Test
    void testMemberCreation() {
        Member member = Member.builder()
            .email("test@test.com")
            .name("Test User")
            .build();
        
        assertThat(member.getEmail()).isEqualTo("test@test.com");
    }
}
```

#### 2. Repository Test

**파일명:** `{RepositoryName}RepositoryTest.java`

**위치:** `src/test/java/com/jk/amazon2/{domain}/repository/`

**포커스:**
- CRUD 쿼리 검증
- 데이터 영속성
- 복잡한 쿼리 로직 (QueryDSL)

**예시:**
```java
@DataJpaTest
class MemberRepositoryTest {
    @Autowired
    private MemberRepository memberRepository;
    
    @Test
    void testFindByEmail() {
        Member member = memberRepository.save(
            Member.builder().email("test@test.com").build()
        );
        
        Optional<Member> found = memberRepository.findByEmail("test@test.com");
        assertThat(found).isPresent();
    }
}
```

#### 3. Service Test

**파일명:** `{ServiceName}ServiceTest.java`

**위치:** `src/test/java/com/jk/amazon2/{domain}/service/`

**포커스:**
- 비즈니스 로직 검증
- 예외 처리
- 트랜잭션 동작

**예시:**
```java
@ExtendWith(MockitoExtension.class)
class MemberCommandServiceTest {
    @Mock
    private MemberRepository memberRepository;
    
    @InjectMocks
    private MemberCommandService memberCommandService;
    
    @Test
    void testCreateMember() {
        MemberCreateRequest request = new MemberCreateRequest("test@test.com", "Test");
        
        Member member = memberCommandService.create(request);
        
        assertThat(member.getEmail()).isEqualTo("test@test.com");
    }
}
```

#### 4. Controller Test

**파일명:** `{ControllerName}ControllerTest.java`

**위치:** `src/test/java/com/jk/amazon2/{domain}/controller/`

**포커스:**
- HTTP 요청/응답 매핑
- 상태 코드 검증
- API 명세 확인

**예시:**
```java
@WebMvcTest(MemberController.class)
class MemberControllerTest {
    @Autowired
    private MockMvc mockMvc;
    
    @MockBean
    private MemberQueryService memberQueryService;
    
    @Test
    void testGetMember() throws Exception {
        MemberResponse response = new MemberResponse(1L, "test@test.com");
        
        when(memberQueryService.getMember(1L))
            .thenReturn(response);
        
        mockMvc.perform(get("/api/v1/members/1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.email").value("test@test.com"));
    }
}
```

---

## 예외 처리

### 글로벌 예외 처리 구조

```
GlobalExceptionHandler (com.jk.amazon2.exception)
    ↑ (catch)
├── MemberErrorCode (도메인 예외)
├── CategoryErrorCode
└── PostingErrorCode
```

### ErrorCode 인터페이스 정의

**위치:** `com.jk.amazon2.common.exception.ErrorCode`

```java
public interface ErrorCode {
    HttpStatus getHttpStatus();
    String getCode();
    String getMessage();
}
```

### 도메인 ErrorCode 구현

**파일:** `com.jk.amazon2.{domain}.exception.{DomainName}ErrorCode`

```java
public enum MemberErrorCode implements ErrorCode {
    MEMBER_NOT_FOUND(
        HttpStatus.NOT_FOUND, 
        "M001", 
        "회원을 찾을 수 없습니다"
    ),
    DUPLICATE_EMAIL(
        HttpStatus.CONFLICT, 
        "M002", 
        "이미 사용 중인 이메일입니다"
    );

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;

    MemberErrorCode(HttpStatus httpStatus, String code, String message) {
        this.httpStatus = httpStatus;
        this.code = code;
        this.message = message;
    }

    @Override
    public HttpStatus getHttpStatus() { return httpStatus; }
    @Override
    public String getCode() { return code; }
    @Override
    public String getMessage() { return message; }
}
```

### 비즈니스 예외 클래스

**패턴:** `{DomainName}Exception extends RuntimeException`

```java
public class MemberNotFoundException extends RuntimeException {
    private final MemberErrorCode errorCode;

    public MemberNotFoundException() {
        super(MemberErrorCode.MEMBER_NOT_FOUND.getMessage());
        this.errorCode = MemberErrorCode.MEMBER_NOT_FOUND;
    }

    public MemberErrorCode getErrorCode() {
        return errorCode;
    }
}
```

### GlobalExceptionHandler 처리

**파일:** `com.jk.amazon2.exception.GlobalExceptionHandler`

```java
@RestControllerAdvice
public class GlobalExceptionHandler {
    
    @ExceptionHandler(MemberNotFoundException.class)
    public ResponseEntity<?> handleMemberNotFoundException(
        MemberNotFoundException e) {
        return ResponseEntity
            .status(e.getErrorCode().getHttpStatus())
            .body(new ErrorResponse(
                e.getErrorCode().getCode(),
                e.getErrorCode().getMessage()
            ));
    }
}
```

---

## 검증 체크리스트

### 신규 도메인 추가 시

- [ ] 패키지 생성: `com.jk.amazon2.{domain}`
- [ ] 하위 계층 패키지 생성: `entity`, `repository`, `service`, `dto`, `controller`, `exception`
- [ ] Entity 클래스 작성 및 테스트
- [ ] Repository 인터페이스 작성 및 테스트
- [ ] Service 클래스 작성 및 테스트
- [ ] Controller 클래스 작성 및 테스트
- [ ] ErrorCode Enum 정의
- [ ] 개발 가이드 문서 작성 (`harnesses/{domain}/README.md`)

### 코드 리뷰 시

- [ ] 패키지 네이밍: `com.jk.amazon2.{domain}.{layer}` 준수
- [ ] 클래스 네이밍: 규칙 준수 (Controller, Service, Repository, Entity 등)
- [ ] 의존성 방향: 상향만 허용, 순환 의존성 없음
- [ ] 도메인 간 의존성: API 계층에서만 허용
- [ ] Controller → Service → Repository 의존성 흐름 준수
- [ ] 테스트 파일 위치: `src/test/java/com/jk/amazon2/{domain}/{layer}/`
- [ ] 예외 처리: ErrorCode 구현 + GlobalExceptionHandler 연동

### 아키텍처 검증 자동화

**YAML 규칙 파일:** `.claude/agents/shared-foundation.yaml`

이 파일은 다음 도구에서 사용됩니다:
- 에이전트 기반 코드 검증
- IDE 플러그인 (ArchUnit, NArchitecture)
- CI/CD 파이프라인 (정적 분석)

---

## 참고 자료

- **프로젝트 CLAUDE.md**: `/CLAUDE.md`
- **개발 가이드**: `/docs/DEVELOPMENT.md`
- **기여 규칙**: `/docs/CONTRIBUTING.md`
- **아키텍처 공유 기반**: `/.claude/agents/shared-foundation.yaml`
- **ERD**: `/docs/ERD.md`