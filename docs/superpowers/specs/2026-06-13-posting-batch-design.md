# 포스팅 배치 시스템 설계 (Posting Batch System Design)

**작성일**: 2026-06-13  
**작성자**: Claude Code  
**상태**: 설계 완료, 구현 준비 중

---

## 1. 개요

블로그 사용자들의 네이버 블로그 포스팅 통계를 자동으로 수집하고 데이터베이스에 저장하는 배치 시스템입니다.

**목표:**
- 매주 월요일 자정에 지난주(월~일) 포스팅 데이터 자동 수집
- 관리자가 특정 날짜 범위의 데이터를 수동으로 수집 가능
- 안정적인 에러 처리 및 재시도 메커니즘
- 모니터링 및 통계 조회 기능 제공

**핵심 요구사항:**
- 요청 속도 제한: 2초에 1개 요청 (네이버 서버 과부하 방지)
- 동시성 제어: 자동 배치 + 수동 API 동시 실행 처리
- 재시도 로직: 3회 실패 시 DeadLetter 처리
- 모니터링: 배치 실행 이력, 에러 로그, 통계

---

## 2. 아키텍처 개요

```
┌─────────────────────────────────────────┐
│   Spring @Scheduled (월요일 자정)        │
│   + Manual API (관리자 요청)             │
└────────────┬────────────────────────────┘
             │
             ▼
┌─────────────────────────────────────────┐
│   BatchService (배치 엔진)                │
│   - 날짜 계산                             │
│   - 사용자 목록 조회                      │
│   - 요청 큐 생성                          │
└────────────┬────────────────────────────┘
             │
             ▼
┌─────────────────────────────────────────┐
│   RequestQueue + RateLimiter             │
│   - 초당 2개 요청 제한                   │
│   - BlockingQueue로 순차 처리            │
│   - 3회 실패 시 DeadLetter로 이동       │
└────────────┬────────────────────────────┘
             │
             ▼
┌─────────────────────────────────────────┐
│   NaverBlogScraper                      │
│   - HttpClient로 네이버 요청             │
│   - Jsoup으로 파싱                       │
│   - 에러 로깅                             │
└────────────┬────────────────────────────┘
             │
             ▼
┌─────────────────────────────────────────┐
│   Database                               │
│   - posting: 주간 데이터                 │
│   - posting_error: 에러 로그             │
│   - posting_dead_letter: 최종 실패      │
│   - batch_execution: 배치 실행 이력      │
└─────────────────────────────────────────┘
```

**주요 컴포넌트:**

- **BatchScheduler**: @Scheduled로 매주 월요일 자정 실행
- **BatchService**: 배치 실행 로직 (날짜 계산, 회원 조회, 요청 생성)
- **RequestQueue**: BlockingQueue 기반 요청 큐
- **RateLimiter**: 초당 2개 요청 제한
- **NaverBlogScraper**: 네이버 HTTP 요청 및 파싱
- **PostingService**: DB 저장 (트랜잭션 + 동시성 제어)
- **ErrorHandler**: 에러 로깅 및 재시도 관리
- **MonitoringController**: 대시보드/통계 API 제공

---

## 3. 데이터베이스 스키마

### 3.1 posting (기존 테이블 유지)

```sql
CREATE TABLE posting (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    member_id       BIGINT      NOT NULL,
    week_start_date DATE        NOT NULL,
    mon             INT         NOT NULL DEFAULT 0,
    tue             INT         NOT NULL DEFAULT 0,
    wed             INT         NOT NULL DEFAULT 0,
    thu             INT         NOT NULL DEFAULT 0,
    fri             INT         NOT NULL DEFAULT 0,
    sat             INT         NOT NULL DEFAULT 0,
    sun             INT         NOT NULL DEFAULT 0,
    created_at      DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by      VARCHAR(50) NOT NULL,
    CONSTRAINT fk_posting_member FOREIGN KEY (member_id) REFERENCES member (id),
    CONSTRAINT uk_user_week UNIQUE (member_id, week_start_date),
    INDEX idx_week (week_start_date)
);
```

### 3.2 posting_error (신규 - 에러 로깅)

```sql
CREATE TABLE posting_error (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    member_id       BIGINT      NOT NULL,
    target_date     DATE        NOT NULL,  -- 실패한 날짜
    day_of_week     VARCHAR(10) NOT NULL,  -- mon, tue, ... sun
    error_message   TEXT,
    retry_count     INT         NOT NULL DEFAULT 1,
    created_at      DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_error_member FOREIGN KEY (member_id) REFERENCES member (id),
    INDEX idx_retry_count (retry_count),
    INDEX idx_created_at (created_at)
);
```

### 3.3 posting_dead_letter (신규 - 최종 실패 항목)

