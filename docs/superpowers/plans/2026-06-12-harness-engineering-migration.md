# 하네스 엔지니어링 적용 마이그레이션 계획

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 현재 평면적 레이어 구조(controller/, service/, repository/ 평면)를 도메인 기반 모듈화 구조(member/, category/, posting/)로 마이그레이션하여 확장성 있는 하네스 엔지니어링 구조 확립

**Architecture:** 
3개 도메인(Member, Category, Posting)을 각각의 독립적인 모듈로 분리. 각 모듈은 자체 controller, service, repository, entity, dto, exception을 가짐. 공통 코드(common, config, exception)는 모든 모듈이 공유. 한 도메인씩 단계적으로 마이그레이션하여 테스트 검증 후 다음 도메인으로 진행.

**Tech Stack:** Java 21, Spring Boot 4.0.0, Gradle, JPA, QueryDSL, MySQL, Testcontainers

---

## Phase 1: Member 도메인 마이그레이션

### Task 1: Member 패키지 구조 생성

**Files:**
- Create: `src/main/java/com/jk/amazon2/member/` (디렉토리)
- Create: `src/main/java/com/jk/amazon2/member/controller/` (디렉토리)
- Create: `src/main/java/com/jk/amazon2/member/service/` (디렉토리)
- Create: `src/main/java/com/jk/amazon2/member/repository/` (디렉토리)
- Create: `src/main/java/com/jk/amazon2/member/entity/` (디렉토리)
- Create: `src/main/java/com/jk/amazon2/member/dto/` (디렉토리)
- Create: `src/main/java/com/jk/amazon2/member/exception/` (디렉토리)

- [ ] **Step 1: 패키지 디렉토리 구조 생성**

```bash
mkdir -p src/main/java/com/jk/amazon2/member/{controller,service,repository,entity,dto,exception}
mkdir -p src/test/java/com/jk/amazon2/member/{controller,service,repository}
```

- [ ] **Step 2: 커밋**

```bash
git add -A
git commit -m "Refactor: member 패키지 구조 생성"
```

---

### Task 2: Member 엔티티 및 DTO 이동

**Files:**
- Move: `src/main/java/com/jk/amazon2/entity/Member.java` → `src/main/java/com/jk/amazon2/member/entity/Member.java`
- Move: `src/main/java/com/jk/amazon2/controller/dto/MemberCreateRequest.java` → `src/main/java/com/jk/amazon2/member/dto/MemberCreateRequest.java`
- Move: `src/main/java/com/jk/amazon2/controller/dto/MemberUpdateRequest.java` → `src/main/java/com/jk/amazon2/member/dto/MemberUpdateRequest.java`
- Move: `src/main/java/com/jk/amazon2/controller/dto/MemberResponse.java` → `src/main/java/com/jk/amazon2/member/dto/MemberResponse.java`

- [ ] **Step 1: 파일 이동**

```bash
# 엔티티 이동
mv src/main/java/com/jk/amazon2/entity/Member.java \
   src/main/java/com/jk/amazon2/member/entity/Member.java

# DTO 이동
mv src/main/java/com/jk/amazon2/controller/dto/MemberCreateRequest.java \
   src/main/java/com/jk/amazon2/member/dto/MemberCreateRequest.java
mv src/main/java/com/jk/amazon2/controller/dto/MemberUpdateRequest.java \
   src/main/java/com/jk/amazon2/member/dto/MemberUpdateRequest.java
mv src/main/java/com/jk/amazon2/controller/dto/MemberResponse.java \
   src/main/java/com/jk/amazon2/member/dto/MemberResponse.java
```

- [ ] **Step 2: 패키지명 수정 (Member.java)**

`Member.java` 파일 상단 수정:
```java
package com.jk.amazon2.member.entity;

import com.jk.amazon2.common.constant.DeleteStatus;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

// ... 나머지는 동일
```

- [ ] **Step 3: 패키지명 수정 (DTO들)**

`MemberCreateRequest.java`:
```java
package com.jk.amazon2.member.dto;

public record MemberCreateRequest(
    String name,
    String email
) {}
```

`MemberUpdateRequest.java`:
```java
package com.jk.amazon2.member.dto;

public record MemberUpdateRequest(
    String name,
    String email
) {}
```

