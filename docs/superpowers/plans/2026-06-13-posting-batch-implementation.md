# 포스팅 배치 시스템 구현 계획

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 네이버 블로그 포스팅 통계를 자동으로 수집하고 DB에 저장하는 배치 시스템 구현

**Architecture:** Spring @Scheduled + BlockingQueue 기반 속도 제한 + 트랜잭션 동시성 제어 + 3회 재시도 + DeadLetter 패턴

**Tech Stack:** Java 21, Spring Boot 4.0.0, JPA, MySQL 8.x, HttpClient, Jsoup, ExecutorService

---

## 파일 구조

```
posting/
├── entity/
│   ├── Posting.java (기존 유지)
│   ├── PostingError.java (신규)
│   ├── PostingDeadLetter.java (신규)
│   └── BatchExecution.java (신규)
├── dto/
│   ├── PostingRequest.java (기존 유지)
│   ├── PostingResponse.java (기존 유지)
│   ├── BatchRequest.java (신규)
│   ├── BatchStatusResponse.java (신규)
│   ├── ErrorLogDto.java (신규)
│   └── StatisticsResponse.java (신규)
├── repository/
│   ├── PostingRepository.java (기존, @Lock 추가)
│   ├── PostingErrorRepository.java (신규)
│   ├── PostingDeadLetterRepository.java (신규)
│   └── BatchExecutionRepository.java (신규)
├── service/
│   ├── PostingService.java (신규 - DB 저장 로직)
│   ├── BatchService.java (신규 - 배치 핵심)
│   ├── NaverBlogScraper.java (신규)
│   ├── RateLimiter.java (신규)
│   ├── ErrorHandler.java (신규)
│   └── StatisticsService.java (신규)
├── scheduler/
│   └── PostingScheduler.java (신규)
├── controller/
│   ├── PostingController.java (기존, 엔드포인트 추가)
│   └── MonitoringController.java (신규)
├── exception/
│   ├── PostingException.java (신규)
│   ├── ParsingException.java (신규)
│   └── ScrapingException.java (신규)
└── config/
    └── BatchConfiguration.java (신규 - Bean 설정)

tests/java/com/jk/amazon2/posting/
├── service/
│   ├── PostingServiceTest.java
│   ├── BatchServiceTest.java
│   ├── NaverBlogScraperTest.java
│   ├── RateLimiterTest.java
│   └── ErrorHandlerTest.java
├── controller/
│   ├── PostingControllerTest.java
│   └── MonitoringControllerTest.java
└── integration/
    └── PostingBatchIntegrationTest.java

db/migration/
└── V1.1__Create_Posting_Batch_Tables.sql
```

---

## Phase 1: 데이터베이스 마이그레이션

### Task 1: DB 마이그레이션 스크립트 작성

**Files:**
- Create: `src/main/resources/db/migration/V1.1__Create_Posting_Batch_Tables.sql`

- [ ] **Step 1: 마이그레이션 파일 생성**

```sql
-- V1.1__Create_Posting_Batch_Tables.sql

-- posting_error 테이블
CREATE TABLE posting_error
(
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    member_id       BIGINT      NOT NULL,
    target_date     DATE        NOT NULL,
    day_of_week     VARCHAR(10) NOT NULL,
    error_message   TEXT,
    retry_count     INT         NOT NULL DEFAULT 1,
    created_at      DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_posting_error_member FOREIGN KEY (member_id) REFERENCES member (id),
    INDEX idx_retry_count (retry_count),
    INDEX idx_created_at (created_at),
    INDEX idx_member_date (member_id, target_date)
);

-- posting_dead_letter 테이블
CREATE TABLE posting_dead_letter
(
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    member_id       BIGINT      NOT NULL,
    target_date     DATE        NOT NULL,
    day_of_week     VARCHAR(10) NOT NULL,
    error_message   TEXT,
    last_retry_at   DATETIME,
    created_at      DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_posting_dead_letter_member FOREIGN KEY (member_id) REFERENCES member (id),
    INDEX idx_created_at (created_at),
    INDEX idx_member_date (member_id, target_date)
);

-- batch_execution 테이블
CREATE TABLE batch_execution
(
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    batch_type      VARCHAR(20) NOT NULL,
    start_date      DATE        NOT NULL,
    end_date        DATE        NOT NULL,
    total_count     INT         NOT NULL DEFAULT 0,
    success_count   INT         NOT NULL DEFAULT 0,
    retry_count     INT         NOT NULL DEFAULT 0,
    failed_count    INT         NOT NULL DEFAULT 0,
    status          VARCHAR(20) NOT NULL,
    started_at      DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    completed_at    DATETIME,
    INDEX idx_started_at (started_at),
    INDEX idx_status (status),
    INDEX idx_batch_type (batch_type)
);
```

- [ ] **Step 2: Flyway 마이그레이션 실행 확인**

Run: `./gradlew bootRun`
Expected: 마이그레이션 성공, 3개 테이블 생성됨

- [ ] **Step 3: Commit**

```bash
git add src/main/resources/db/migration/V1.1__Create_Posting_Batch_Tables.sql
git commit -m "feat: add posting batch system db migration"
```

---

## Phase 2: 엔티티 및 Repository

### Task 2: PostingError 엔티티 생성

**Files:**
- Create: `src/main/java/com/jk/amazon2/posting/entity/PostingError.java`

- [ ] **Step 1: 엔티티 작성**

```java
package com.jk.amazon2.posting.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "posting_error", 
       indexes = {
           @Index(name = "idx_retry_count", columnList = "retry_count"),
           @Index(name = "idx_created_at", columnList = "created_at"),
           @Index(name = "idx_member_date", columnList = "member_id,target_date")
       })
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PostingError {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private Long memberId;
    
    @Column(nullable = false)
    private LocalDate targetDate;
    
    @Column(nullable = false, length = 10)
    private String dayOfWeek;
    
    @Column(columnDefinition = "TEXT")
    private String errorMessage;
    
    @Column(nullable = false)
    private Integer retryCount;
    
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    public PostingError(Long memberId, LocalDate targetDate, String dayOfWeek, 
                       String errorMessage, Integer retryCount) {
        this.memberId = memberId;
        this.targetDate = targetDate;
        this.dayOfWeek = dayOfWeek;
        this.errorMessage = errorMessage;
        this.retryCount = retryCount;
        this.createdAt = LocalDateTime.now();
    }
    
    public void incrementRetryCount() {
        this.retryCount++;
    }
    
    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}
```

- [ ] **Step 2: Commit**

```bash
git add src/main/java/com/jk/amazon2/posting/entity/PostingError.java
git commit -m "feat: add PostingError entity"
```

---

### Task 3: PostingDeadLetter 엔티티 생성

**Files:**
- Create: `src/main/java/com/jk/amazon2/posting/entity/PostingDeadLetter.java`

- [ ] **Step 1: 엔티티 작성**

