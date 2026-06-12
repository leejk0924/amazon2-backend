# 하네스 엔지니어링 개발 가이드

새로운 도메인 추가나 기능 개발 시 다음을 따릅니다.

---

## 새 도메인 추가 체크리스트

### 1. 패키지 구조 생성

```bash
mkdir -p src/main/java/com/jk/amazon2/[domain]/{controller,service,repository,entity,dto,exception}
mkdir -p src/test/java/com/jk/amazon2/[domain]/{controller,service,repository}
```

### 2. 개발 순서

#### 1단계: Entity 작성 (`entity/[Domain].java`)

JPA 엔티티를 정의합니다.

```java
package com.jk.amazon2.[domain].entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "[domain]")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class [Domain] {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "name", nullable = false)
    private String name;
    
    // 필드들...
    
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
    
    @PrePersist
    void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }
    
    @PreUpdate
    void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
```

**주의사항:**
- `@Entity`, `@Table`, `@Id` 등의 어노테이션 설정
- 관계 설정 필요시 `@ManyToOne`, `@OneToMany`, `@JoinColumn` 등 사용
- `createdAt`, `updatedAt` 필드는 필수
- `@PrePersist`, `@PreUpdate` 메서드로 자동 관리

#### 2단계: Repository 작성 (`repository/[Domain]Repository.java`)

데이터 접근을 담당합니다.

```java
package com.jk.amazon2.[domain].repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.jk.amazon2.[domain].entity.[Domain];

@Repository
public interface [Domain]Repository extends JpaRepository<[Domain], Long> {
    
    // 커스텀 쿼리 메서드
    Optional<[Domain]> findByName(String name);
    
    // QueryDSL 사용 시 인터페이스 상속
    // List<[Domain]> searchBy[Criteria]([Criteria]Request request);
}
```

**QueryDSL 사용 시:**

```java
// 1. 레포지토리 인터페이스 분리
@Repository
public interface [Domain]Repository extends JpaRepository<[Domain], Long>, 
                                           [Domain]RepositoryCustom {
}

// 2. 커스텀 인터페이스
public interface [Domain]RepositoryCustom {
    List<[Domain]> searchBy[Criteria]([Criteria]Request request);
}

// 3. 커스텀 구현 클래스
@Repository
public class [Domain]RepositoryImpl implements [Domain]RepositoryCustom {
    
    private final JPAQueryFactory queryFactory;
    
    public [Domain]RepositoryImpl(JPAQueryFactory queryFactory) {
        this.queryFactory = queryFactory;
    }
    
    @Override
    public List<[Domain]> searchBy[Criteria]([Criteria]Request request) {
        // QueryDSL 구현
        return queryFactory
            .selectFrom(Q[Domain].[domain])
            .where(/* 조건 */)
            .fetch();
    }
}
```

#### 3단계: Service 작성 (Command/Query 분리)

비즈니스 로직을 담당합니다.

**CommandService 작성** (`service/[Domain]CommandService.java`):

```java
package com.jk.amazon2.[domain].service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import com.jk.amazon2.[domain].repository.[Domain]Repository;
import com.jk.amazon2.[domain].entity.[Domain];
import com.jk.amazon2.[domain].dto.[Domain]Request;
import com.jk.amazon2.[domain].dto.[Domain]Response;
import com.jk.amazon2.[domain].exception.[Domain]Exception;
import com.jk.amazon2.[domain].exception.[Domain]ErrorCode;

@Service
@RequiredArgsConstructor
@Transactional
public class [Domain]CommandService {
    
    private final [Domain]Repository repository;
    
    // 생성
    public [Domain]Response create([Domain]Request.Create request) {
        // 검증
        validateCreate(request);
        
        // 저장
        [Domain] entity = request.toEntity();
        [Domain] saved = repository.save(entity);
        
        return [Domain]Response.from(saved);
    }
    
    // 수정
    public [Domain]Response update(Long id, [Domain]Request.Update request) {
        [Domain] entity = findById(id);
        entity.update(request);
        
        return [Domain]Response.from(entity);
    }
    
    // 삭제
    public void delete(Long id) {
        [Domain] entity = findById(id);
        repository.delete(entity);
    }
    
    // 헬퍼 메서드
    private [Domain] findById(Long id) {
        return repository.findById(id)
            .orElseThrow(() -> new [Domain]Exception([Domain]ErrorCode.NOT_FOUND));
    }
    
    private void validateCreate([Domain]Request.Create request) {
        // 검증 로직
    }
}
```