`MemberResponse.java`:
```java
package com.jk.amazon2.member.dto;

import com.jk.amazon2.common.constant.DeleteStatus;

public record MemberResponse(
    Long id,
    String name,
    String email,
    DeleteStatus deleteStatus
) {}
```

- [ ] **Step 4: 임포트 확인 및 수정**

IDE의 "Optimize imports" 기능 사용하거나 수동으로 확인

- [ ] **Step 5: 커밋**

```bash
git add src/main/java/com/jk/amazon2/member/
git commit -m "Refactor: member 엔티티 및 DTO를 member 패키지로 이동"
```

---

### Task 3: Member Repository 이동

**Files:**
- Move: `src/main/java/com/jk/amazon2/repository/MemberRepository.java` → `src/main/java/com/jk/amazon2/member/repository/MemberRepository.java`
- Move: `src/main/java/com/jk/amazon2/repository/MemberQueryRepository.java` → `src/main/java/com/jk/amazon2/member/repository/MemberQueryRepository.java`

- [ ] **Step 1: 파일 이동**

```bash
mv src/main/java/com/jk/amazon2/repository/MemberRepository.java \
   src/main/java/com/jk/amazon2/member/repository/MemberRepository.java
mv src/main/java/com/jk/amazon2/repository/MemberQueryRepository.java \
   src/main/java/com/jk/amazon2/member/repository/MemberQueryRepository.java
```

- [ ] **Step 2: 패키지명 수정**

`MemberRepository.java`:
```java
package com.jk.amazon2.member.repository;

import com.jk.amazon2.member.entity.Member;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MemberRepository extends JpaRepository<Member, Long> {
    // ...
}
```

`MemberQueryRepository.java`:
```java
package com.jk.amazon2.member.repository;

import com.jk.amazon2.member.entity.Member;
import com.jk.amazon2.member.dto.MemberResponse;
// ... 다른 임포트들

public class MemberQueryRepository {
    // ...
}
```

- [ ] **Step 3: 임포트 확인 및 수정**

특히 `MemberQueryRepository`에서:
- `QMember` import 확인
- `MemberResponse` import 경로 확인

- [ ] **Step 4: 테스트 가동 확인**

```bash
./gradlew test --tests "com.jk.amazon2.member.*" -v
```

예상: 아직 테스트가 없을 수 있으니 무시해도 됨

- [ ] **Step 5: 커밋**

```bash
git add src/main/java/com/jk/amazon2/member/repository/
git commit -m "Refactor: member repository를 member 패키지로 이동"
```

---

### Task 4: Member Service 이동

**Files:**
- Move: `src/main/java/com/jk/amazon2/service/MemberCommandService.java` → `src/main/java/com/jk/amazon2/member/service/MemberCommandService.java`
- Move: `src/main/java/com/jk/amazon2/service/MemberQueryService.java` → `src/main/java/com/jk/amazon2/member/service/MemberQueryService.java`

- [ ] **Step 1: 파일 이동**

```bash
mv src/main/java/com/jk/amazon2/service/MemberCommandService.java \
   src/main/java/com/jk/amazon2/member/service/MemberCommandService.java
mv src/main/java/com/jk/amazon2/service/MemberQueryService.java \
   src/main/java/com/jk/amazon2/member/service/MemberQueryService.java
```

- [ ] **Step 2: 패키지명 수정**

`MemberCommandService.java`:
```java
package com.jk.amazon2.member.service;

import com.jk.amazon2.member.repository.MemberRepository;
import com.jk.amazon2.member.entity.Member;
import com.jk.amazon2.member.dto.MemberCreateRequest;
import com.jk.amazon2.member.dto.MemberUpdateRequest;
import com.jk.amazon2.member.exception.MemberException;
// ... 다른 임포트들

@Service
public class MemberCommandService {
    // ...
}
```

`MemberQueryService.java`:
```java
package com.jk.amazon2.member.service;

import com.jk.amazon2.member.repository.MemberQueryRepository;
import com.jk.amazon2.member.dto.MemberResponse;
// ... 다른 임포트들

@Service
public class MemberQueryService {
    // ...
}
```

- [ ] **Step 3: 의존성 주입 확인**

두 Service 모두 `MemberRepository`와 `MemberQueryRepository`를 주입받는지 확인