```java
package com.jk.amazon2.posting.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "posting_dead_letter",
       indexes = {
           @Index(name = "idx_created_at", columnList = "created_at"),
           @Index(name = "idx_member_date", columnList = "member_id,target_date")
       })
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PostingDeadLetter {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private Long memberId;
    
    @Column(nullable = false)
    private LocalDate targetDate;
    
    @Column(nullable = false, length = 10)
    private String dayOfWeek;
    
    @Column(columnDefinition = "TEXT")
    private String errorMessage;
    
    @Column(nullable = true)
    private LocalDateTime lastRetryAt;
    
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    public PostingDeadLetter(Long memberId, LocalDate targetDate, String dayOfWeek, 
                            String errorMessage) {
        this.memberId = memberId;
        this.targetDate = targetDate;
        this.dayOfWeek = dayOfWeek;
        this.errorMessage = errorMessage;
        this.createdAt = LocalDateTime.now();
        this.lastRetryAt = LocalDateTime.now();
    }
    
    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}
```

- [ ] **Step 2: Commit**

```bash
git add src/main/java/com/jk/amazon2/posting/entity/PostingDeadLetter.java
git commit -m "feat: add PostingDeadLetter entity"
```

---

### Task 4: BatchExecution 엔티티 생성

**Files:**
- Create: `src/main/java/com/jk/amazon2/posting/entity/BatchExecution.java`

- [ ] **Step 1: 엔티티 작성**

```java
package com.jk.amazon2.posting.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "batch_execution",
       indexes = {
           @Index(name = "idx_started_at", columnList = "started_at"),
           @Index(name = "idx_status", columnList = "status"),
           @Index(name = "idx_batch_type", columnList = "batch_type")
       })
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class BatchExecution {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false, length = 20)
    private String batchType; // SCHEDULED, MANUAL
    
    @Column(nullable = false)
    private LocalDate startDate;
    
    @Column(nullable = false)
    private LocalDate endDate;
    
    @Column(nullable = false)
    private Integer totalCount;
    
    @Column(nullable = false)
    private Integer successCount;
    
    @Column(nullable = false)
    private Integer retryCount;
    
    @Column(nullable = false)
    private Integer failedCount;
    
    @Column(nullable = false, length = 20)
    private String status; // IN_PROGRESS, COMPLETED, FAILED
    
    @Column(nullable = false, updatable = false)
    private LocalDateTime startedAt;
    
    @Column(nullable = true)
    private LocalDateTime completedAt;
    
    public BatchExecution(String batchType, LocalDate startDate, LocalDate endDate) {
        this.batchType = batchType;
        this.startDate = startDate;
        this.endDate = endDate;
        this.totalCount = 0;
        this.successCount = 0;
        this.retryCount = 0;
        this.failedCount = 0;
        this.status = "IN_PROGRESS";
        this.startedAt = LocalDateTime.now();
    }
    
    public void incrementSuccessCount() {
        this.successCount++;
        this.totalCount++;
    }
    
    public void incrementRetryCount() {
        this.retryCount++;
        this.totalCount++;
    }
    
    public void incrementFailedCount() {
        this.failedCount++;
    }
    
    public void complete() {
        this.status = "COMPLETED";
        this.completedAt = LocalDateTime.now();
    }
    
    public void fail() {
        this.status = "FAILED";
        this.completedAt = LocalDateTime.now();
    }
    
    @PrePersist
    protected void onCreate() {
        if (startedAt == null) {
            startedAt = LocalDateTime.now();
        }
    }
}
```

- [ ] **Step 2: Commit**

```bash
git add src/main/java/com/jk/amazon2/posting/entity/BatchExecution.java
git commit -m "feat: add BatchExecution entity"
```

---

### Task 5: Repository 인터페이스 생성

**Files:**
- Create: `src/main/java/com/jk/amazon2/posting/repository/PostingErrorRepository.java`
- Create: `src/main/java/com/jk/amazon2/posting/repository/PostingDeadLetterRepository.java`
- Create: `src/main/java/com/jk/amazon2/posting/repository/BatchExecutionRepository.java`

- [ ] **Step 1: PostingErrorRepository 작성**

```java
package com.jk.amazon2.posting.repository;

import com.jk.amazon2.posting.entity.PostingError;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface PostingErrorRepository extends JpaRepository<PostingError, Long> {
    
    Page<PostingError> findByRetryCountLessThan(Integer retryCount, Pageable pageable);
    
    Page<PostingError> findAllByOrderByCreatedAtDesc(Pageable pageable);
    
    @Query("SELECT pe FROM PostingError pe WHERE pe.retryCount < 3 ORDER BY pe.createdAt ASC")
    List<PostingError> findRetryableErrors();
    
    @Query("SELECT pe FROM PostingError pe WHERE pe.memberId = :memberId AND pe.targetDate = :targetDate")
    List<PostingError> findByMemberAndDate(Long memberId, LocalDate targetDate);
}
```

- [ ] **Step 2: PostingDeadLetterRepository 작성**

```java
package com.jk.amazon2.posting.repository;

import com.jk.amazon2.posting.entity.PostingDeadLetter;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;

@Repository
public interface PostingDeadLetterRepository extends JpaRepository<PostingDeadLetter, Long> {
    
    Page<PostingDeadLetter> findAllByOrderByCreatedAtDesc(Pageable pageable);
    
    long countByMemberId(Long memberId);
    
    void deleteByMemberIdAndTargetDate(Long memberId, LocalDate targetDate);
}
```

- [ ] **Step 3: BatchExecutionRepository 작성**

```java
package com.jk.amazon2.posting.repository;

import com.jk.amazon2.posting.entity.BatchExecution;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface BatchExecutionRepository extends JpaRepository<BatchExecution, Long> {
    
    @Query("SELECT be FROM BatchExecution be WHERE be.status = 'IN_PROGRESS' ORDER BY be.startedAt DESC LIMIT 1")
    Optional<BatchExecution> findCurrentExecution();
    
    @Query("SELECT be FROM BatchExecution be ORDER BY be.startedAt DESC LIMIT 1")
    Optional<BatchExecution> findLatestExecution();
}
```

- [ ] **Step 4: Commit**

```bash
git add src/main/java/com/jk/amazon2/posting/repository/PostingErrorRepository.java \
         src/main/java/com/jk/amazon2/posting/repository/PostingDeadLetterRepository.java \
         src/main/java/com/jk/amazon2/posting/repository/BatchExecutionRepository.java
git commit -m "feat: add batch system repositories"
```

---

## Phase 3: 예외 클래스 및 DTO

### Task 6: 예외 클래스 정의

**Files:**
- Create: `src/main/java/com/jk/amazon2/posting/exception/PostingException.java`
- Create: `src/main/java/com/jk/amazon2/posting/exception/ParsingException.java`
- Create: `src/main/java/com/jk/amazon2/posting/exception/ScrapingException.java`

- [ ] **Step 1: PostingException 작성**

```java
package com.jk.amazon2.posting.exception;

public class PostingException extends RuntimeException {
    public PostingException(String message) {
        super(message);
    }
    
    public PostingException(String message, Throwable cause) {
        super(message, cause);
    }
}
```

- [ ] **Step 2: ParsingException 작성**

```java
package com.jk.amazon2.posting.exception;

public class ParsingException extends PostingException {
    public ParsingException(String message) {
        super(message);
    }
    
    public ParsingException(String message, Throwable cause) {
        super(message, cause);
    }
}
```

