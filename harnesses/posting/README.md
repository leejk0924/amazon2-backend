# Posting Harness

포스팅(Posting) 도메인 개발 가이드입니다.

---

## 개요

블로그 포스팅 조회 및 통계 기능을 담당합니다.

**주요 기능**:
- 포스팅 조회 (개별, 목록, 필터)
- 포스팅 검색 (제목, 내용)
- 포스팅 통계 (카테고리별, 기간별)
- 포스팅 상태 관리

---

## 주요 클래스

### Controller
- `PostingController` - REST API 엔드포인트

### Service
- `PostingQueryService` - 조회 및 검색 로직
- `PostingStatisticsService` - 통계 로직

### Repository
- `PostingRepository` - JPA 레포지토리
- `PostingQueryRepository` - QueryDSL 동적 쿼리

### Entity
- `Posting` - 포스팅 엔티티
- `PostingStatistics` - 통계 엔티티 (선택)

### DTO
- `PostingResponse` - 포스팅 응답
- `PostingSearchRequest` - 검색 요청
- `StatisticsResponse` - 통계 응답

### Exception
- `PostingException` - 도메인 예외
- `PostingErrorCode` - 에러 코드

---

## 개발 가이드

### API 엔드포인트

```
GET    /api/postings                      # 포스팅 목록
GET    /api/postings/{id}                 # 포스팅 조회
GET    /api/postings/search               # 포스팅 검색
GET    /api/postings/category/{categoryId} # 카테고리별 포스팅
GET    /api/postings/statistics           # 통계
GET    /api/postings/statistics/category  # 카테고리별 통계
```

### 개발 순서

1. **Entity 설계**
   - `Posting` (title, content, category, createdAt, updatedAt)
   - `PostingStatistics` (카테고리, 개수, 최근 업데이트)

2. **Repository 구현**
   - 기본 CRUD
   - QueryDSL: 검색, 필터링, 통계

3. **Service 구현**
   - `PostingQueryService` - 조회, 검색
   - `PostingStatisticsService` - 통계 계산

4. **Controller 구현**
   - REST 엔드포인트
   - Pagination 지원

5. **Exception 처리**
   - `POSTING_NOT_FOUND` - 포스팅 미존재

### QueryDSL 동적 쿼리 예시

```java
// 검색 + 필터 조합
public List<PostingResponse> searchPostings(
    String keyword, 
    Long categoryId, 
    LocalDate startDate,
    LocalDate endDate) {
    
    QPosting posting = QPosting.posting;
    
    return queryFactory
        .selectFrom(posting)
        .where(
            posting.title.contains(keyword),
            posting.categoryId.eq(categoryId),
            posting.createdAt.between(startDate, endDate)
        )
        .orderBy(posting.createdAt.desc())
        .fetch();
}
```

### 통계 로직

```java
// 카테고리별 포스팅 개수
public Map<String, Long> getStatisticsByCategory() {
    // GROUP BY category_id
}

// 기간별 통계
public Map<LocalDate, Long> getStatisticsByDateRange(
    LocalDate startDate, 
    LocalDate endDate) {
    // GROUP BY DATE
}
```

---

## 테스트

```bash
./gradlew test --tests "com.jk.amazon2.posting.*"
```

### 테스트 항목

1. **검색 기능**
   ```java
   @Test
   void testSearchByKeyword() {
       // 제목/내용으로 검색
   }
   ```

2. **필터링**
   ```java
   @Test
   void testFilterByCategory() {
       // 카테고리별 필터
   }
   
   @Test
   void testFilterByDateRange() {
       // 기간별 필터
   }
   ```

3. **통계**
   ```java
   @Test
   void testStatisticsByCategory() {
       // 카테고리별 통계
   }
   ```

---

## 성능 최적화

### 대량 데이터 처리

1. **Pagination**
   - `Pageable` 사용
   - 기본 크기: 20, 최대: 100

2. **인덱스**
   - `title`, `categoryId`, `createdAt` 인덱스 필수

3. **캐싱**
   - 통계 데이터 캐시 (1시간)
   - 자주 조회되는 카테고리별 포스팅

### 쿼리 최적화

```java
// N+1 문제 해결
@Query("SELECT p FROM Posting p JOIN FETCH p.category")
List<Posting> findAllWithCategory();
```

---

## 주의사항

1. **검색 성능**: 전체 텍스트 검색 고려
2. **통계 갱신**: 배치 작업으로 주기적 갱신
3. **Soft Delete**: 삭제된 포스팅은 조회 제외

---

## 커밋 메시지 예시

```
Feat: 포스팅 조회 및 검색 API 구현

- add: PostingController 조회/검색 엔드포인트
- add: PostingQueryService 동적 쿼리 로직
- add: QueryDSL 검색/필터 구현
- add: PostingStatisticsService 통계 로직
- test: 검색, 필터, 통계 통합 테스트
```

---

## 참고

- **요구사항**: [docs/Requirements.md](../../docs/Requirements.md)
- **ERD**: [docs/ERD.md](../../docs/ERD.md)
- **QueryDSL**: [QueryDSL 가이드](../../harnesses/README.md)