- [ ] **Step 4: 커밋**

```bash
git add src/main/java/com/jk/amazon2/member/service/
git commit -m "Refactor: member service를 member 패키지로 이동"
```

---

### Task 5: Member Exception 이동

**Files:**
- Move: `src/main/java/com/jk/amazon2/exception/MemberException.java` → `src/main/java/com/jk/amazon2/member/exception/MemberException.java`
- Move: `src/main/java/com/jk/amazon2/exception/MemberErrorCode.java` → `src/main/java/com/jk/amazon2/member/exception/MemberErrorCode.java`

- [ ] **Step 1: 파일 이동**

```bash
mv src/main/java/com/jk/amazon2/exception/MemberException.java \
   src/main/java/com/jk/amazon2/member/exception/MemberException.java
mv src/main/java/com/jk/amazon2/exception/MemberErrorCode.java \
   src/main/java/com/jk/amazon2/member/exception/MemberErrorCode.java
```

- [ ] **Step 2: 패키지명 수정**

`MemberException.java`:
```java
package com.jk.amazon2.member.exception;

import com.jk.amazon2.common.exception.ErrorCode;

public class MemberException extends RuntimeException {
    private final ErrorCode errorCode;
    
    public MemberException(MemberErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }
    
    public ErrorCode getErrorCode() {
        return errorCode;
    }
}
```

`MemberErrorCode.java`:
```java
package com.jk.amazon2.member.exception;

import com.jk.amazon2.common.exception.ErrorCode;
import org.springframework.http.HttpStatus;

public enum MemberErrorCode implements ErrorCode {
    MEMBER_NOT_FOUND("M001", "회원을 찾을 수 없습니다", HttpStatus.NOT_FOUND),
    MEMBER_EMAIL_DUPLICATE("M002", "이미 존재하는 이메일입니다", HttpStatus.CONFLICT),
    MEMBER_INVALID_EMAIL("M003", "유효하지 않은 이메일입니다", HttpStatus.BAD_REQUEST);
    
    private final String code;
    private final String message;
    private final HttpStatus httpStatus;
    
    MemberErrorCode(String code, String message, HttpStatus httpStatus) {
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

- [ ] **Step 3: 임포트 확인**

`MemberService`들에서 `MemberErrorCode` import 경로 확인:
```java
import com.jk.amazon2.member.exception.MemberException;
import com.jk.amazon2.member.exception.MemberErrorCode;
```

- [ ] **Step 4: 커밋**

```bash
git add src/main/java/com/jk/amazon2/member/exception/
git commit -m "Refactor: member exception을 member 패키지로 이동"
```

---

### Task 6: Member Controller 이동

**Files:**
- Move: `src/main/java/com/jk/amazon2/controller/MemberController.java` → `src/main/java/com/jk/amazon2/member/controller/MemberController.java`

- [ ] **Step 1: 파일 이동**

```bash
mv src/main/java/com/jk/amazon2/controller/MemberController.java \
   src/main/java/com/jk/amazon2/member/controller/MemberController.java
```

- [ ] **Step 2: 패키지명 수정**

`MemberController.java`:
```java
package com.jk.amazon2.member.controller;

import com.jk.amazon2.member.service.MemberCommandService;
import com.jk.amazon2.member.service.MemberQueryService;
import com.jk.amazon2.member.dto.MemberCreateRequest;
import com.jk.amazon2.member.dto.MemberUpdateRequest;
import com.jk.amazon2.member.dto.MemberResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/members")
public class MemberController {
    // ...
}
```

- [ ] **Step 3: 의존성 주입 확인**

`MemberCommandService`와 `MemberQueryService`가 올바르게 주입되는지 확인

- [ ] **Step 4: 통합 테스트 실행**

```bash
./gradlew bootRun --args='--spring.profiles.active=local'
```

로컬에서 서버 시작 후 API 정상 작동 확인 (curl 또는 Swagger)

```bash
# 예: 회원 조회
curl -X GET http://localhost:8080/api/members
```

또는 Swagger UI 접속:
```
http://localhost:8080/swagger-ui/index.html
```

- [ ] **Step 5: 빌드 및 테스트**

```bash
./gradlew clean build
```

예상: 모든 테스트 통과

- [ ] **Step 6: 커밋**

```bash
git add src/main/java/com/jk/amazon2/member/controller/
git commit -m "Refactor: member controller를 member 패키지로 이동"
```

---

### Task 7: Member 테스트 코드 이동

**Files:**
- Move: `src/test/java/com/jk/amazon2/controller/MemberControllerTest.java` → `src/test/java/com/jk/amazon2/member/controller/MemberControllerTest.java`
- Move: `src/test/java/com/jk/amazon2/service/MemberServiceTest.java` → `src/test/java/com/jk/amazon2/member/service/MemberServiceTest.java` (있으면)
- Move: `src/test/java/com/jk/amazon2/repository/MemberRepositoryTest.java` → `src/test/java/com/jk/amazon2/member/repository/MemberRepositoryTest.java` (있으면)

- [ ] **Step 1: 테스트 파일 이동**

```bash
# 있는 파일만 이동
mv src/test/java/com/jk/amazon2/controller/MemberControllerTest.java \
   src/test/java/com/jk/amazon2/member/controller/MemberControllerTest.java

