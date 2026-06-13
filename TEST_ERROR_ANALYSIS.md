# 테스트 에러 분석 보고서

**작성일**: 2026-06-13  
**프로젝트**: Amazon2 (Spring Boot 백엔드)  
**테스트 환경**: JUnit5 + Testcontainers + MySQL 8.0

---

## 1. 에러 요약

### 에러 메시지
```
org.springframework.jdbc.UncategorizedSQLException: PreparedStatementCallback; 
uncategorized SQLException for SQL [
    INSERT INTO blog_category (code, name, description, created_at, created_by) 
    VALUES(?, ?, ?, NOW(), "system")
]
SQL state [HY000]; error code [1364]; Field 'deleted' doesn't have a default value
```

### 근본 원인
테스트에서 SQL로 `blog_category` 테이블에 데이터를 삽입할 때, **필수 컬럼인 `deleted`를 포함하지 않았음**.

Hibernate의 `ddl-auto: create-drop` 설정으로 생성된 스키마에서 `deleted` 컬럼은:
- NOT NULL 제약 조건
- DEFAULT 값 없음

따라서 INSERT 시에 명시적으로 값을 제공해야 함.

---

## 2. 문제점 분석

### 2.1 엔티티 정의 vs 테스트 SQL

**Category 엔티티** (src/main/java/com/jk/amazon2/category/entity/Category.java)
```java
@Entity
@Table(name = "blog_category")
public class Category extends BaseCreation implements Persistable<String> {
    @Id
    private String code;
    private String name;
    private String description;
    private boolean deleted = Boolean.FALSE;  // ← NOT NULL, DEFAULT FALSE
    // BaseCreation에서:
    // - createdAt: NOT NULL, @CreatedDate
    // - createdBy: NOT NULL, @CreatedBy
}
```

**Hibernate이 생성한 스키마**
```sql
CREATE TABLE blog_category (
    code VARCHAR(10) PRIMARY KEY,
    name VARCHAR(50) NOT NULL UNIQUE,
    description VARCHAR(50),
    deleted TINYINT(1) NOT NULL,              -- ← DEFAULT 값 없음!
    created_at DATETIME NOT NULL,
    created_by VARCHAR(50) NOT NULL
);
```

**테스트 SQL (잘못된 버전)**
```sql
INSERT INTO blog_category (code, name, description, created_at, created_by) 
VALUES(?, ?, ?, NOW(), "system")
-- ↑ deleted 컬럼이 빠짐!
```

### 2.2 발생 원인

1. **스키마 생성 방식의 불일치**
   - 프로덕션: Flyway 마이그레이션으로 명시적 스키마 정의 (DEFAULT 값 포함)
   - 테스트: Hibernate `ddl-auto: create-drop`으로 자동 생성 (엔티티에 DEFAULT가 없으면 생성 안 함)

2. **테스트 코드에서 직접 SQL 사용**
   - JPA Repository가 아닌 JdbcTemplate으로 직접 SQL 실행
   - 엔티티의 필드 정의를 반영하지 않음

3. **필수 컬럼 누락**
   - `deleted`: 엔티티에서 `boolean deleted = Boolean.FALSE`로 초기화되지만, 테스트 SQL에 포함 안 됨
   - `created_at`, `created_by`: 추가했지만 `deleted` 추가 안 함

---

## 3. 영향받는 테스트

### CategoryIntegrationTest
- `createCategory_Integration_fail_DuplicateCode()` - **FAILED**
  - 라인 85에서 INSERT 실패
  
### CategoryRepositoryTest (추정)
- `Category` 데이터 삽입하는 모든 테스트가 실패할 가능성

### MemberRepositoryTest
- 라인 44: `blog_category` 삽입 후 `member` 삽입하는 배치 업데이트 실패
- `blog_category` 삽입이 실패하면 외래키로 인해 `member` 삽입도 실패

---

## 4. 해결 방안