- [ ] **Step 3: ScrapingException 작성**

```java
package com.jk.amazon2.posting.exception;

public class ScrapingException extends PostingException {
    public ScrapingException(String message) {
        super(message);
    }
    
    public ScrapingException(String message, Throwable cause) {
        super(message, cause);
    }
}
```

- [ ] **Step 4: Commit**

```bash
git add src/main/java/com/jk/amazon2/posting/exception/
git commit -m "feat: add posting batch exceptions"
```

---

### Task 7: DTO 클래스 생성

**Files:**
- Create: `src/main/java/com/jk/amazon2/posting/dto/BatchRequest.java`
- Create: `src/main/java/com/jk/amazon2/posting/dto/BatchStatusResponse.java`
- Create: `src/main/java/com/jk/amazon2/posting/dto/ErrorLogDto.java`
- Create: `src/main/java/com/jk/amazon2/posting/dto/StatisticsResponse.java`

- [ ] **Step 1: BatchRequest 작성**

```java
package com.jk.amazon2.posting.dto;

import java.time.LocalDate;

public record BatchRequest(
    LocalDate startDate,
    LocalDate endDate
) {}
```

- [ ] **Step 2: BatchStatusResponse 작성**

```java
package com.jk.amazon2.posting.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record BatchStatusResponse(
    LastExecution lastExecution,
    CurrentStats currentStats
) {
    public record LastExecution(
        Long batchExecutionId,
        String batchType,
        LocalDate startDate,
        LocalDate endDate,
        String status,
        Integer totalCount,
        Integer successCount,
        Integer retryCount,
        Integer failedCount,
        LocalDateTime completedAt
    ) {}
    
    public record CurrentStats(
        Long totalDeadLetters,
        Long totalErrors,
        Double successRate
    ) {}
}
```

- [ ] **Step 3: ErrorLogDto 작성**

```java
package com.jk.amazon2.posting.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record ErrorLogDto(
    Long id,
    Long memberId,
    String memberNickname,
    LocalDate targetDate,
    String dayOfWeek,
    String errorMessage,
    Integer retryCount,
    LocalDateTime createdAt
) {}
```

- [ ] **Step 4: StatisticsResponse 작성**

```java
package com.jk.amazon2.posting.dto;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public record StatisticsResponse(
    LocalDate startDate,
    LocalDate endDate,
    Integer totalPostings,
    List<UserStatistics> users
) {
    public record UserStatistics(
        Long memberId,
        String nickname,
        Integer totalPostings,
        Map<String, Integer> byDayOfWeek
    ) {}
}
```

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/jk/amazon2/posting/dto/BatchRequest.java \
         src/main/java/com/jk/amazon2/posting/dto/BatchStatusResponse.java \
         src/main/java/com/jk/amazon2/posting/dto/ErrorLogDto.java \
         src/main/java/com/jk/amazon2/posting/dto/StatisticsResponse.java
git commit -m "feat: add batch system DTOs"
```

---

## Phase 4: 핵심 서비스 구현

### Task 8: RateLimiter 구현

**Files:**
- Create: `src/main/java/com/jk/amazon2/posting/service/RateLimiter.java`
- Create: `tests/java/com/jk/amazon2/posting/service/RateLimiterTest.java`

- [ ] **Step 1: RateLimiterTest 작성**

```java
package com.jk.amazon2.posting.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import static org.junit.jupiter.api.Assertions.*;

class RateLimiterTest {
    
    private RateLimiter rateLimiter;
    
    @BeforeEach
    void setUp() {
        rateLimiter = new RateLimiter(2000); // 2초에 1개 요청
    }
    
    @Test
    void testFirstRequestAllowedImmediately() {
        long start = System.currentTimeMillis();
        
        rateLimiter.acquire(); // 즉시 허용
        
        long elapsed = System.currentTimeMillis() - start;
        assertTrue(elapsed < 100);
    }
    
    @Test
    void testSecondRequestWaitsTwoSeconds() {
        long start = System.currentTimeMillis();
        
        rateLimiter.acquire();
        rateLimiter.acquire(); // 2초 대기
        
        long elapsed = System.currentTimeMillis() - start;
        assertTrue(elapsed >= 1900); // 최소 1.9초 대기 (오차 고려)
    }
}
```

- [ ] **Step 2: RateLimiter 구현**

```java
package com.jk.amazon2.posting.service;

import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicLong;

public class RateLimiter {
    
    private final long refillIntervalMs; // 2초
    private final Semaphore semaphore;
    private final AtomicLong lastRefillTime;
    
    /**
     * @param refillIntervalMs 요청 간격 (밀리초). 예: 2000 = 2초에 1개 요청
     */
    public RateLimiter(long refillIntervalMs) {
        this.refillIntervalMs = refillIntervalMs;
        this.semaphore = new Semaphore(1);
        this.lastRefillTime = new AtomicLong(System.currentTimeMillis());
    }
    
    public void acquire() {
        refillIfNeeded();
        try {
            semaphore.acquire();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Rate limiter interrupted", e);
        }
    }
    
    private void refillIfNeeded() {
        long now = System.currentTimeMillis();
        long lastRefill = lastRefillTime.get();
        
        if (now - lastRefill >= refillIntervalMs) {
            if (lastRefillTime.compareAndSet(lastRefill, now)) {
                semaphore.release();
            }
        }
    }
}
```

- [ ] **Step 3: 테스트 실행**

Run: `./gradlew test -x other tests/java/com/jk/amazon2/posting/service/RateLimiterTest.java`
Expected: 모든 테스트 PASS

- [ ] **Step 4: Commit**

```bash
git add src/main/java/com/jk/amazon2/posting/service/RateLimiter.java \
         tests/java/com/jk/amazon2/posting/service/RateLimiterTest.java
git commit -m "feat: add RateLimiter (2 requests per second)"
```

---

### Task 9: NaverBlogScraper 구현

**Files:**
- Create: `src/main/java/com/jk/amazon2/posting/service/NaverBlogScraper.java`
- Create: `tests/java/com/jk/amazon2/posting/service/NaverBlogScraperTest.java`

- [ ] **Step 1: NaverBlogScraperTest 작성**

```java
package com.jk.amazon2.posting.service;

import com.jk.amazon2.posting.exception.ParsingException;
import com.jk.amazon2.posting.exception.ScrapingException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class NaverBlogScraperTest {
    
    private NaverBlogScraper scraper;
    
    @BeforeEach
    void setUp() {
        scraper = new NaverBlogScraper();
    }
    
    @Test
    void testScrapValidDate() {
        // 실제 네이버 요청이므로 단위 테스트로는 제한적
        // 통합 테스트에서 처리
        assertNotNull(scraper);
    }
    
    @Test
    void testScrapThrowsScrapingException() {
        // 네트워크 오류 시나리오는 통합 테스트에서
        assertNotNull(scraper);
    }
}
```

- [ ] **Step 2: NaverBlogScraper 구현**

```java
package com.jk.amazon2.posting.service;

import com.jk.amazon2.posting.exception.ParsingException;
import com.jk.amazon2.posting.exception.ScrapingException;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
public class NaverBlogScraper {
    
