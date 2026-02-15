CREATE TABLE blog_category
(
    code        VARCHAR(10) PRIMARY KEY,
    name        VARCHAR(50) NOT NULL UNIQUE,
    description VARCHAR(50)
);

CREATE TABLE member
(
    id            BIGINT PRIMARY KEY AUTO_INCREMENT,
    nickname      VARCHAR(50) NOT NULL UNIQUE,
    category_code VARCHAR(10) NULL,
    created_at    DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by    VARCHAR(50) NOT NULL,
    updated_at    DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    updated_by    VARCHAR(50) NOT NULL,
    deleted_at    DATETIME             DEFAULT NULL,
    CONSTRAINT fk_member_category FOREIGN KEY (category_code) REFERENCES blog_category (code),
    INDEX idx_member_deleted (deleted_at)
);

-- uk_user_week : 데이터 무결성을 위해
CREATE TABLE posting
(
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    member_id       BIGINT      NOT NULL,
    mon             INT         NOT NULL DEFAULT 0,
    tue             INT         NOT NULL DEFAULT 0,
    wed             INT         NOT NULL DEFAULT 0,
    thu             INT         NOT NULL DEFAULT 0,
    fri             INT         NOT NULL DEFAULT 0,
    sat             INT         NOT NULL DEFAULT 0,
    sun             INT         NOT NULL DEFAULT 0,
    week_start_date DATE        NOT NULL,
    created_at      DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by      VARCHAR(50) NOT NULL,
    CONSTRAINT fk_posting_member FOREIGN KEY (member_id) REFERENCES member (id),
    CONSTRAINT uk_user_week UNIQUE (member_id, week_start_date),
    INDEX idx_week (week_start_date)
);