```sql
CREATE TABLE posting_dead_letter (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    member_id       BIGINT      NOT NULL,
    target_date     DATE        NOT NULL,
    day_of_week     VARCHAR(10) NOT NULL,
    error_message   TEXT,
    last_retry_at   DATETIME,
    created_at      DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_dead_letter_member FOREIGN KEY (member_id) REFERENCES member (id),
    INDEX idx_created_at (created_at)
);
```

### 3.4 batch_execution (신규 - 배치 실행 이력)

```sql
CREATE TABLE batch_execution (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    batch_type      VARCHAR(20) NOT NULL,  -- SCHEDULED, MANUAL
    start_date      DATE        NOT NULL,
    end_date        DATE        NOT NULL,
    total_count     INT         NOT NULL DEFAULT 0,
    success_count   INT         NOT NULL DEFAULT 0,
    retry_count     INT         NOT NULL DEFAULT 0,
    failed_count    INT         NOT NULL DEFAULT 0,
    status          VARCHAR(20) NOT NULL,  -- IN_PROGRESS, COMPLETED, FAILED
    started_at      DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    completed_at    DATETIME,
    INDEX idx_started_at (started_at),
    INDEX idx_status (status)
);
```

---

## 4. 배치 처리 흐름

### 4.1 트리거

**자동 배치:**
- 매주 월요일 00:00 (자정) 실행
- 지난주 월요일 ~ 일요일 데이터 수집

**수동 API:**
- 관리자가 `POST /api/postings/batch` 호출
- `startDate`, `endDate` 파라미터로 범위 지정

### 4.2 배치 실행 프로세스

```
1. 트리거 (자동/수동)
   ├─ @Scheduled: 매주 월요일 00:00
   └─ /api/postings/batch (관리자 요청)

2. BatchService.executeBatch()
   ├─ batch_execution 레코드 생성 (status=IN_PROGRESS)
   ├─ 수집 대상 계산
   │  ├─ 모든 member 조회
   │  ├─ member별로 (start_date ~ end_date) 범위 분해
   │  └─ 요청 객체 생성 (memberId, targetDate, dayOfWeek)
   └─ 요청 큐에 삽입

3. RequestQueue + RateLimiter
   ├─ BlockingQueue에서 순차 대기
   ├─ RateLimiter: 초당 2개 요청 처리
   └─ 각 요청을 NaverBlogScraper로 전달

4. NaverBlogScraper.scrape()
   ├─ HttpClient로 네이버 요청
   ├─ Jsoup으로 HTML 파싱
   ├─ 포스팅 수 추출
   └─ 결과 반환 (또는 예외 발생)

5. 결과 처리
   ├─ 성공 (0개 포함)
   │  └─ PostingService.savePosting() 호출
   │     ├─ @Transactional(isolation = SERIALIZABLE)
   │     ├─ 현재 데이터 조회 (FOR UPDATE)
   │     ├─ 데이터 비교
   │     │  ├─ 없음 → INSERT
   │     │  └─ 있음 → UPDATE (덮어쓰기)
   │     └─ batch_execution 카운트 증가 (success_count++)
   │
   └─ 실패 (예외 발생)
      ├─ ErrorHandler.handleError()
      ├─ posting_error에 로깅
      ├─ retry_count 확인
      │  ├─ < 3: 큐 맨 뒤 재삽입
      │  └─ >= 3: posting_dead_letter 이동
      └─ batch_execution 카운트 증가

6. 배치 완료
   ├─ batch_execution 업데이트
   │  ├─ status = COMPLETED
   │  ├─ completed_at = 현재 시간
   │  └─ 최종 집계 (success, retry, failed)
   └─ 로그 출력
```

### 4.3 동시성 제어

**문제 상황:**
- 월요일 자동 배치 실행 중
- 동시에 관리자가 수동 API로 같은 기간 데이터 수집
- 같은 `member_id, week_start_date`에 동시 접근

**해결책:**
```java
@Transactional(isolation = IsolationLevel.SERIALIZABLE)
public void savePosting(Long memberId, LocalDate weekStartDate, 
                        int mon, int tue, ...) {
    // FOR UPDATE로 행 락
    Posting existing = postingRepository.findByMemberIdAndWeekStartDate(
        memberId, weekStartDate, LockModeType.PESSIMISTIC_WRITE);
    
    if (existing == null) {
        postingRepository.save(new Posting(...));
    } else {
        existing.update(mon, tue, ...);
    }
}
```

**효과:**
- SERIALIZABLE 격리 수준: 트랜잭션 순차 처리
- FOR UPDATE: 행 락으로 동시 수정 방지
- 결과: 일관된 데이터 저장

---

## 5. API 엔드포인트

### 5.1 수동 데이터 수집 API