**QueryService 작성** (`service/[Domain]QueryService.java`):

```java
package com.jk.amazon2.[domain].service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import com.jk.amazon2.[domain].repository.[Domain]Repository;
import com.jk.amazon2.[domain].entity.[Domain];
import com.jk.amazon2.[domain].dto.[Domain]Response;
import com.jk.amazon2.[domain].exception.[Domain]Exception;
import com.jk.amazon2.[domain].exception.[Domain]ErrorCode;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class [Domain]QueryService {
    
    private final [Domain]Repository repository;
    
    // 개별 조회
    public [Domain]Response getById(Long id) {
        [Domain] entity = findById(id);
        return [Domain]Response.from(entity);
    }
    
    // 목록 조회
    public List<[Domain]Response> getAll() {
        return repository.findAll().stream()
            .map([Domain]Response::from)
            .toList();
    }
    
    // 헬퍼 메서드
    private [Domain] findById(Long id) {
        return repository.findById(id)
            .orElseThrow(() -> new [Domain]Exception([Domain]ErrorCode.NOT_FOUND));
    }
}
```

#### 4단계: DTO 작성

클라이언트 요청/응답을 정의합니다.

```java
package com.jk.amazon2.[domain].dto;

import lombok.*;
import com.jk.amazon2.[domain].entity.[Domain];

// Request DTO
public class [Domain]Request {
    
    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Create {
        private String name;
        private String description;
        
        // 검증
        public void validate() {
            if (name == null || name.isBlank()) {
                throw new IllegalArgumentException("name은 필수입니다");
            }
        }
        
        public [Domain] toEntity() {
            return [Domain].builder()
                .name(name)
                .description(description)
                .build();
        }
    }
    
    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Update {
        private String name;
        private String description;
    }
}

// Response DTO
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class [Domain]Response {
    private Long id;
    private String name;
    private String description;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    public static [Domain]Response from([Domain] entity) {
        return [Domain]Response.builder()
            .id(entity.getId())
            .name(entity.getName())
            .description(entity.getDescription())
            .createdAt(entity.getCreatedAt())
            .updatedAt(entity.getUpdatedAt())
            .build();
    }
}
```

#### 5단계: Exception 작성

예외 처리를 정의합니다.

**Exception 클래스** (`exception/[Domain]Exception.java`):

```java
package com.jk.amazon2.[domain].exception;

import com.jk.amazon2.common.exception.ErrorCode;

public class [Domain]Exception extends RuntimeException {
    
    private final ErrorCode errorCode;
    
    public [Domain]Exception(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }
    
    public ErrorCode getErrorCode() {
        return errorCode;
    }
}
```

**ErrorCode Enum** (`exception/[Domain]ErrorCode.java`):