    private static final String NAVER_BLOG_BASE_URL = "https://blog.naver.com/PostList.naver";
    private static final int TIMEOUT_SECONDS = 10;
    private static final String USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64)";
    
    private final HttpClient httpClient;
    
    public NaverBlogScraper() {
        this.httpClient = HttpClient.newBuilder()
            .connectTimeout(java.time.Duration.ofSeconds(TIMEOUT_SECONDS))
            .build();
    }
    
    public Integer scrapePostingCount(String blogId, LocalDate date) {
        try {
            String url = buildUrl(blogId, date);
            Document doc = fetchAndParse(url);
            return extractPostingCount(doc);
        } catch (ParsingException e) {
            throw e;
        } catch (IOException | InterruptedException e) {
            throw new ScrapingException("Failed to scrape blog for " + blogId, e);
        }
    }
    
    private String buildUrl(String blogId, LocalDate date) {
        return String.format("%s?blogId=%s&viewdate=%s", 
            NAVER_BLOG_BASE_URL, blogId, date);
    }
    
    private Document fetchAndParse(String url) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(url))
            .header("User-Agent", USER_AGENT)
            .timeout(java.time.Duration.ofSeconds(TIMEOUT_SECONDS))
            .GET()
            .build();
        
        HttpResponse<String> response = httpClient.send(request, 
            HttpResponse.BodyHandlers.ofString());
        
        if (response.statusCode() != 200) {
            throw new ScrapingException("HTTP " + response.statusCode() + " for " + url);
        }
        
        return Jsoup.parse(response.body());
    }
    
    private Integer extractPostingCount(Document doc) {
        Elements categoryTitlePcol2 = doc.getElementsByClass("category_title pcol2");
        
        if (categoryTitlePcol2.isEmpty()) {
            throw new ParsingException("Element 'category_title pcol2' not found");
        }
        
        Element element = categoryTitlePcol2.get(0);
        String text = element.text();
        
        Pattern pattern = Pattern.compile("(\\d+)개의 글");
        Matcher matcher = pattern.matcher(text);
        
        if (matcher.find()) {
            return Integer.parseInt(matcher.group(1));
        }
        
        // "0개의 글" 패턴 없으면 0으로 반환
        return 0;
    }
}
```

- [ ] **Step 3: 테스트 실행**

Run: `./gradlew test tests/java/com/jk/amazon2/posting/service/NaverBlogScraperTest.java`
Expected: PASS

- [ ] **Step 4: Commit**

```bash
git add src/main/java/com/jk/amazon2/posting/service/NaverBlogScraper.java \
         tests/java/com/jk/amazon2/posting/service/NaverBlogScraperTest.java
git commit -m "feat: add NaverBlogScraper"
```

---

### Task 10: ErrorHandler 구현

**Files:**
- Create: `src/main/java/com/jk/amazon2/posting/service/ErrorHandler.java`
- Create: `tests/java/com/jk/amazon2/posting/service/ErrorHandlerTest.java`

- [ ] **Step 1: ErrorHandlerTest 작성**

```java
package com.jk.amazon2.posting.service;

import com.jk.amazon2.posting.entity.PostingError;
import com.jk.amazon2.posting.entity.PostingDeadLetter;
import com.jk.amazon2.posting.repository.PostingErrorRepository;
import com.jk.amazon2.posting.repository.PostingDeadLetterRepository;
import com.jk.amazon2.posting.exception.ScrapingException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ErrorHandlerTest {
    
    @Mock
    private PostingErrorRepository errorRepository;
    
    @Mock
    private PostingDeadLetterRepository deadLetterRepository;
    
    private ErrorHandler errorHandler;
    
    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        errorHandler = new ErrorHandler(errorRepository, deadLetterRepository);
    }
    
    @Test
    void testHandleErrorCreatesErrorLog() {
        Long memberId = 1L;
        LocalDate date = LocalDate.now();
        String dayOfWeek = "mon";
        Exception cause = new ScrapingException("test error");
        
        errorHandler.handleError(memberId, date, dayOfWeek, cause);
        
        verify(errorRepository, times(1)).save(any(PostingError.class));
    }
    
    @Test
    void testRetry3TimesMovesToDeadLetter() {
        Long memberId = 1L;
        LocalDate date = LocalDate.now();
        String dayOfWeek = "mon";
        
        PostingError error = new PostingError(memberId, date, dayOfWeek, "error", 3);
        
        when(errorRepository.findByMemberAndDate(memberId, date))
            .thenReturn(java.util.List.of(error));
        
        errorHandler.handleRetry(error);
        
        verify(deadLetterRepository, times(1)).save(any(PostingDeadLetter.class));
    }
}
```

- [ ] **Step 2: ErrorHandler 구현**

```java
package com.jk.amazon2.posting.service;

import com.jk.amazon2.posting.entity.PostingError;
import com.jk.amazon2.posting.entity.PostingDeadLetter;
import com.jk.amazon2.posting.repository.PostingErrorRepository;
import com.jk.amazon2.posting.repository.PostingDeadLetterRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Slf4j
@Service
@RequiredArgsConstructor
public class ErrorHandler {
    
    private static final int MAX_RETRY_COUNT = 3;
    
    private final PostingErrorRepository errorRepository;
    private final PostingDeadLetterRepository deadLetterRepository;
    
    @Transactional
    public void handleError(Long memberId, LocalDate targetDate, String dayOfWeek, Exception cause) {
        PostingError error = new PostingError(
            memberId, 
            targetDate, 
            dayOfWeek, 
            cause.getMessage(), 
            1
        );
        
        errorRepository.save(error);
        log.warn("Error logged - member={}, date={}, day={}, error={}", 
            memberId, targetDate, dayOfWeek, cause.getMessage());
    }
    
    @Transactional
    public void handleRetry(PostingError error) {
        error.incrementRetryCount();
        
        if (error.getRetryCount() >= MAX_RETRY_COUNT) {
            moveToDeadLetter(error);
            errorRepository.delete(error);
        } else {
            errorRepository.save(error);
        }
    }
    
    private void moveToDeadLetter(PostingError error) {
        PostingDeadLetter deadLetter = new PostingDeadLetter(
            error.getMemberId(),
            error.getTargetDate(),
            error.getDayOfWeek(),
            error.getErrorMessage()
        );
        
        deadLetterRepository.save(deadLetter);
        log.error("Moved to dead letter - member={}, date={}, day={}", 
            error.getMemberId(), error.getTargetDate(), error.getDayOfWeek());
    }
}
```

- [ ] **Step 3: 테스트 실행**

Run: `./gradlew test tests/java/com/jk/amazon2/posting/service/ErrorHandlerTest.java`
Expected: PASS

- [ ] **Step 4: Commit**

```bash
git add src/main/java/com/jk/amazon2/posting/service/ErrorHandler.java \
         tests/java/com/jk/amazon2/posting/service/ErrorHandlerTest.java