# 다른 테스트 파일 있으면 유사하게 이동
```

- [ ] **Step 2: 테스트 파일 패키지명 수정**

`MemberControllerTest.java`:
```java
package com.jk.amazon2.member.controller;

import com.jk.amazon2.member.service.MemberCommandService;
import com.jk.amazon2.member.service.MemberQueryService;
import com.jk.amazon2.member.dto.MemberCreateRequest;
import com.jk.amazon2.member.dto.MemberResponse;
// ... 다른 임포트

@SpringBootTest
@Testcontainers
class MemberControllerTest {
    // ...
}
```

- [ ] **Step 3: Member 테스트 실행**

```bash
./gradlew test --tests "com.jk.amazon2.member.*" -v
```

예상: 모든 테스트 통과

- [ ] **Step 4: 커밋**

```bash
git add src/test/java/com/jk/amazon2/member/
git commit -m "Refactor: member 테스트를 member 패키지로 이동"
```

---

## Phase 2: Category 도메인 마이그레이션

> **Note:** Phase 1과 동일한 패턴으로 진행합니다. 간결함을 위해 요약합니다.

### Task 8: Category 패키지 구조 및 파일 이동

**Files:**
- Create: `src/main/java/com/jk/amazon2/category/` (및 하위 패키지)
- Move: Category 관련 모든 파일

- [ ] **Step 1: 패키지 디렉토리 생성**

```bash
mkdir -p src/main/java/com/jk/amazon2/category/{controller,service,repository,entity,dto,exception}
mkdir -p src/test/java/com/jk/amazon2/category/{controller,service,repository}
```

- [ ] **Step 2: 모든 Category 파일 이동 및 패키지명 수정**

```bash
# 엔티티
mv src/main/java/com/jk/amazon2/entity/Category.java \
   src/main/java/com/jk/amazon2/category/entity/Category.java

# DTO
mv src/main/java/com/jk/amazon2/controller/dto/Category*.java \
   src/main/java/com/jk/amazon2/category/dto/

# Repository
mv src/main/java/com/jk/amazon2/repository/Category*.java \
   src/main/java/com/jk/amazon2/category/repository/

# Service
mv src/main/java/com/jk/amazon2/service/Category*.java \
   src/main/java/com/jk/amazon2/category/service/

# Exception
mv src/main/java/com/jk/amazon2/exception/Category*.java \
   src/main/java/com/jk/amazon2/category/exception/

# Controller
mv src/main/java/com/jk/amazon2/controller/CategoryController.java \
   src/main/java/com/jk/amazon2/category/controller/CategoryController.java
```

- [ ] **Step 3: 각 파일의 패키지명 및 임포트 수정**

모든 파일에서:
- `package com.jk.amazon2.category.[layer];` 로 변경
- 임포트 경로를 `com.jk.amazon2.category.*`로 업데이트

특히 주의:
- `CategoryEntity`에서 `Member` 관계 import 확인 (member 패키지에서 import)
- `CategoryService`에서 `MemberRepository` 사용 시 경로 확인

- [ ] **Step 4: 테스트 코드 이동 및 패키지명 수정**

```bash
mv src/test/java/com/jk/amazon2/controller/CategoryControllerTest.java \
   src/test/java/com/jk/amazon2/category/controller/CategoryControllerTest.java