```java
package com.jk.amazon2.[domain].exception;

import com.jk.amazon2.common.exception.ErrorCode;
import org.springframework.http.HttpStatus;

public enum [Domain]ErrorCode implements ErrorCode {
    
    NOT_FOUND("CD_001", "조회할 [domain]이 없습니다", HttpStatus.NOT_FOUND),
    ALREADY_EXISTS("CD_002", "[domain]이 이미 존재합니다", HttpStatus.CONFLICT),
    INVALID_REQUEST("CD_003", "잘못된 요청입니다", HttpStatus.BAD_REQUEST),
    ;
    
    private final String code;
    private final String message;
    private final HttpStatus httpStatus;
    
    [Domain]ErrorCode(String code, String message, HttpStatus httpStatus) {
        this.code = code;
        this.message = message;
        this.httpStatus = httpStatus;
    }
    
    @Override
    public String getCode() {
        return code;
    }
    
    @Override
    public String getMessage() {
        return message;
    }
    
    @Override
    public HttpStatus getHttpStatus() {
        return httpStatus;
    }
}
```

#### 6단계: Controller 작성

REST API 엔드포인트를 정의합니다.

```java
package com.jk.amazon2.[domain].controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import lombok.RequiredArgsConstructor;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import com.jk.amazon2.[domain].service.[Domain]CommandService;
import com.jk.amazon2.[domain].service.[Domain]QueryService;
import com.jk.amazon2.[domain].dto.[Domain]Request;
import com.jk.amazon2.[domain].dto.[Domain]Response;

@RestController
@RequestMapping("/api/v1/[domains]")
@RequiredArgsConstructor
@Tag(name = "[Domain] API", description = "[domain] 관리 API")
public class [Domain]Controller {
    
    private final [Domain]CommandService commandService;
    private final [Domain]QueryService queryService;
    
    @PostMapping
    @Operation(summary = "[domain] 생성", description = "새로운 [domain]을 생성합니다")
    public ResponseEntity<[Domain]Response> create(@RequestBody [Domain]Request.Create request) {
        [Domain]Response response = commandService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
    
    @GetMapping("/{id}")
    @Operation(summary = "[domain] 조회", description = "ID로 [domain]을 조회합니다")
    public ResponseEntity<[Domain]Response> getById(@PathVariable Long id) {
        [Domain]Response response = queryService.getById(id);
        return ResponseEntity.ok(response);
    }
    
    @GetMapping
    @Operation(summary = "[domain] 목록 조회", description = "모든 [domain]을 조회합니다")
    public ResponseEntity<List<[Domain]Response>> getAll() {
        List<[Domain]Response> responses = queryService.getAll();
        return ResponseEntity.ok(responses);
    }
    
    @PutMapping("/{id}")
    @Operation(summary = "[domain] 수정", description = "[domain]을 수정합니다")
    public ResponseEntity<[Domain]Response> update(
            @PathVariable Long id,
            @RequestBody [Domain]Request.Update request) {
        [Domain]Response response = commandService.update(id, request);
        return ResponseEntity.ok(response);
    }
    
    @DeleteMapping("/{id}")
    @Operation(summary = "[domain] 삭제", description = "[domain]을 삭제합니다")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        commandService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
```

#### 7단계: 테스트 작성

Testcontainers 기반 통합 테스트를 작성합니다.

**Controller 통합 테스트** (`src/test/java/com/jk/amazon2/[domain]/controller/[Domain]ControllerTest.java`):

```java
package com.jk.amazon2.[domain].controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jk.amazon2.[domain].dto.[Domain]Request;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.*;

@SpringBootTest
@Testcontainers
@AutoConfigureMockMvc
@DisplayName("[Domain] Controller 통합 테스트")
class [Domain]ControllerTest {
    
    @Container
    static MySQLContainer<?> mysql = 
        new MySQLContainer<>(DockerImageName.parse("mysql:8.0"))
            .withDatabaseName("amazon2");
    
    @Autowired
    private MockMvc mockMvc;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    @Test
    @DisplayName("GET /api/v1/[domains] - 목록 조회")
    void testGetAll() throws Exception {
        mockMvc.perform(get("/api/v1/[domains]"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(0)));
    }
    
    @Test
    @DisplayName("POST /api/v1/[domains] - 생성")
    void testCreate() throws Exception {
        [Domain]Request.Create request = [Domain]Request.Create.builder()
            .name("Test [Domain]")
            .description("Test Description")
            .build();
        
        mockMvc.perform(post("/api/v1/[domains]")
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id", notNullValue()))
            .andExpect(jsonPath("$.name", equalTo("Test [Domain]")));
    }
}
```