```
POST /api/postings/batch

요청:
{
  "startDate": "2026-06-01",
  "endDate": "2026-06-07"
}

응답 (202 Accepted):
{
  "batchExecutionId": 123,
  "status": "IN_PROGRESS",
  "totalCount": 45,
  "message": "배치 작업 시작됨"
}
```

### 5.2 모니터링 대시보드 API

```
GET /api/postings/batch/status

응답:
{
  "lastExecution": {
    "batchExecutionId": 122,
    "batchType": "SCHEDULED",
    "startDate": "2026-06-09",
    "endDate": "2026-06-15",
    "status": "COMPLETED",
    "totalCount": 50,
    "successCount": 48,
    "retryCount": 2,
    "failedCount": 0,
    "completedAt": "2026-06-09T00:15:30"
  },
  "currentStats": {
    "totalDeadLetters": 3,
    "totalErrors": 5,
    "successRate": 96.0
  }
}
```

### 5.3 에러 조회 API

```
GET /api/postings/errors?page=0&size=20

응답:
{
  "content": [
    {
      "id": 1,
      "memberId": 5,
      "memberNickname": "blog-user1",
      "targetDate": "2026-06-10",
      "dayOfWeek": "tue",
      "errorMessage": "Connection timeout",
      "retryCount": 1,
      "createdAt": "2026-06-09T00:05:30"
    }
  ],
  "totalElements": 5,
  "totalPages": 1,
  "currentPage": 0
}
```

**DeadLetter 조회:**
```
GET /api/postings/dead-letters?page=0&size=20
(동일한 응답 구조)
```

### 5.4 재시도 관리 API

```
POST /api/postings/errors/{errorId}/retry

응답:
{
  "status": "QUEUED",
  "message": "재시도 요청이 큐에 추가됨"
}

또는

POST /api/postings/dead-letters/{deadLetterId}/retry

(로직 동일)
```

### 5.5 통계 조회 API

```
GET /api/postings/statistics?startDate=2026-06-01&endDate=2026-06-30

응답:
{
  "startDate": "2026-06-01",
  "endDate": "2026-06-30",
  "totalPostings": 1250,
  "users": [
    {
      "memberId": 1,
      "nickname": "user1",
      "totalPostings": 120,
      "byDayOfWeek": {
        "mon": 20,
        "tue": 18,
        "wed": 22,
        "thu": 19,
        "fri": 15,
        "sat": 8,
        "sun": 18
      }
    },
    {
      "memberId": 2,
      "nickname": "user2",
      "totalPostings": 95,
      "byDayOfWeek": {
        "mon": 15,
        "tue": 14,
        "wed": 18,
        "thu": 16,
        "fri": 12,
        "sat": 6,
        "sun": 14
      }
    }
  ]
}
```

---

## 6. 에러 처리 및 재시도

### 6.1 예외 분류

```
예외 타입별 처리

├─ IOException (네트워크 오류, 타임아웃)
│  └─ 재시도 대상: 임시 오류 가능성 높음
│
├─ HttpStatusException (429, 503 등)
│  └─ 재시도 대상: 서버 일시 문제
│
├─ ParsingException (HTML 파싱 실패)
│  └─ DeadLetter 즉시 이동: 로직 오류, 재시도 무의미
│
└─ UnexpectedException (기타)
   └─ 재시도 대상: 불명확한 오류
```

### 6.2 재시도 흐름

```
요청 실패 (예외 발생)
│
├─ ErrorHandler.handleError()
│  ├─ 예외 타입 판정
│  ├─ posting_error 테이블에 로깅
│  │  (memberId, targetDate, dayOfWeek, errorMessage, retryCount=1)
│  │
│  └─ retryCount 확인
│     ├─ retryCount < 3
│     │  └─ 동일 요청을 BlockingQueue 맨 뒤에 재삽입
│     │     (retry_count +1)
│     │     (다른 요청들 처리 후 재시도되므로 자동 간격 발생)
│     │
│     └─ retryCount >= 3
│        └─ posting_dead_letter로 이동
│           (더 이상 자동 재시도 안 함, 관리자 개입 필요)
```

### 6.3 관리자 수동 재시도

```
POST /api/postings/errors/{errorId}/retry
또는
POST /api/postings/dead-letters/{deadLetterId}/retry

처리:
├─ retry_count를 0으로 초기화
├─ posting_error / posting_dead_letter에서 삭제
└─ 요청을 큐에 재삽입하여 다시 처리
```

---

## 7. 모니터링 및 통계

### 7.1 배치 실행 모니터링

```
batch_execution 테이블 기반 추적

배치 시작
├─ batch_execution 생성
│  └─ id, batch_type, start_date, end_date, status=IN_PROGRESS, started_at
│
배치 진행 중
├─ 각 요청 처리 후 실시간 업데이트
│  ├─ success_count (성공)
│  ├─ retry_count (재시도 대기 중)
│  └─ failed_count (DeadLetter 이동)
│
배치 완료
└─ batch_execution 업데이트
   ├─ status=COMPLETED
   ├─ completed_at
   └─ 최종 집계
```