```

- [ ] **Step 5: 테스트 실행**

```bash
./gradlew test --tests "com.jk.amazon2.category.*" -v
```

- [ ] **Step 6: 빌드 및 API 확인**

```bash
./gradlew clean build
./gradlew bootRun --args='--spring.profiles.active=local'

# 테스트
curl -X GET http://localhost:8080/api/categories
```

- [ ] **Step 7: 커밋**

```bash
git add src/main/java/com/jk/amazon2/category/ \
        src/test/java/com/jk/amazon2/category/
git commit -m "Refactor: category를 도메인 기반 패키지 구조로 마이그레이션"
```

---

## Phase 3: Posting 도메인 마이그레이션

### Task 9: Posting 패키지 구조 및 파일 이동

**Files:**
- Create: `src/main/java/com/jk/amazon2/posting/` (및 하위 패키지)
- Move: Posting 관련 모든 파일

- [ ] **Step 1: 패키지 디렉토리 생성**

```bash
mkdir -p src/main/java/com/jk/amazon2/posting/{controller,service,repository,entity,dto,exception}
mkdir -p src/test/java/com/jk/amazon2/posting/{controller,service,repository}
```

- [ ] **Step 2: 모든 Posting 파일 이동**

```bash
# 엔티티
mv src/main/java/com/jk/amazon2/entity/Posting.java \
   src/main/java/com/jk/amazon2/posting/entity/Posting.java

# DTO
mv src/main/java/com/jk/amazon2/controller/dto/Posting*.java \
   src/main/java/com/jk/amazon2/posting/dto/

# Repository
mv src/main/java/com/jk/amazon2/repository/Posting*.java \
   src/main/java/com/jk/amazon2/posting/repository/

# Service
mv src/main/java/com/jk/amazon2/service/Posting*.java \
   src/main/java/com/jk/amazon2/posting/service/

# Exception
mv src/main/java/com/jk/amazon2/exception/Posting*.java \
   src/main/java/com/jk/amazon2/posting/exception/

# Controller
mv src/main/java/com/jk/amazon2/controller/PostingController.java \
   src/main/java/com/jk/amazon2/posting/controller/PostingController.java
```

- [ ] **Step 3: 패키지명 및 임포트 수정**

모든 파일에서:
- `package com.jk.amazon2.posting.[layer];`로 변경
- 의존 패키지 임포트 경로 수정:
  - `com.jk.amazon2.member.*` (Member 관계)
  - `com.jk.amazon2.category.*` (Category 관계)

특히:
- `PostingEntity`에서 `@ManyToOne Member`, `@ManyToOne Category` 관계 확인
- QueryDSL 관련 Q클래스 생성 확인

- [ ] **Step 4: 테스트 코드 이동**

```bash
mv src/test/java/com/jk/amazon2/controller/PostingControllerTest.java \
   src/test/java/com/jk/amazon2/posting/controller/PostingControllerTest.java
```

- [ ] **Step 5: 테스트 실행**

```bash
./gradlew test --tests "com.jk.amazon2.posting.*" -v
```

- [ ] **Step 6: 빌드 및 API 확인**

```bash
./gradlew clean build
./gradlew bootRun --args='--spring.profiles.active=local'

# 테스트
curl -X GET http://localhost:8080/api/postings
```

- [ ] **Step 7: 커밋**

```bash
git add src/main/java/com/jk/amazon2/posting/ \
        src/test/java/com/jk/amazon2/posting/
git commit -m "Refactor: posting을 도메인 기반 패키지 구조로 마이그레이션"
```

---

## Phase 4: 공통 코드 정리

### Task 10: 공통 Exception 정리

**Files:**
- Keep: `src/main/java/com/jk/amazon2/common/exception/` (공통 예외 처리)
- Delete: 이동된 도메인별 예외 제외한 레거시 exception 패키지

- [ ] **Step 1: common/exception 패키지 구조 확인**

현재 구조:
```
src/main/java/com/jk/amazon2/exception/
├── ErrorCode.java (인터페이스)
├── GlobalExceptionHandler.java
├── ErrorResponse.java
└── (도메인별 예외들은 이미 이동됨)
```

필요하면 `src/main/java/com/jk/amazon2/common/exception/`로 이동

- [ ] **Step 2: 글로벌 예외 핸들러 확인**

`GlobalExceptionHandler.java`가 모든 도메인의 `ErrorCode`를 처리하는지 확인:

```java
package com.jk.amazon2.common.exception;