### 방안 1: 테스트 SQL에 모든 필수 컬럼 포함 (권장)

**현재 코드 (잘못됨)**
```java
String insertSql = "INSERT INTO blog_category (code, name, description, created_at, created_by) VALUES (?, ?, ?, NOW(), 'system')";
jdbcTemplate.update(insertSql, code, name, description);
```

**수정 코드**
```java
String insertSql = "INSERT INTO blog_category (code, name, description, deleted, created_at, created_by) VALUES (?, ?, ?, ?, NOW(), 'system')";
jdbcTemplate.update(insertSql, code, name, description, false);
```

### 방안 2: Hibernate 스키마에 DEFAULT 값 추가

**src/main/java/com/jk/amazon2/category/entity/Category.java 수정**
```java
@Column(name = "deleted", columnDefinition = "TINYINT(1) NOT NULL DEFAULT 0")
private boolean deleted = Boolean.FALSE;
```

**장점**:
- 엔티티 정의와 일치
- 모든 테스트에서 자동으로 적용

### 방안 3: JPA Repository 사용으로 변경

**현재 (JdbcTemplate)**
```java
String insertSql = "INSERT INTO blog_category (code, name, description, created_at, created_by) VALUES (?, ?, ?, NOW(), 'system')";
jdbcTemplate.update(insertSql, code, name, description);
```

**수정 (JPA Repository)**
```java
Category category = Category.of(code, name, description);
categoryRepository.save(category);
```

**장점**:
- 엔티티 필드 자동 처리
- 타입 안정성
- 유지보수 용이

---

## 5. 권장 조치 순서

### 단기 (즉시 적용)
1. **방안 1 적용**: 테스트 SQL에 `deleted` 컬럼 추가
   - 파일: `src/test/java/com/jk/amazon2/category/integration/CategoryIntegrationTest.java`
   - 파일: `src/test/java/com/jk/amazon2/member/repository/MemberRepositoryTest.java`

### 중기 (다음 리팩토링)
2. **방안 2 또는 3 검토**: 
   - 방안 2: 모든 엔티티의 `@Column` 정의 검토 및 개선
   - 방안 3: 테스트 코드에서 JPA Repository 사용으로 변경

### 장기 (아키텍처 개선)
3. **테스트 데이터 관리 표준화**:
   - Test Fixture 라이브러리 도입 (Jqwik, Fixture Monkey 등)
   - 테스트 데이터 빌더 패턴 적용
   - @DataSet 어노테이션 기반 관리

---

## 6. 재현 방법

```bash
cd amazon2-backend
./gradlew test --tests "CategoryIntegrationTest.createCategory_Integration_fail_DuplicateCode"
```

**예상 결과**:
- `BUILD FAILED`
- `1 test completed, 1 failed`
- 에러: `Field 'deleted' doesn't have a default value`

---

## 7. 체크리스트

- [ ] 테스트 SQL에 `deleted` 컬럼 추가 (모든 blog_category INSERT)
- [ ] 테스트 SQL에 `deleted` 컬럼 추가 (모든 member INSERT, 외래키 참조)
- [ ] 테스트 재실행 확인
- [ ] 다른 엔티티(posting, batch_execution 등)의 필수 컬럼 검토
- [ ] 장기 개선 계획 수립 (JPA Repository 사용 또는 Fixture 도입)

---

## 8. 참고 자료

### 관련 파일
- 엔티티: `src/main/java/com/jk/amazon2/category/entity/Category.java`
- 테스트: `src/test/java/com/jk/amazon2/category/integration/CategoryIntegrationTest.java`
- 테스트: `src/test/java/com/jk/amazon2/member/repository/MemberRepositoryTest.java`
- 설정: `src/test/resources/application-test.properties`

### 관련 에러 코드
- SQL State: HY000 (General error)
- MySQL Error Code: 1364 (Field doesn't have a default value)

---

**작성자**: Claude AI  
**최종 업데이트**: 2026-06-13 17:15 KST
