
# Member Harness

회원(Member) 도메인 개발 가이드입니다.

---

## 개요

회원 조회, 등록, 수정, 삭제 기능을 담당합니다.

**주요 기능**:
- 회원 조회 (개별, 목록)
- 회원 등록
- 회원 정보 수정
- 회원 소프트/영구 삭제
- 회원 검색

---

## 주요 클래스

### Controller
- `MemberController` - REST API 엔드포인트

### Service
- `MemberCommandService` - 생성, 수정, 삭제 로직
- `MemberQueryService` - 조회 로직

### Repository
- `MemberRepository` - JPA 레포지토리
- `MemberQueryRepository` - QueryDSL 동적 쿼리

### Entity
- `Member` - 회원 엔티티

### DTO
- `MemberRequest` - 요청 DTO
- `MemberResponse` - 응답 DTO

### Exception
- `MemberException` - 도메인 예외
- `MemberErrorCode` - 에러 코드

---

## 개발 가이드

### API 엔드포인트

```
GET    /api/members              # 회원 목록 조회
GET    /api/members/{id}         # 회원 조회
POST   /api/members              # 회원 등록
PUT    /api/members/{id}         # 회원 정보 수정
DELETE /api/members/{id}         # 회원 삭제 (소프트 삭제)
DELETE /api/members/{id}/hard    # 회원 영구 삭제
```

### 개발 순서

1. **Entity 설계**
   - `Member` 엔티티 속성 정의
   - `@Entity`, `@Table` 어노테이션 설정

2. **Repository 구현**
   - `MemberRepository` extends `JpaRepository<Member, Long>`
   - QueryDSL이 필요한 경우 `MemberQueryRepository` 구현

3. **Service 구현**
   - `MemberCommandService` - 생성, 수정, 삭제
   - `MemberQueryService` - 조회
   - 도메인 로직 집중

4. **Controller 구현**
   - REST API 엔드포인트
   - `@Operation`, `@ApiResponse` 어노테이션으로 문서화

5. **Exception 처리**
   - `MemberErrorCode` enum 정의
   - `MemberException` 발생

### DTO 규칙

```java
// Request
public record MemberCreateRequest(
    String name,
    String email
) {}

// Response
public record MemberResponse(
    Long id,
    String name,
    String email
) {}
```

---

## 테스트

### 통합 테스트 예시

```bash
./gradlew test --tests "com.jk.amazon2.member.*"
```

### 테스트 구조

```
src/test/java/com/jk/amazon2/member/
├── controller/    API 테스트
├── service/       비즈니스 로직 테스트
└── repository/    DB 접근 테스트
```

### Testcontainers 설정

```java
@SpringBootTest
@Testcontainers
class MemberControllerTest {
    
    @Container
    static MySQLContainer<?> mysql = 
        new MySQLContainer<>(DockerImageName.parse("mysql:8.0"));
    
    @Test
    void testCreateMember() {
        // 테스트
    }
}
```

---

## 주의사항

1. **소프트 삭제**: `deletedAt` 필드로 관리
2. **영구 삭제**: 실제 DB에서 제거 (취소 불가)
3. **검색**: QueryDSL로 동적 쿼리 구현
4. **예외**: `MemberException` 사용

---

## 커밋 메시지 예시

```
Feat: 회원 CRUD API 구현

- add: MemberController REST 엔드포인트
- add: MemberCommandService 비즈니스 로직
- add: MemberQueryService 조회 로직
- add: MemberRepository JPA 레포지토리
- docs: Swagger 어노테이션 추가
```

---

## 참고

- **요구사항**: [docs/Requirements.md](../../docs/Requirements.md)
- **ERD**: [docs/ERD.md](../../docs/ERD.md)
- **API**: http://localhost:8080/swagger-ui/index.html