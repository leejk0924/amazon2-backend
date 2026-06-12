# Category Harness

카테고리(Category) 도메인 개발 가이드입니다.

---

## 개요

포스팅 카테고리 관리 기능을 담당합니다.

**주요 기능**:
- 카테고리 조회 (개별, 목록, 코드별)
- 카테고리 등록
- 카테고리 정보 수정
- 카테고리 삭제

---

## 주요 클래스

### Controller
- `CategoryController` - REST API 엔드포인트

### Service
- `CategoryCommandService` - 생성, 수정, 삭제
- `CategoryQueryService` - 조회

### Repository
- `CategoryRepository` - JPA 레포지토리
- `CategoryQueryRepository` - QueryDSL 동적 쿼리

### Entity
- `Category` - 카테고리 엔티티

### DTO
- `CategoryRequest` - 요청 DTO
- `CategoryResponse` - 응답 DTO

### Exception
- `CategoryException` - 도메인 예외
- `CategoryErrorCode` - 에러 코드

---

## 개발 가이드

### API 엔드포인트

```
GET    /api/categories              # 카테고리 목록
GET    /api/categories/{id}         # 카테고리 조회 (ID)
GET    /api/categories/code/{code}  # 카테고리 조회 (Code)
POST   /api/categories              # 카테고리 등록
PUT    /api/categories/{id}         # 카테고리 수정
DELETE /api/categories/{id}         # 카테고리 삭제
```

### 개발 순서

1. **Entity 설계**
   - `Category` 엔티티 (id, code, name, description)
   - 유니크 제약: `code` 필드

2. **Repository 구현**
   - `findByCode(String code)` - 코드 기반 조회
   - `existsByCode(String code)` - 코드 중복 체크

3. **Service 구현**
   - `CategoryCommandService` - CUD 로직
   - `CategoryQueryService` - R 로직

4. **Controller 구현**
   - REST 엔드포인트
   - 경로: `/api/categories`

5. **Exception 처리**
   - `CATEGORY_NOT_FOUND` - 카테고리 미존재
   - `CATEGORY_CODE_DUPLICATE` - 코드 중복
   - `CATEGORY_IN_USE` - 사용 중인 카테고리 삭제 불가

### 코드 규칙

```java
// 코드는 영문 대문자 스네이크 케이스
// 예: TRAVEL, FOOD, TECH

public record CategoryResponse(
    Long id,
    String code,      // "TRAVEL"
    String name,      // "여행"
    String description
) {}
```

---

## 테스트

```bash
./gradlew test --tests "com.jk.amazon2.category.*"
```

### 테스트 항목

1. **코드 중복 체크**
   ```java
   @Test
   void testCodeDuplicate() {
       // CATEGORY_CODE_DUPLICATE 예외 발생
   }
   ```

2. **카테고리 코드별 조회**
   ```java
   @Test
   void testFindByCode() {
       // 코드로 정확히 조회
   }
   ```

3. **카테고리 삭제 제약**
   ```java
   @Test
   void testCannotDeleteInUseCategory() {
       // 포스팅이 있으면 삭제 불가
   }
   ```

---

## 주의사항

1. **코드 필드**: UNIQUE 제약, 변경 불가
2. **삭제 제약**: 포스팅이 있으면 삭제 불가
3. **캐싱**: 자주 조회되므로 캐시 고려

---

## 커밋 메시지 예시

```
Feat: 카테고리 관리 API 구현

- add: CategoryController REST 엔드포인트
- add: CategoryCommandService 생성, 수정, 삭제
- add: CategoryQueryService 조회 로직
- add: 코드 필드 UNIQUE 제약
- test: 카테고리 통합 테스트
```

---

## 참고

- **요구사항**: [docs/Requirements.md](../../docs/Requirements.md)
- **ERD**: [docs/ERD.md](../../docs/ERD.md)