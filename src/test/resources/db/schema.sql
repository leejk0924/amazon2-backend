-- Test Database Schema
-- 초기 스키마 (V1.0)

CREATE TABLE IF NOT EXISTS blog_category
(
    code            VARCHAR(10) PRIMARY KEY,
    name            VARCHAR(50) NOT NULL UNIQUE,
    description     VARCHAR(50),
    deleted         BOOLEAN NOT NULL DEFAULT false,
    created_at      DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by      VARCHAR(50) NOT NULL DEFAULT 'admin'
);

CREATE TABLE IF NOT EXISTS member
(
    id            BIGINT PRIMARY KEY AUTO_INCREMENT,
    category_code VARCHAR(10) NULL,
    nickname      VARCHAR(50) NOT NULL UNIQUE,
    deleted       BOOLEAN     NOT NULL DEFAULT false,
    created_at    DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by    VARCHAR(50) NOT NULL,
    updated_at    DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    updated_by    VARCHAR(50) NOT NULL,
    CONSTRAINT fk_member_category FOREIGN KEY (category_code) REFERENCES blog_category (code),
    INDEX idx_member_deleted (deleted),
    INDEX idx_nickname (nickname)
);

CREATE TABLE IF NOT EXISTS posting
(
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

-- 배치 관련 스키마 (V1.1)

CREATE TABLE IF NOT EXISTS posting_error
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

CREATE TABLE IF NOT EXISTS posting_dead_letter
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

CREATE TABLE IF NOT EXISTS batch_execution
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
