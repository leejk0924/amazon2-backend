-- V1.3__Add_Member_Name_Column.sql

ALTER TABLE member
    ADD COLUMN name VARCHAR(50) NULL AFTER nickname;
