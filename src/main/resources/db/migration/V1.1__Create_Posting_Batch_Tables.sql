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