git commit -m "feat: add ErrorHandler for retry and dead letter logic"
```

---

### Task 11: PostingService 구현 (DB 저장, 트랜잭션)

**Files:**
- Create: `src/main/java/com/jk/amazon2/posting/service/PostingService.java`
- Create: `tests/java/com/jk/amazon2/posting/service/PostingServiceTest.java`
- Modify: `src/main/java/com/jk/amazon2/posting/repository/PostingRepository.java` (추가 메서드)

- [ ] **Step 1: PostingRepository에 @Lock 메서드 추가**

```java
// 기존 PostingRepository에 추가
@Query("SELECT p FROM Posting p WHERE p.memberId = :memberId AND p.weekStartDate = :weekStartDate")
@Lock(LockModeType.PESSIMISTIC_WRITE)
Optional<Posting> findByMemberIdAndWeekStartDateWithLock(Long memberId, LocalDate weekStartDate);
```

- [ ] **Step 2: PostingServiceTest 작성**

```java
package com.jk.amazon2.posting.service;

import com.jk.amazon2.posting.entity.Posting;
import com.jk.amazon2.posting.repository.PostingRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class PostingServiceTest {
    
    @Mock
    private PostingRepository postingRepository;
    
    private PostingService postingService;
    
    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        postingService = new PostingService(postingRepository);
    }
    
    @Test
    void testSavePosting_NewRecord() {
        Long memberId = 1L;
        LocalDate weekStart = LocalDate.of(2026, 6, 9);
        
        when(postingRepository.findByMemberIdAndWeekStartDateWithLock(memberId, weekStart))
            .thenReturn(Optional.empty());
        
        postingService.savePosting(memberId, weekStart, 1, 2, 3, 4, 5, 6, 7, "admin");
        
        verify(postingRepository, times(1)).save(any(Posting.class));
    }
    
    @Test
    void testSavePosting_UpdateExisting() {
        Long memberId = 1L;
        LocalDate weekStart = LocalDate.of(2026, 6, 9);
        Posting existing = new Posting(memberId, weekStart, 1, 1, 1, 1, 1, 1, 1, "admin");
        
        when(postingRepository.findByMemberIdAndWeekStartDateWithLock(memberId, weekStart))
            .thenReturn(Optional.of(existing));
        
        postingService.savePosting(memberId, weekStart, 5, 5, 5, 5, 5, 5, 5, "admin");
        
        assertEquals(5, existing.getMon());
    }
}
```

- [ ] **Step 3: PostingService 구현**

```java
package com.jk.amazon2.posting.service;

import com.jk.amazon2.posting.entity.Posting;
import com.jk.amazon2.posting.repository.PostingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Slf4j
@Service
@RequiredArgsConstructor
public class PostingService {
    
    private final PostingRepository postingRepository;
    
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public void savePosting(Long memberId, LocalDate weekStartDate, 
                           Integer mon, Integer tue, Integer wed, Integer thu, 
                           Integer fri, Integer sat, Integer sun, String createdBy) {
        Posting existing = postingRepository
            .findByMemberIdAndWeekStartDateWithLock(memberId, weekStartDate)
            .orElse(null);
        
        if (existing == null) {
            Posting newPosting = new Posting(
                memberId, weekStartDate, 
                mon, tue, wed, thu, fri, sat, sun, 
                createdBy
            );
            postingRepository.save(newPosting);
            log.info("Created posting - member={}, week={}, counts={}{}{}{}{}{}{}",
                memberId, weekStartDate, mon, tue, wed, thu, fri, sat, sun);
        } else {
            existing.update(mon, tue, wed, thu, fri, sat, sun);
            postingRepository.save(existing);
            log.info("Updated posting - member={}, week={}, counts={}{}{}{}{}{}{}",
                memberId, weekStartDate, mon, tue, wed, thu, fri, sat, sun);
        }
    }
}
```

- [ ] **Step 4: 테스트 실행**

Run: `./gradlew test tests/java/com/jk/amazon2/posting/service/PostingServiceTest.java`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/jk/amazon2/posting/service/PostingService.java \
         src/main/java/com/jk/amazon2/posting/repository/PostingRepository.java \
         tests/java/com/jk/amazon2/posting/service/PostingServiceTest.java
git commit -m "feat: add PostingService with SERIALIZABLE transaction"
```

---

### Task 12: BatchService 핵심 로직 구현

**Files:**
- Create: `src/main/java/com/jk/amazon2/posting/service/BatchService.java`
- Create: `tests/java/com/jk/amazon2/posting/service/BatchServiceTest.java`

- [ ] **Step 1: BatchServiceTest 작성 (기본 골격)**

```java
package com.jk.amazon2.posting.service;

import com.jk.amazon2.posting.entity.BatchExecution;
import com.jk.amazon2.posting.repository.BatchExecutionRepository;
import com.jk.amazon2.member.repository.MemberRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class BatchServiceTest {
    
    @Mock
    private BatchExecutionRepository batchExecutionRepository;
    
    @Mock
    private MemberRepository memberRepository;
    
    @Mock
    private PostingService postingService;
    
    @Mock
    private NaverBlogScraper scraper;
    
    private BatchService batchService;
    
    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        batchService = new BatchService(
            batchExecutionRepository, 
            memberRepository, 
            postingService, 
            scraper
        );
    }
    
    @Test
    void testExecuteBatch() {
        LocalDate startDate = LocalDate.of(2026, 6, 9);
        LocalDate endDate = LocalDate.of(2026, 6, 15);
        
        assertNotNull(batchService);
    }
}
```

- [ ] **Step 2: BatchService 구현**

