-- V1.4__Create_Monthly_Posting_Summary_Table.sql

CREATE TABLE monthly_posting_summary
(
    id             BIGINT PRIMARY KEY AUTO_INCREMENT,
    member_id      BIGINT      NOT NULL,
    summary_month  DATE        NOT NULL,
    total_count    INT         NOT NULL DEFAULT 0,
    created_at     DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at     DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_monthly_summary_member FOREIGN KEY (member_id) REFERENCES member (id),
    CONSTRAINT uk_member_month UNIQUE (member_id, summary_month),
    INDEX idx_summary_month_total (summary_month, total_count DESC)
);