**Service 단위 테스트** (`src/test/java/com/jk/amazon2/[domain]/service/[Domain]CommandServiceTest.java`):

```java
package com.jk.amazon2.[domain].service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.assertj.core.api.Assertions.*;
import com.jk.amazon2.[domain].repository.[Domain]Repository;
import com.jk.amazon2.[domain].entity.[Domain];
import com.jk.amazon2.[domain].dto.[Domain]Request;
import com.jk.amazon2.[domain].exception.[Domain]Exception;

@ExtendWith(MockitoExtension.class)
@DisplayName("[Domain]CommandService 단위 테스트")
class [Domain]CommandServiceTest {
    
    @Mock
    private [Domain]Repository repository;
    
    @InjectMocks
    private [Domain]CommandService service;
    
    @Test
    @DisplayName("create() - 정상 생성")
    void testCreate() {
        // given
        [Domain]Request.Create request = [Domain]Request.Create.builder()
            .name("Test")
            .build();
        
        [Domain] entity = [Domain].builder()
            .id(1L)
            .name("Test")
            .build();
        
        when(repository.save(any([Domain].class))).thenReturn(entity);
        
        // when
        var response = service.create(request);
        
        // then
        assertThat(response.getName()).isEqualTo("Test");
        verify(repository, times(1)).save(any([Domain].class));
    }
    
    @Test
    @DisplayName("delete() - [domain] 없으면 예외")
    void testDeleteNotFound() {
        // given
        when(repository.findById(anyLong())).thenReturn(Optional.empty());
        
        // when & then
        assertThatThrownBy(() -> service.delete(1L))
            .isInstanceOf([Domain]Exception.class);
    }
}
```

### 3. 예외 처리 등록

`GlobalExceptionHandler`에 새 도메인 예외 핸들러를 추가합니다.

**파일**: `src/main/java/com/jk/amazon2/common/exception/GlobalExceptionHandler.java`

```java
package com.jk.amazon2.common.exception;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import com.jk.amazon2.[domain].exception.[Domain]Exception;

@RestControllerAdvice
public class GlobalExceptionHandler {
    
    // 기존 핸들러들...
    
    @ExceptionHandler([Domain]Exception.class)
    public ResponseEntity<ErrorResponse> handle[Domain]Exception([Domain]Exception e) {
        return ResponseEntity.status(e.getErrorCode().getHttpStatus())
            .body(new ErrorResponse(e.getErrorCode().getCode(), e.getMessage()));
    }
}
```

### 4. 커밋 메시지 규칙

Conventional Commits 규칙을 준수합니다. 자세한 내용은 [docs/CONTRIBUTING.md](CONTRIBUTING.md)를 참조하세요.

**예시:**

```
Feat: [domain] CRUD API 구현

- add: [Domain]Controller REST 엔드포인트
- add: [Domain]CommandService 생성/수정/삭제
- add: [Domain]QueryService 조회
- add: [Domain]Repository JPA 레포지토리
- add: [Domain]Entity 엔티티
- add: [Domain]DTO Request/Response
- add: [Domain]Exception/ErrorCode 예외 처리
- add: GlobalExceptionHandler에 [Domain]Exception 핸들러 등록
- test: [Domain] 통합 테스트
```

---

## 패키지 구조와 책임

### 계층 구조