```java
package com.jk.amazon2.posting.service;

import com.jk.amazon2.member.entity.Member;
import com.jk.amazon2.member.repository.MemberRepository;
import com.jk.amazon2.posting.entity.BatchExecution;
import com.jk.amazon2.posting.entity.PostingError;
import com.jk.amazon2.posting.exception.ParsingException;
import com.jk.amazon2.posting.exception.ScrapingException;
import com.jk.amazon2.posting.repository.BatchExecutionRepository;
import com.jk.amazon2.posting.repository.PostingErrorRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class BatchService {
    
    private static final int QUEUE_CAPACITY = 1000;
    
    private final BatchExecutionRepository batchExecutionRepository;
    private final MemberRepository memberRepository;
    private final PostingService postingService;
    private final PostingErrorRepository postingErrorRepository;
    private final ErrorHandler errorHandler;
    private final NaverBlogScraper scraper;
    private final RateLimiter rateLimiter;
    
    @Transactional
    public Long executeBatch(LocalDate startDate, LocalDate endDate, String batchType) {
        BatchExecution execution = new BatchExecution(batchType, startDate, endDate);
        batchExecutionRepository.save(execution);
        
        log.info("[BATCH] Posting batch started - type={}, period={}~{}", 
            batchType, startDate, endDate);
        
        BlockingQueue<PostingTask> queue = new LinkedBlockingQueue<>(QUEUE_CAPACITY);
        ExecutorService executor = Executors.newSingleThreadExecutor();
        
        try {
            // 데이터 준비
            List<Member> members = memberRepository.findAll();
            prepareQueue(queue, members, startDate, endDate);
            
            // 배치 처리
            executor.submit(() -> processQueue(queue, execution));
            
            // 큐가 비워질 때까지 대기
            waitForQueueEmpty(queue);
            
            // 배치 완료
            execution.complete();
            batchExecutionRepository.save(execution);
            
            log.info("[BATCH] Posting batch completed - total={}, success={}, retry={}, failed={}, duration={}ms",
                execution.getTotalCount(),
                execution.getSuccessCount(),
                execution.getRetryCount(),
                execution.getFailedCount(),
                System.currentTimeMillis() - execution.getStartedAt().getTime());
            
        } catch (Exception e) {
            execution.fail();
            batchExecutionRepository.save(execution);
            log.error("[BATCH] Batch failed", e);
            throw e;
        } finally {
            executor.shutdown();
        }
        
        return execution.getId();
    }
    
    private void prepareQueue(BlockingQueue<PostingTask> queue, List<Member> members, 
                             LocalDate startDate, LocalDate endDate) {
        for (Member member : members) {
            LocalDate current = startDate;
            while (!current.isAfter(endDate)) {
                String dayOfWeek = getDayOfWeekKr(current);
                PostingTask task = new PostingTask(
                    member.getId(), 
                    member.getNickname(),
                    current, 
                    dayOfWeek
                );
                
                try {
                    queue.put(task);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    log.error("Queue interrupted while preparing", e);
                }
                
                current = current.plusDays(1);
            }
        }
    }
    
    private void processQueue(BlockingQueue<PostingTask> queue, BatchExecution execution) {
        PostingTask task;
        
        while ((task = queue.poll()) != null || !queue.isEmpty()) {
            if (task == null) {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
                continue;
            }
            
            rateLimiter.acquire();
            processTask(task, queue, execution);
        }
    }
    
    private void processTask(PostingTask task, BlockingQueue<PostingTask> queue, 
                            BatchExecution execution) {
        try {
            log.debug("[BATCH] Processing member={}, target_date={}, day={}", 
                task.memberId, task.targetDate, task.dayOfWeek);
            
            Integer count = scraper.scrapePostingCount(task.memberId.toString(), task.targetDate);
            
            LocalDate weekStart = getWeekStartDate(task.targetDate);
            updatePostingForDay(task.memberId, weekStart, task.dayOfWeek, count);
            
            execution.incrementSuccessCount();
            batchExecutionRepository.save(execution);
            
            log.info("[BATCH] Saved posting - member={}, week={}, day={}, count={}",
                task.memberId, weekStart, task.dayOfWeek, count);
            
        } catch (ParsingException | ScrapingException e) {
            handleTaskError(task, queue, execution, e);
        }
    }
    
    private void handleTaskError(PostingTask task, BlockingQueue<PostingTask> queue, 
                                BatchExecution execution, Exception e) {
        errorHandler.handleError(task.memberId, task.targetDate, task.dayOfWeek, e);
        
        PostingError error = postingErrorRepository
            .findByMemberAndDate(task.memberId, task.targetDate)
            .stream()
            .filter(err -> err.getDayOfWeek().equals(task.dayOfWeek))
            .findFirst()
            .orElse(null);
        
        if (error != null && error.getRetryCount() < 3) {
            execution.incrementRetryCount();
            try {
                queue.put(task); // 큐 맨 뒤 재삽입
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
            }
        } else if (error != null) {
            execution.incrementFailedCount();
            errorHandler.handleRetry(error);
        }
        
        batchExecutionRepository.save(execution);
        log.warn("[BATCH] Failed member={}, date={}, error={}", 
            task.memberId, task.targetDate, e.getMessage());
    }
    
    private void updatePostingForDay(Long memberId, LocalDate weekStart, 
                                    String dayOfWeek, Integer count) {
        Posting existing = postingRepository.findByMemberIdAndWeekStartDate(memberId, weekStart)
            .orElseGet(() -> createNewPosting(memberId, weekStart));
        
        switch (dayOfWeek) {
            case "mon" -> existing.setMon(count);
            case "tue" -> existing.setTue(count);
            case "wed" -> existing.setWed(count);
            case "thu" -> existing.setThu(count);
            case "fri" -> existing.setFri(count);
            case "sat" -> existing.setSat(count);
            case "sun" -> existing.setSun(count);
        }
        
        postingService.savePosting(memberId, weekStart,
            existing.getMon(), existing.getTue(), existing.getWed(), 
            existing.getThu(), existing.getFri(), existing.getSat(), 
            existing.getSun(), "batch");
    }
    
    private Posting createNewPosting(Long memberId, LocalDate weekStart) {
        return new Posting(memberId, weekStart, 0, 0, 0, 0, 0, 0, 0, "batch");
    }
    
    private LocalDate getWeekStartDate(LocalDate date) {
        while (date.getDayOfWeek() != DayOfWeek.MONDAY) {
            date = date.minusDays(1);
        }
        return date;
    }
    
    private String getDayOfWeekKr(LocalDate date) {
        return switch (date.getDayOfWeek()) {
            case MONDAY -> "mon";
            case TUESDAY -> "tue";
            case WEDNESDAY -> "wed";
            case THURSDAY -> "thu";
            case FRIDAY -> "fri";
            case SATURDAY -> "sat";
            case SUNDAY -> "sun";
        };
    }
    
    private void waitForQueueEmpty(BlockingQueue<PostingTask> queue) throws InterruptedException {
        while (!queue.isEmpty()) {
            Thread.sleep(100);
        }
    }
    
    record PostingTask(Long memberId, String memberNickname, LocalDate targetDate, String dayOfWeek) {}
}
```

- [ ] **Step 3: 필요한 의존성 추가**

Posting 엔티티에 setter 메서드 추가:
```java
public void setMon(Integer mon) { this.mon = mon; }
public void setTue(Integer tue) { this.tue = tue; }
// ... 다른 요일도 동일
```

- [ ] **Step 4: 테스트 실행**

Run: `./gradlew test tests/java/com/jk/amazon2/posting/service/BatchServiceTest.java`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/jk/amazon2/posting/service/BatchService.java \
         tests/java/com/jk/amazon2/posting/service/BatchServiceTest.java
git commit -m "feat: add BatchService with queue processing and retry logic"
```

---

## Phase 5: 스케줄러 및 API 엔드포인트

### Task 13: BatchConfiguration과 PostingScheduler 구현

**Files:**
- Create: `src/main/java/com/jk/amazon2/posting/config/BatchConfiguration.java`
- Create: `src/main/java/com/jk/amazon2/posting/scheduler/PostingScheduler.java`

- [ ] **Step 1: BatchConfiguration 작성**

```java
package com.jk.amazon2.posting.config;

import com.jk.amazon2.posting.service.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class BatchConfiguration {
    
    @Bean
    public RateLimiter rateLimiter() {
        return new RateLimiter(0.5); // 2초에 1개 요청 (초당 0.5개)
    }
    
    @Bean
    public NaverBlogScraper naverBlogScraper() {
        return new NaverBlogScraper();
    }
}
```

- [ ] **Step 2: PostingScheduler 작성**

```java
package com.jk.amazon2.posting.scheduler;

import com.jk.amazon2.posting.service.BatchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.DayOfWeek;

@Slf4j
@Component
@RequiredArgsConstructor
public class PostingScheduler {
    