import com.jk.amazon2.member.exception.MemberException;
import com.jk.amazon2.category.exception.CategoryException;
import com.jk.amazon2.posting.exception.PostingException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {
    
    @ExceptionHandler(MemberException.class)
    public ResponseEntity<ErrorResponse> handleMemberException(MemberException e) {
        return ResponseEntity.status(e.getErrorCode().getHttpStatus())
            .body(new ErrorResponse(e.getErrorCode().getCode(), e.getMessage()));
    }
    
    @ExceptionHandler(CategoryException.class)
    public ResponseEntity<ErrorResponse> handleCategoryException(CategoryException e) {
        return ResponseEntity.status(e.getErrorCode().getHttpStatus())
            .body(new ErrorResponse(e.getErrorCode().getCode(), e.getMessage()));
    }
    
    @ExceptionHandler(PostingException.class)
    public ResponseEntity<ErrorResponse> handlePostingException(PostingException e) {
        return ResponseEntity.status(e.getErrorCode().getHttpStatus())
            .body(new ErrorResponse(e.getErrorCode().getCode(), e.getMessage()));
    }
}
```

- [ ] **Step 3: 레거시 exception 디렉토리 제거**

```bash
# 이동 완료 확인 후
rm -rf src/main/java/com/jk/amazon2/exception/
```

- [ ] **Step 4: 테스트 실행**

```bash
./gradlew test -v
```

예상: 모든 테스트 통과

- [ ] **Step 5: 커밋**

```bash
git add -A
git commit -m "Refactor: 레거시 exception 디렉토리 제거 및 글로벌 예외 핸들러 통합"
```

---

### Task 11: 레거시 controller/dto, service, repository 디렉토리 정리

**Files:**
- Delete: `src/main/java/com/jk/amazon2/controller/` (spec 제외)
- Delete: `src/main/java/com/jk/amazon2/service/`
- Delete: `src/main/java/com/jk/amazon2/repository/`
- Delete: `src/main/java/com/jk/amazon2/entity/`
- Delete: `src/test/java/com/jk/amazon2/controller/`
- Delete: `src/test/java/com/jk/amazon2/service/`
- Delete: `src/test/java/com/jk/amazon2/repository/`

- [ ] **Step 1: 레거시 디렉토리 확인**

```bash
find src/main/java/com/jk/amazon2 -maxdepth 1 -type d | grep -E "(controller|service|repository|entity|exception)"
```

이동되지 않은 파일이 있는지 확인

- [ ] **Step 2: 특수한 파일 확인**

`controller/spec/` 또는 `controller/dto/` 내 공용 DTO가 있는지 확인

있으면:
```bash
mkdir -p src/main/java/com/jk/amazon2/common/dto
mv src/main/java/com/jk/amazon2/controller/spec/* \
   src/main/java/com/jk/amazon2/common/dto/
```

- [ ] **Step 3: 레거시 디렉토리 삭제**

```bash
rm -rf src/main/java/com/jk/amazon2/controller
rm -rf src/main/java/com/jk/amazon2/service
rm -rf src/main/java/com/jk/amazon2/repository
rm -rf src/main/java/com/jk/amazon2/entity
rm -rf src/test/java/com/jk/amazon2/controller
rm -rf src/test/java/com/jk/amazon2/service
rm -rf src/test/java/com/jk/amazon2/repository
```

- [ ] **Step 4: 최종 구조 확인**

```bash
find src/main/java/com/jk/amazon2 -maxdepth 1 -type d | sort
```

예상 결과:
```
src/main/java/com/jk/amazon2
├── member/
├── category/
├── posting/
├── common/
└── config/
```

- [ ] **Step 5: 빌드 및 테스트**

```bash
./gradlew clean build
```

- [ ] **Step 6: 서버 시작 확인**

```bash
./gradlew bootRun --args='--spring.profiles.active=local'

# 모든 API 정상 작동 확인
curl -X GET http://localhost:8080/api/members
curl -X GET http://localhost:8080/api/categories
curl -X GET http://localhost:8080/api/postings

# Swagger UI 확인
# http://localhost:8080/swagger-ui/index.html
```

- [ ] **Step 7: 커밋**

```bash
git add -A
git commit -m "Refactor: 레거시 레이어별 디렉토리 제거 (controller, service, repository, entity)"
```

---

### Task 12: 최종 검증 및 문서화

**Files:**
- Verify: 전체 프로젝트 구조
- Update: `CLAUDE.md`와 `harnesses/README.md`에 새 구조 반영

- [ ] **Step 1: 전체 구조 확인**

```bash
tree src/main/java/com/jk/amazon2 -L 3 -d
```

예상 구조:
```
src/main/java/com/jk/amazon2
├── member/
│   ├── controller/
│   ├── service/
│   ├── repository/
│   ├── entity/
│   ├── dto/
│   └── exception/
├── category/
│   ├── controller/
│   ├── service/
│   ├── repository/
│   ├── entity/
│   ├── dto/
│   └── exception/
├── posting/
│   ├── controller/
│   ├── service/
│   ├── repository/
│   ├── entity/
│   ├── dto/
│   └── exception/
├── common/
│   ├── exception/
│   ├── constant/
│   ├── dto/
│   └── utils/
└── config/
```

- [ ] **Step 2: 전체 테스트 실행**

```bash
./gradlew test -v
```

예상: 모든 테스트 통과

- [ ] **Step 3: 전체 빌드**

```bash
./gradlew clean build
```

- [ ] **Step 4: 통합 테스트 (수동)**

```bash
./gradlew bootRun --args='--spring.profiles.active=local'
```

다음 API 호출 및 정상 응답 확인:
- `GET /api/members` - 200
- `POST /api/members` - 201 (요청 본문 필요)
- `GET /api/categories` - 200
- `GET /api/postings` - 200
- `GET /swagger-ui/index.html` - 200

- [ ] **Step 5: CLAUDE.md 업데이트**

기존 패키지 구조 섹션을 새 구조로 업데이트:

```markdown
## 패키지 구조

```
src/main/java/com/jk/amazon2/
├── member/              # 회원 모듈
│   ├── controller/      REST API
│   ├── service/         비즈니스 로직
│   ├── repository/      데이터 접근
│   ├── entity/          JPA 엔티티
│   ├── dto/             DTO
│   └── exception/       도메인 예외
├── category/            # 카테고리 모듈
├── posting/             # 포스팅 모듈
├── common/              공통 (exception, dto, utils, constant)
└── config/              Spring 설정
```

모든 새로운 기능은 도메인 폴더 아래에서 개발합니다.
```

- [ ] **Step 6: harnesses/README.md 확인**

현재 harnesses 구조가 실제 코드 구조와 일치하는지 확인. 필요시 업데이트.

- [ ] **Step 7: 최종 커밋**

```bash
git add CLAUDE.md docs/CLAUDE.md harnesses/
git commit -m "Docs: 하네스 엔지니어링 마이그레이션 완료 문서 업데이트"
```

---

## Phase 5: 향후 개발 가이드

### Task 13: 하네스 엔지니어링 개발 체크리스트 작성

**Files:**
- Create: `docs/HARNESS_DEVELOPMENT.md`

- [ ] **Step 1: 개발 체크리스트 파일 생성**

```markdown
# 하네스 엔지니어링 개발 가이드

새로운 도메인 추가나 기능 개발 시 다음을 따릅니다.

## 새 도메인 추가 체크리스트

### 1. 패키지 구조 생성
```bash
mkdir -p src/main/java/com/jk/amazon2/[domain]/{controller,service,repository,entity,dto,exception}
mkdir -p src/test/java/com/jk/amazon2/[domain]/{controller,service,repository}
```

### 2. 개발 순서
1. Entity 작성 (`entity/[Domain].java`)
2. Repository 작성 (`repository/[Domain]Repository.java`)
3. Service 작성 (`service/[Domain]CommandService.java`, `service/[Domain]QueryService.java`)
4. DTO 작성 (`dto/[Domain]Request.java`, `dto/[Domain]Response.java`)
5. Exception 작성 (`exception/[Domain]Exception.java`, `exception/[Domain]ErrorCode.java`)
6. Controller 작성 (`controller/[Domain]Controller.java`)
7. 테스트 작성 (`src/test/java/com/jk/amazon2/[domain]/`)

### 3. 예외 처리 등록
`GlobalExceptionHandler`에 새 도메인 예외 핸들러 추가:

```java
@ExceptionHandler([Domain]Exception.class)
public ResponseEntity<ErrorResponse> handle[Domain]Exception([Domain]Exception e) {
    return ResponseEntity.status(e.getErrorCode().getHttpStatus())
        .body(new ErrorResponse(e.getErrorCode().getCode(), e.getMessage()));
}
```

### 4. 커밋 메시지 규칙
```
Feat: [domain] CRUD API 구현

- add: [Domain]Controller REST 엔드포인트
- add: [Domain]CommandService 생성/수정/삭제
- add: [Domain]QueryService 조회
- add: [Domain]Repository JPA 레포지토리
- add: [Domain]Entity 엔티티
- test: [Domain] 통합 테스트
```

## 패키지별 책임

### member/
- 회원 조회, 등록, 수정, 삭제
- 회원 소프트/영구 삭제

### category/
- 카테고리 CRUD
- 카테고리 검증

### posting/
- 포스팅 조회, 검색
- 포스팅 통계

### common/
- 공통 예외 처리 (ErrorCode, GlobalExceptionHandler, ErrorResponse)
- 공통 DTO, 상수, 유틸

## 테스트 작성
각 도메인별로 다음 테스트 최소:
- Controller 통합 테스트
- Service 단위 테스트
- Repository 데이터 접근 테스트

```

- [ ] **Step 2: 파일 저장**

```bash
cat > docs/HARNESS_DEVELOPMENT.md << 'EOF'
# 하네스 엔지니어링 개발 가이드

[위의 내용 입력]
EOF
```

- [ ] **Step 3: 커밋**

```bash
git add docs/HARNESS_DEVELOPMENT.md
git commit -m "Docs: 하네스 엔지니어링 개발 가이드 작성"
```

---

## 최종 검증

### Task 14: 마이그레이션 완료 검증

- [ ] **Step 1: 전체 빌드 성공**

```bash
./gradlew clean build
```

예상: ✅ Build successful

- [ ] **Step 2: 모든 테스트 통과**

```bash
./gradlew test
```

예상: ✅ All tests passed

- [ ] **Step 3: 서버 정상 시작**

```bash
./gradlew bootRun --args='--spring.profiles.active=local'
```

예상: ✅ Application started successfully

- [ ] **Step 4: 모든 API 정상 작동**

```bash
curl http://localhost:8080/api/members
curl http://localhost:8080/api/categories
curl http://localhost:8080/api/postings
```

예상: 각각 200 OK

- [ ] **Step 5: Swagger UI 정상 표시**

```
http://localhost:8080/swagger-ui/index.html
```

예상: 모든 엔드포인트 표시

- [ ] **Step 6: 최종 커밋**

```bash
git log --oneline | head -20
# 마이그레이션 커밋들이 보이는지 확인

git commit --allow-empty -m "Feat: 하네스 엔지니어링 마이그레이션 완료"
```

---

## 마이그레이션 체크리스트

- [ ] Phase 1: Member 마이그레이션 완료
- [ ] Phase 2: Category 마이그레이션 완료
- [ ] Phase 3: Posting 마이그레이션 완료
- [ ] Phase 4: 공통 코드 정리 완료
- [ ] Phase 5: 문서화 및 가이드 작성 완료
- [ ] 전체 빌드 성공
- [ ] 모든 테스트 통과
- [ ] 서버 정상 시작
- [ ] 모든 API 정상 작동

---

## 예상 결과

마이그레이션 완료 후:

```
src/main/java/com/jk/amazon2/
├── member/                    # 독립적인 회원 모듈
│   ├── controller/
│   ├── service/
│   ├── repository/
│   ├── entity/
│   ├── dto/
│   └── exception/
├── category/                  # 독립적인 카테고리 모듈
├── posting/                   # 독립적인 포스팅 모듈
├── common/                    # 공통 코드
│   ├── exception/
│   ├── dto/
│   └── constant/
└── config/                    # Spring 설정

각 도메인은 완전히 독립적이며 확장 가능한 구조입니다.
```