### 7.2 대시보드 지표

```
1. 최근 배치 실행 상태
   ├─ 성공률 = (success_count / total_count) * 100
   ├─ 재시도율 = (retry_count / total_count) * 100
   └─ 실패율 = (failed_count / total_count) * 100

2. 에러 현황
   ├─ posting_error: 재시도 대기 중인 항목 수
   └─ posting_dead_letter: 최종 실패 항목 수

3. 포스팅 통계
   ├─ 전체 포스팅 수
   ├─ 요일별 분포
   └─ 사용자별 상세 통계 (요일별 + 주간 합계)
```

### 7.3 로깅

```
배치 시작:
  [BATCH] Posting batch started - type=SCHEDULED, period=2026-06-09~2026-06-15

처리 중:
  [BATCH] Processing member=5, target_date=2026-06-10, day=tue

성공:
  [BATCH] Saved posting - member=5, week_start_date=2026-06-09, 
          mon=15, tue=18, ... sun=14

실패 (재시도):
  [BATCH] Failed member=5, target_date=2026-06-11, retry_count=1, 
          error=Connection timeout

재시도 중:
  [BATCH] Requeued - member=5, target_date=2026-06-11, retry_count=2

최종 실패 (DeadLetter):
  [BATCH] Moved to dead letter - member=5, target_date=2026-06-12, 
          error=Parsing failed

배치 완료:
  [BATCH] Posting batch completed - total=50, success=48, retry=2, 
          failed=0, duration=5m 30s
```

---

## 8. 구현 범위

### 8.1 Phase 1 (필수)

- [ ] Database 스키마 생성 (posting_error, posting_dead_letter, batch_execution)
- [ ] BatchScheduler (자동 배치)
- [ ] BatchService (배치 로직)
- [ ] RequestQueue + RateLimiter (초당 2개)
- [ ] NaverBlogScraper (네이버 스크래핑)
- [ ] PostingService (트랜잭션 + 동시성 제어)
- [ ] ErrorHandler (에러 처리 + 재시도)
- [ ] PostingController (수동 API)
- [ ] MonitoringController (대시보드 + 통계)

### 8.2 Phase 2 (선택, 향후)

- [ ] 알림 기능 (실패 시 관리자 알림)
- [ ] 성능 최적화 (캐싱, 쿼리 최적화)
- [ ] 분산 처리 (RabbitMQ/Kafka 통합)

---

## 9. 설계 결정 사항

| 항목 | 선택 | 이유 |
|------|------|------|
| 스케줄러 | Spring @Scheduled | 외부 의존성 최소화, 단순한 크론식으로 충분 |
| 요청 처리 | ExecutorService + BlockingQueue | 가볍고, 속도 제한 구현이 간단함 |
| 동시성 제어 | SERIALIZABLE + FOR UPDATE | 안전한 데이터 일관성 보장 |
| 재시도 전략 | 큐 뒤 재삽입 + 3회 제한 | 자동 간격 발생, 단순하고 효과적 |
| 데이터 처리 | 덮어쓰기 | 네이버의 최신 데이터 반영 가능 |

---

## 10. 위험 요소 및 완화 방안

| 위험 | 영향 | 완화 방안 |
|------|------|----------|
| 네이버 IP 차단 | 배치 중단 | 속도 제한(초당 2개), 에러 모니터링 |
| DB 잠금 | 느린 응답 | SERIALIZABLE 적절히 사용, 타임아웃 설정 |
| 메모리 누수 | 서버 다운 | BlockingQueue 크기 제한, 모니터링 |
| 네트워크 장애 | 데이터 손실 | 재시도 로직, DeadLetter 보관 |

---

## 11. 향후 고려사항

1. **알림 기능**: 배치 실패/DeadLetter 발생 시 관리자 이메일/Slack 알림
2. **성능 최적화**: 배치 처리 시간 단축 (현재 예상: 50개 회원 × 7일 ÷ (2/초) ≈ 17.5분)
3. **분산 처리**: Message Queue 통합으로 여러 인스턴스에서 병렬 처리
4. **데이터 검증**: 수집 데이터와 실제 블로그 데이터 비교 검증
5. **백업**: 변경 전 데이터 백업 기능

---

## 12. 체크리스트

구현 전 확인사항:

- [ ] DB 마이그레이션 스크립트 준비
- [ ] 환경 변수 설정 (네이버 요청 타임아웃 등)
- [ ] 테스트 데이터 준비
- [ ] 로깅 레벨 설정 (DEBUG/INFO/WARN/ERROR)
- [ ] 모니터링 메트릭 정의 (성공률, 실패율 등)

---