```
src/main/java/com/jk/amazon2/[domain]/
├── controller/          REST API 엔드포인트
│   └── [Domain]Controller.java
├── service/             비즈니스 로직
│   ├── [Domain]CommandService.java    (Create, Update, Delete)
│   └── [Domain]QueryService.java      (Read)
├── repository/          데이터 접근
│   ├── [Domain]Repository.java
│   ├── [Domain]RepositoryCustom.java  (QueryDSL용)
│   └── [Domain]RepositoryImpl.java     (QueryDSL 구현)
├── entity/              JPA 엔티티
│   └── [Domain].java
├── dto/                 요청/응답 DTO
│   ├── [Domain]Request.java
│   └── [Domain]Response.java
└── exception/           도메인 예외
    ├── [Domain]Exception.java
    └── [Domain]ErrorCode.java
```

### 기존 도메인

#### member/ - 회원 도메인
- 회원 조회, 등록, 수정, 삭제
- 회원 소프트/영구 삭제
- 회원 검증 및 인증
- 관련 문서: [harnesses/member/README.md](../harnesses/member/README.md)

#### category/ - 카테고리 도메인
- 카테고리 CRUD 작업
- 카테고리 검증
- 카테고리 조회 (개별, 목록, 코드별)
- 관련 문서: [harnesses/category/README.md](../harnesses/category/README.md)

#### posting/ - 포스팅 도메인
- 포스팅 조회, 검색
- 포스팅 통계
- 포스팅 필터링
- 관련 문서: [harnesses/posting/README.md](../harnesses/posting/README.md)

### common/ - 공통 코드

```
src/main/java/com/jk/amazon2/common/
├── exception/           글로벌 예외 처리
│   ├── ErrorCode.java              에러 코드 인터페이스
│   ├── GlobalExceptionHandler.java  글로벌 예외 핸들러
│   └── ErrorResponse.java           에러 응답 DTO
├── constant/            공통 상수
│   └── [Constants].java
├── dto/                 공통 DTO (여러 도메인에서 사용)
│   └── [Common].java
└── utils/               유틸리티 클래스
    └── [Utility].java
```

### config/ - Spring 설정

```
src/main/java/com/jk/amazon2/config/
├── DataSourceConfig.java    데이터베이스 설정
├── JpaConfig.java           JPA 설정 (QueryDSL 등)
├── SwaggerConfig.java       Swagger/OpenAPI 설정
└── [OtherConfig].java       기타 설정
```

---

## 테스트 작성 가이드

### Testcontainers 환경 설정

**의존성** (build.gradle):

```gradle
dependencies {
    // Spring Test
    testImplementation 'org.springframework.boot:spring-boot-starter-test'
    testImplementation 'org.springframework.boot:spring-boot-testcontainers'
    
    // Testcontainers
    testImplementation 'org.testcontainers:testcontainers:1.19.0'
    testImplementation 'org.testcontainers:junit-jupiter:1.19.0'
    testImplementation 'org.testcontainers:mysql:1.19.0'
    
    // REST Assured
    testImplementation 'io.rest-assured:rest-assured:5.3.1'
}
```

### 테스트 필수 항목

#### 1. Controller 통합 테스트

각 엔드포인트가 정상 작동하는지 확인합니다.

- 정상 요청/응답 확인
- 요청 데이터 검증 확인
- 예외 처리 확인
- HTTP 상태 코드 확인

**체크리스트:**
- [ ] 모든 GET 엔드포인트 테스트
- [ ] 모든 POST 엔드포인트 테스트
- [ ] 모든 PUT 엔드포인트 테스트
- [ ] 모든 DELETE 엔드포인트 테스트
- [ ] 예외 응답 (404, 400, 409 등) 테스트
- [ ] 데이터 바인딩 테스트

#### 2. Service 단위 테스트

비즈니스 로직의 정확성을 검증합니다.

- 정상 케이스 테스트
- Edge case 처리 확인
- 예외 발생 확인
- 데이터 일관성 확인

**체크리스트:**
- [ ] CommandService의 각 메서드 테스트
- [ ] QueryService의 각 메서드 테스트
- [ ] 유효성 검증 로직 테스트
- [ ] 중복/충돌 검사 테스트
- [ ] 트랜잭션 처리 테스트

