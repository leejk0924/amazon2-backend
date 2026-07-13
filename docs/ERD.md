## ERD 다이어그램

### 핵심 도메인

회원/카테고리/포스팅 원본 데이터를 다룬다.

```mermaid
erDiagram

    BLOG_CATEGORY {
        VARCHAR(10) code PK
        VARCHAR(50) name "UK"
        VARCHAR(50) description
        BOOLEAN deleted
        DATETIME created_at
        VARCHAR(50) created_by
    }

    MEMBER {
        BIGINT id PK
        VARCHAR(10) category_code FK
        VARCHAR(50) nickname "UK"
        BOOLEAN deleted
        DATETIME created_at
        VARCHAR(50) created_by
        DATETIME updated_at
        VARCHAR(50) updated_by
    }

    POSTING {
        BIGINT id PK
        BIGINT member_id FK
        DATE week_start_date
        INT mon
        INT tue
        INT wed
        INT thu
        INT fri
        INT sat
        INT sun
        DATETIME created_at
        VARCHAR(50) created_by
    }

    BLOG_CATEGORY |o--o{ MEMBER : "category_code"
    MEMBER ||--o{ POSTING : "member_id"
```

### 배치/모니터링 도메인

`posting` 배치 파이프라인(크롤링 → 저장 → 재시도 → 집계)이 사용하는 운영성 테이블이다. `BATCH_EXECUTION`은 실행 단위(배치 1회 실행)를 기록하며 회원과는 직접 연관관계가 없다.

```mermaid
erDiagram

    MEMBER {
        BIGINT id PK
    }

    POSTING {
        BIGINT id PK
        BIGINT member_id FK
    }

    MONTHLY_POSTING_SUMMARY {
        BIGINT id PK
        BIGINT member_id FK
        DATE summary_month "UK(member_id, summary_month)"
        INT total_count
    }

    BATCH_EXECUTION {
        BIGINT id PK
        VARCHAR(20) batch_type "SCHEDULED, MANUAL"
        DATE start_date
        DATE end_date
        INT total_count
        INT success_count
        INT retry_count
        INT failed_count
        VARCHAR(20) status "IN_PROGRESS, COMPLETED, FAILED"
        DATETIME started_at
        DATETIME completed_at
    }

    POSTING_ERROR {
        BIGINT id PK
        BIGINT member_id FK
        DATE target_date
        VARCHAR(10) day_of_week
        TEXT error_message
        INT retry_count
        DATETIME created_at
    }

    POSTING_DEAD_LETTER {
        BIGINT id PK
        BIGINT member_id FK
        DATE target_date
        VARCHAR(10) day_of_week
        TEXT error_message
        DATETIME last_retry_at
        DATETIME created_at
    }

    MEMBER ||--o{ POSTING : "member_id"
    MEMBER ||--o{ MONTHLY_POSTING_SUMMARY : "member_id"
    MEMBER ||--o{ POSTING_ERROR : "member_id"
    MEMBER ||--o{ POSTING_DEAD_LETTER : "member_id"
```

- `MONTHLY_POSTING_SUMMARY`는 `POSTING` 저장 트랜잭션 커밋 후 `StatisticsUpdateEvent`를 통해 비동기로 upsert된다 (회원 x 월 단위 총 포스팅 수 집계).
- `POSTING_ERROR`는 재시도 3회를 초과하면 `POSTING_DEAD_LETTER`로 이동하고 원본 레코드는 삭제된다.

전체 파이프라인 흐름은 [../README.md](../README.md#43-포스팅-수집-배치-파이프라인)의 아키텍처 다이어그램을 참고한다.