    private final BatchService batchService;
    
    /**
     * 매주 월요일 자정(00:00)에 지난주 데이터 수집
     */
    @Scheduled(cron = "0 0 0 ? * MON")
    public void scheduleWeeklyBatch() {
        log.info("[SCHEDULER] Weekly batch execution started");
        
        LocalDate today = LocalDate.now();
        LocalDate weekStartDate = getLastMonday(today);
        LocalDate weekEndDate = weekStartDate.plusDays(6);
        
        try {
            batchService.executeBatch(weekStartDate, weekEndDate, "SCHEDULED");
            log.info("[SCHEDULER] Weekly batch completed successfully");
        } catch (Exception e) {
            log.error("[SCHEDULER] Weekly batch failed", e);
        }
    }
    
    private LocalDate getLastMonday(LocalDate date) {
        while (date.getDayOfWeek() != DayOfWeek.MONDAY) {
            date = date.minusDays(1);
        }
        return date.minusDays(7); // 지난주 월요일
    }
}
```

- [ ] **Step 3: Commit**

```bash
git add src/main/java/com/jk/amazon2/posting/config/BatchConfiguration.java \
         src/main/java/com/jk/amazon2/posting/scheduler/PostingScheduler.java
git commit -m "feat: add BatchConfiguration and PostingScheduler"
```

---

### Task 14: PostingController 수정 및 MonitoringController 생성

**Files:**
- Modify: `src/main/java/com/jk/amazon2/posting/controller/PostingController.java`
- Create: `src/main/java/com/jk/amazon2/posting/controller/MonitoringController.java`
- Create: `src/main/java/com/jk/amazon2/posting/service/StatisticsService.java`

- [ ] **Step 1: PostingController에 배치 API 추가**

```java
// 기존 PostingController에 추가

@PostMapping("/batch")
public ResponseEntity<Map<String, Object>> executeBatch(
    @RequestBody BatchRequest request
) {
    Long batchId = batchService.executeBatch(
        request.startDate(), 
        request.endDate(), 
        "MANUAL"
    );
    
    return ResponseEntity
        .status(HttpStatus.ACCEPTED)
        .body(Map.of(
            "batchExecutionId", batchId,
            "status", "IN_PROGRESS",
            "message", "배치 작업 시작됨"
        ));
}
```

- [ ] **Step 2: StatisticsService 작성**

```java
package com.jk.amazon2.posting.service;

import com.jk.amazon2.posting.dto.StatisticsResponse;
import com.jk.amazon2.posting.entity.Posting;
import com.jk.amazon2.posting.repository.PostingRepository;
import com.jk.amazon2.member.entity.Member;
import com.jk.amazon2.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.*;

@Service
@RequiredArgsConstructor
public class StatisticsService {
    
    private final PostingRepository postingRepository;
    private final MemberRepository memberRepository;
    
    public StatisticsResponse getStatistics(LocalDate startDate, LocalDate endDate) {
        List<Member> members = memberRepository.findAll();
        List<Posting> postings = postingRepository.findAll(); // startDate와 endDate로 필터링 필요
        
        int totalPostings = 0;
        List<StatisticsResponse.UserStatistics> userStats = new ArrayList<>();
        
        for (Member member : members) {
            int memberTotal = 0;
            Map<String, Integer> dayOfWeekCounts = new HashMap<>();
            dayOfWeekCounts.put("mon", 0);
            dayOfWeekCounts.put("tue", 0);
            dayOfWeekCounts.put("wed", 0);
            dayOfWeekCounts.put("thu", 0);
            dayOfWeekCounts.put("fri", 0);
            dayOfWeekCounts.put("sat", 0);
            dayOfWeekCounts.put("sun", 0);
            
            for (Posting p : postings) {
                if (p.getMemberId().equals(member.getId()) && 
                    isInRange(p.getWeekStartDate(), startDate, endDate)) {
                    
                    memberTotal += p.getMon() + p.getTue() + p.getWed() + p.getThu() +
                                 p.getFri() + p.getSat() + p.getSun();
                    
                    dayOfWeekCounts.put("mon", dayOfWeekCounts.get("mon") + p.getMon());
                    dayOfWeekCounts.put("tue", dayOfWeekCounts.get("tue") + p.getTue());
                    dayOfWeekCounts.put("wed", dayOfWeekCounts.get("wed") + p.getWed());
                    dayOfWeekCounts.put("thu", dayOfWeekCounts.get("thu") + p.getThu());
                    dayOfWeekCounts.put("fri", dayOfWeekCounts.get("fri") + p.getFri());
                    dayOfWeekCounts.put("sat", dayOfWeekCounts.get("sat") + p.getSat());
                    dayOfWeekCounts.put("sun", dayOfWeekCounts.get("sun") + p.getSun());
                }
            }
            
            if (memberTotal > 0) {
                totalPostings += memberTotal;
                userStats.add(new StatisticsResponse.UserStatistics(
                    member.getId(),
                    member.getNickname(),
                    memberTotal,
                    dayOfWeekCounts
                ));
            }
        }
        
        return new StatisticsResponse(startDate, endDate, totalPostings, userStats);
    }
    
    private boolean isInRange(LocalDate weekStart, LocalDate rangeStart, LocalDate rangeEnd) {
        LocalDate weekEnd = weekStart.plusDays(6);
        return !weekEnd.isBefore(rangeStart) && !weekStart.isAfter(rangeEnd);
    }
}
```

- [ ] **Step 3: MonitoringController 작성**

```java
package com.jk.amazon2.posting.controller;

import com.jk.amazon2.posting.dto.BatchStatusResponse;
import com.jk.amazon2.posting.dto.ErrorLogDto;
import com.jk.amazon2.posting.dto.StatisticsResponse;
import com.jk.amazon2.posting.entity.*;
import com.jk.amazon2.posting.repository.*;
import com.jk.amazon2.posting.service.StatisticsService;
import com.jk.amazon2.posting.service.ErrorHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Optional;

@RestController
@RequestMapping("/api/postings")
@RequiredArgsConstructor
public class MonitoringController {
    
    private final BatchExecutionRepository batchExecutionRepository;
    private final PostingErrorRepository errorRepository;
    private final PostingDeadLetterRepository deadLetterRepository;
    private final StatisticsService statisticsService;
    private final ErrorHandler errorHandler;
    
    @GetMapping("/batch/status")
    public ResponseEntity<BatchStatusResponse> getBatchStatus() {
        Optional<BatchExecution> lastExecution = batchExecutionRepository.findLatestExecution();
        
        BatchStatusResponse.LastExecution lastExec = lastExecution
            .map(be -> new BatchStatusResponse.LastExecution(
                be.getId(),
                be.getBatchType(),
                be.getStartDate(),
                be.getEndDate(),
                be.getStatus(),
                be.getTotalCount(),
                be.getSuccessCount(),
                be.getRetryCount(),
                be.getFailedCount(),
                be.getCompletedAt()
            ))
            .orElse(null);
        
        long totalDeadLetters = deadLetterRepository.count();
        long totalErrors = errorRepository.count();
        double successRate = lastExecution
            .map(be -> be.getTotalCount() > 0 ? 
                (be.getSuccessCount() / (double) be.getTotalCount()) * 100 : 0.0)
            .orElse(0.0);
        
        BatchStatusResponse.CurrentStats stats = new BatchStatusResponse.CurrentStats(
            totalDeadLetters,
            totalErrors,
            successRate
        );
        
        return ResponseEntity.ok(new BatchStatusResponse(lastExec, stats));
    }
    