#### 3. Repository 데이터 접근 테스트

JPA 쿼리와 데이터 영속성을 확인합니다.

- 커스텀 쿼리 메서드 정확성
- 데이터 저장/조회 확인
- QueryDSL 쿼리 정확성 (사용시)

**체크리스트:**
- [ ] 기본 CRUD 동작 테스트
- [ ] 커스텀 쿼리 메서드 테스트
- [ ] QueryDSL 동적 쿼리 테스트 (사용시)
- [ ] 관계(Relationship) 로딩 테스트

### 테스트 실행 명령어

```bash
# 전체 테스트
./gradlew test

# 도메인별 테스트
./gradlew test --tests "com.jk.amazon2.[domain].*"

# 특정 클래스 테스트
./gradlew test --tests "com.jk.amazon2.[domain].controller.[Domain]ControllerTest"

# 특정 메서드 테스트
./gradlew test --tests "com.jk.amazon2.[domain].service.[Domain]CommandServiceTest.testCreate"

# 테스트 결과 리포트 확인
# build/reports/tests/test/index.html
```

---

## 하네스 README 작성

각 도메인을 추가할 때 `harnesses/[domain]/README.md`를 작성합니다.

### 작성 항목

```markdown
# [Domain] 도메인

[domain]에 대한 개요 설명

## 엔티티 구조

Entity 정의와 필드 설명

## 서비스 계층

- **[Domain]CommandService**: Create, Update, Delete
- **[Domain]QueryService**: Read 담당

## API 엔드포인트

| 메서드 | 경로 | 설명 |
|-------|-----|------|
| GET | /api/v1/[domains] | 목록 조회 |
| GET | /api/v1/[domains]/{id} | 개별 조회 |
| POST | /api/v1/[domains] | 생성 |
| PUT | /api/v1/[domains]/{id} | 수정 |
| DELETE | /api/v1/[domains]/{id} | 삭제 |

## 예외 처리

[Domain]ErrorCode enum에 정의된 예외

| 에러 코드 | 설명 |
|----------|------|
| CD_001 | 조회할 [domain]이 없습니다 |
| CD_002 | [domain]이 이미 존재합니다 |

## 테스트

- `[Domain]ControllerTest`: 통합 테스트
- `[Domain]CommandServiceTest`: 비즈니스 로직 테스트
- `[Domain]RepositoryTest`: 데이터 접근 테스트
```

---

## 개발 체크리스트

### 새 도메인 추가 시

```
새 도메인 이름: [domain]

패키지 및 파일:
- [ ] mkdir -p src/main/java/com/jk/amazon2/[domain]/{controller,service,repository,entity,dto,exception}
- [ ] mkdir -p src/test/java/com/jk/amazon2/[domain]/{controller,service,repository}

개발 (권장 순서):
- [ ] Entity ([domain]/entity/[Domain].java) 작성
- [ ] Repository ([domain]/repository/[Domain]Repository.java) 작성
- [ ] Service - CommandService 작성
- [ ] Service - QueryService 작성
- [ ] DTO - Request/Response 작성
- [ ] Exception/ErrorCode 작성
- [ ] Controller 작성
- [ ] Test - ControllerTest 작성
- [ ] Test - CommandServiceTest 작성
- [ ] Test - RepositoryTest 작성 (필요시)

통합:
- [ ] GlobalExceptionHandler에 [Domain]Exception 핸들러 추가
- [ ] harnesses/[domain]/README.md 작성
- [ ] CLAUDE.md 업데이트 (필요시)

최종 확인:
- [ ] ./gradlew clean build 성공
- [ ] ./gradlew test 모두 통과
- [ ] http://localhost:8080/swagger-ui/index.html에서 API 확인
- [ ] git status 확인
- [ ] git commit (규칙 준수)
```

### 기존 도메인 기능 추가 시

```
기능: [기능 설명]
대상 도메인: [domain]

개발:
- [ ] Service에 새로운 메서드 추가
- [ ] Repository에 필요한 쿼리 추가
- [ ] DTO 수정/추가
- [ ] Controller에 엔드포인트 추가
- [ ] Exception/ErrorCode 추가 (필요시)
- [ ] 테스트 코드 추가

최종 확인:
- [ ] ./gradlew test --tests "com.jk.amazon2.[domain].*" 통과
- [ ] http://localhost:8080/swagger-ui/index.html에서 확인
- [ ] git commit
```

---

## 네이밍 규칙

### 패키지명
- lowercase, 영어
- 예: `member`, `category`, `posting`

### 클래스명
- PascalCase, 영어
- 예: `MemberController`, `MemberService`, `MemberRepository`

### 변수/함수명
- camelCase, 영어
- 예: `getMemberById()`, `validateEmail()`, `isActive`

### 상수명
- UPPER_SNAKE_CASE, 영어
- 예: `MAX_MEMBER_COUNT`, `DEFAULT_PAGE_SIZE`

### 주석
- 한국어
- 예: `// 회원을 ID로 조회`

### SQL 컬럼/테이블명
- snake_case, 영어
- 예: `user_id`, `created_at`, `is_deleted`

---

## 주요 의존성

| 라이브러리 | 버전 | 용도 |
|-----------|------|------|
| Spring Boot | 4.0.0 | 웹 프레임워크 |
| Hibernate (JPA) | 6.x | ORM |
| MySQL Connector | 8.x | JDBC 드라이버 |
| QueryDSL | 7.1 | 동적 쿼리 빌더 |
| Lombok | 1.18.x | 보일러플레이트 제거 |
| SpringDoc OpenAPI | 3.0.0 | API 문서화 (Swagger) |
| Testcontainers | 1.19.0 | 통합 테스트 |
| REST Assured | 5.3.1 | API 테스트 |

---

## 유용한 명령어

### 빌드 및 테스트

```bash
# 전체 빌드
./gradlew clean build

# 빌드 (테스트 제외)
./gradlew clean build -x test

# 도메인별 테스트
./gradlew test --tests "com.jk.amazon2.[domain].*"

# 특정 테스트 클래스
./gradlew test --tests "com.jk.amazon2.[domain].controller.[Domain]ControllerTest"

# 테스트 결과 리포트
# build/reports/tests/test/index.html (브라우저에서 열기)
```

### 로컬 서버 실행

```bash
# 로컬 프로필로 실행
./gradlew bootRun --args='--spring.profiles.active=local'

# API 문서 확인
# http://localhost:8080/swagger-ui/index.html

# API Docs JSON
# http://localhost:8080/api-docs
```

### 구조 확인

```bash
# 도메인 구조 트리 보기
tree src/main/java/com/jk/amazon2 -L 3 -d

# 전체 소스 파일 개수
find src/main/java/com/jk/amazon2 -name "*.java" | wc -l
```

### Git 명령어

```bash
# 현재 상태 확인
git status

# 변경사항 확인
git diff

# 스테이징
git add [files]

# 커밋
git commit -m "[Type]: [description]"

# 커밋 로그 (최근 10개)
git log --oneline -10
```

---

## 개발 흐름 (Step-by-Step)

### 1. 이슈 생성 (GitHub)

```markdown
## 개요
새 도메인을 추가합니다

## 목표
- [domain] 엔티티 및 API 구현
- CRUD 기능 제공
- 테스트 커버리지 100%

## 관련 문서
- [harnesses/README.md](../harnesses/README.md)
- [docs/CONTRIBUTING.md](../docs/CONTRIBUTING.md)
```

### 2. 브랜치 생성

```bash
git checkout -b feature/#이슈번호-domain-crud
```

### 3. 패키지 생성