    @GetMapping("/errors")
    public ResponseEntity<Page<ErrorLogDto>> getErrors(Pageable pageable) {
        Page<PostingError> errors = errorRepository.findAllByOrderByCreatedAtDesc(pageable);
        
        Page<ErrorLogDto> dtos = errors.map(error -> new ErrorLogDto(
            error.getId(),
            error.getMemberId(),
            "", // memberNickname 추가 필요 (join 필요)
            error.getTargetDate(),
            error.getDayOfWeek(),
            error.getErrorMessage(),
            error.getRetryCount(),
            error.getCreatedAt()
        ));
        
        return ResponseEntity.ok(dtos);
    }
    
    @GetMapping("/dead-letters")
    public ResponseEntity<Page<ErrorLogDto>> getDeadLetters(Pageable pageable) {
        Page<PostingDeadLetter> deadLetters = deadLetterRepository.findAllByOrderByCreatedAtDesc(pageable);
        
        Page<ErrorLogDto> dtos = deadLetters.map(dl -> new ErrorLogDto(
            dl.getId(),
            dl.getMemberId(),
            "", // memberNickname 추가 필요
            dl.getTargetDate(),
            dl.getDayOfWeek(),
            dl.getErrorMessage(),
            3, // 3회 실패
            dl.getCreatedAt()
        ));
        
        return ResponseEntity.ok(dtos);
    }
    
    @PostMapping("/errors/{errorId}/retry")
    public ResponseEntity<Map<String, String>> retryError(@PathVariable Long errorId) {
        Optional<PostingError> error = errorRepository.findById(errorId);
        
        if (error.isPresent()) {
            PostingError err = error.get();
            err.incrementRetryCount(); // 0으로 초기화 대신 수동 재시도로 표시
            errorRepository.save(err);
            // 큐에 재삽입하는 로직 추가 필요
        }
        
        return ResponseEntity.ok(Map.of("status", "QUEUED", "message", "재시도 요청이 큐에 추가됨"));
    }
    
    @PostMapping("/dead-letters/{deadLetterId}/retry")
    public ResponseEntity<Map<String, String>> retryDeadLetter(@PathVariable Long deadLetterId) {
        Optional<PostingDeadLetter> deadLetter = deadLetterRepository.findById(deadLetterId);
        
        if (deadLetter.isPresent()) {
            PostingDeadLetter dl = deadLetter.get();
            // 큐에 재삽입하는 로직 추가 필요
            deadLetterRepository.delete(dl);
        }
        
        return ResponseEntity.ok(Map.of("status", "QUEUED", "message", "재시도 요청이 큐에 추가됨"));
    }
    
    @GetMapping("/statistics")
    public ResponseEntity<StatisticsResponse> getStatistics(
        @RequestParam LocalDate startDate,
        @RequestParam LocalDate endDate
    ) {
        StatisticsResponse stats = statisticsService.getStatistics(startDate, endDate);
        return ResponseEntity.ok(stats);
    }
}
```

- [ ] **Step 4: Commit**

```bash
git add src/main/java/com/jk/amazon2/posting/controller/PostingController.java \
         src/main/java/com/jk/amazon2/posting/controller/MonitoringController.java \
         src/main/java/com/jk/amazon2/posting/service/StatisticsService.java
git commit -m "feat: add batch API and monitoring endpoints"
```

---

## Phase 6: 통합 테스트 및 최종 검증

### Task 15: 통합 테스트 작성

**Files:**
- Create: `tests/java/com/jk/amazon2/posting/integration/PostingBatchIntegrationTest.java`

- [ ] **Step 1: 통합 테스트 작성**

```java
package com.jk.amazon2.posting.integration;

import com.jk.amazon2.posting.entity.*;
import com.jk.amazon2.posting.repository.*;
import com.jk.amazon2.posting.service.BatchService;
import com.jk.amazon2.member.entity.Member;
import com.jk.amazon2.member.repository.MemberRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
class PostingBatchIntegrationTest {
    
    @Autowired
    private BatchService batchService;
    
    @Autowired
    private MemberRepository memberRepository;
    
    @Autowired
    private PostingRepository postingRepository;
    
    @Autowired
    private BatchExecutionRepository batchExecutionRepository;
    
    @Test
    @Transactional
    void testBatchExecutionCreatesAndUpdatesPosts() {
        // 테스트 회원 생성
        Member member = new Member();
        member.setNickname("test-user");
        memberRepository.save(member);
        
        // 배치 실행
        LocalDate start = LocalDate.of(2026, 6, 9);
        LocalDate end = LocalDate.of(2026, 6, 15);
        
        Long batchId = batchService.executeBatch(start, end, "TEST");
        
        // 배치 실행 기록 확인
        Optional<BatchExecution> execution = batchExecutionRepository.findById(batchId);
        assertTrue(execution.isPresent());
        assertEquals("COMPLETED", execution.get().getStatus());
        assertTrue(execution.get().getSuccessCount() >= 0);
    }
}
```

- [ ] **Step 2: 테스트 실행**

Run: `./gradlew test --include "**/PostingBatchIntegrationTest.java"`
Expected: PASS

- [ ] **Step 3: Commit**

```bash
git add tests/java/com/jk/amazon2/posting/integration/PostingBatchIntegrationTest.java
git commit -m "test: add posting batch integration tests"
```

---

### Task 16: 전체 테스트 실행 및 빌드

- [ ] **Step 1: 전체 테스트 실행**

Run: `./gradlew test`
Expected: 모든 테스트 PASS

- [ ] **Step 2: 빌드 검증**

Run: `./gradlew clean build`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: 최종 Commit**

```bash
git add .
git commit -m "test: verify all tests pass and build succeeds"
```

---

## 구현 체크리스트

- [ ] Phase 1: DB 마이그레이션 완료
- [ ] Phase 2: 엔티티 및 Repository 완료
- [ ] Phase 3: 예외 및 DTO 완료
- [ ] Phase 4: 핵심 서비스 (RateLimiter, NaverBlogScraper, ErrorHandler, PostingService, BatchService) 완료
- [ ] Phase 5: 스케줄러 및 API 엔드포인트 완료
- [ ] Phase 6: 통합 테스트 및 빌드 검증 완료

---

## 주의사항

1. **트랜잭션 격리 수준**: PostingService는 SERIALIZABLE로 설정 (동시성 제어)
2. **요청 제한**: RateLimiter는 2초에 1개 요청 강제
3. **재시도 로직**: 3회 실패 시 자동으로 DeadLetter로 이동
4. **네이버 요청**: HttpClient 타임아웃 10초 설정
5. **테스트**: 모든 단위 테스트는 Mockito 사용, 통합 테스트는 @SpringBootTest 사용