```bash
mkdir -p src/main/java/com/jk/amazon2/[domain]/{controller,service,repository,entity,dto,exception}
mkdir -p src/test/java/com/jk/amazon2/[domain]/{controller,service,repository}
```

### 4. 코드 작성 (권장 순서)

1. Entity 작성
2. Repository 작성
3. Service 작성 (Command/Query)
4. DTO 작성
5. Exception 작성
6. Controller 작성
7. 테스트 작성

### 5. 테스트 실행

```bash
./gradlew test --tests "com.jk.amazon2.[domain].*"
```

### 6. 빌드 확인

```bash
./gradlew clean build
```

### 7. 문서 작성

- `harnesses/[domain]/README.md` 생성
- `CLAUDE.md` 업데이트 (필요시)

### 8. 커밋

```bash
git add .
git commit -m "Feat: [domain] CRUD API 구현

- add: [Domain]Controller REST 엔드포인트
- add: [Domain]CommandService 생성/수정/삭제
- add: [Domain]QueryService 조회
..."
```

### 9. Pull Request 생성

```bash
gh pr create --title "#이슈번호: [domain] CRUD API 구현" \
    --body "..."
```

### 10. 리뷰 및 머지

---

## 참고 자료

### 프로젝트 문서

- **전체 아키텍처**: [harnesses/README.md](../harnesses/README.md)
- **Member 가이드**: [harnesses/member/README.md](../harnesses/member/README.md)
- **Category 가이드**: [harnesses/category/README.md](../harnesses/category/README.md)
- **Posting 가이드**: [harnesses/posting/README.md](../harnesses/posting/README.md)
- **기여 규칙**: [docs/CONTRIBUTING.md](CONTRIBUTING.md)
- **개발 가이드**: [docs/DEVELOPMENT.md](DEVELOPMENT.md)

### API 문서

- **Swagger UI** (로컬): http://localhost:8080/swagger-ui/index.html
- **OpenAPI JSON**: http://localhost:8080/api-docs

### 외부 자료

- **Spring Boot 공식 문서**: https://spring.io/projects/spring-boot
- **Spring Data JPA**: https://spring.io/projects/spring-data-jpa
- **QueryDSL**: http://www.querydsl.com/
- **Testcontainers**: https://www.testcontainers.org/

---

## FAQ

### Q1: 새 도메인을 추가할 때 어디서부터 시작해야 할까요?

**A**: Entity부터 시작하세요. Entity를 정의하면 나머지 계층 (Repository, Service, DTO, Controller)이 자연스럽게 따라옵니다.

### Q2: CommandService와 QueryService로 분리하는 이유는?

**A**: CQRS (Command Query Responsibility Segregation) 패턴을 따릅니다. 이렇게 하면:
- 비즈니스 로직이 명확하게 분리됨
- 각 서비스의 책임이 단일화됨
- 테스트가 더 쉬워짐
- 향후 성능 최적화가 용이함

### Q3: 테스트는 필수인가요?

**A**: 네, 필수입니다. 모든 도메인은 Controller, Service, Repository 테스트를 작성해야 합니다.

### Q4: QueryDSL은 필수인가요?

**A**: 선택사항입니다. 간단한 쿼리는 JpaRepository의 메서드명 쿼리로 충분합니다. 복잡한 동적 쿼리가 필요할 때만 QueryDSL을 사용하세요.

### Q5: 기존 도메인에 기능을 추가할 때는?

**A**: Service에 메서드를 추가하고, Repository와 DTO를 필요에 따라 수정한 후, Controller에 엔드포인트를 추가하세요. 테스트도 함께 작성합니다.

### Q6: 예외는 어떻게 처리하나요?

**A**: 각 도메인에서 ErrorCode를 구현하고, GlobalExceptionHandler에 핸들러를 등록합니다. 자세한 내용은 위의 "Exception 작성" 섹션을 참조하세요.

---

**마지막 업데이트**: 2026년 6월
**문서 버전**: 1.0
