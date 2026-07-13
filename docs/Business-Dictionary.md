# 비즈니스 용어사전

## 1. 목적
본 문서는 프로젝트 내에서 사용되는 주요 용어의 정의를 명확히하여 해석 차이를 줄이고, 설계 변경 시 기준 문서로 활용하기 위함이다.

또한, 프로젝트의 일관성을 유지하기 위해 본 문서를 지속적으로 관리·보완한다.

## 2. 용어 목록
| 도메인      | 명칭       | 영문명                 | 한글명       | 데이터 출처    | 관련 테이블                  | 정의                          | 비고                                  |
| -------- | -------- | ------------------- | --------- | --------- | ----------------------- | --------------------------- | ----------------------------------- |
| CATEGORY | 블로그 카테고리 | BlogCategory        | 블로그 카테고리  | 관리자 입력    | blog_category           | 인원이 소속되는 블로그 그룹 단위          | 1인 1카테고리 정책                         |
| CATEGORY | 카테고리 코드  | Code                 | 카테고리 코드   | 관리자 입력    | blog_category.code       | 카테고리 식별용 코드 (PK)           | 최대 10자                              |
| CATEGORY | 삭제여부     | Deleted              | 삭제여부      | 시스템 처리    | blog_category.deleted    | soft delete 여부              | 삭제 시 name에 `del_시각_` 접두어 부여 후 보관    |
| | | | | | | |
| MEMBER   | 인원       | Member              | 인원        | 관리자 입력    | member                  | 블로그 포스팅 수를 관리하는 대상 사용자      | soft delete 적용                      |
| MEMBER   | 닉네임      | Nickname            | 닉네임       | 관리자 입력    | member.nickname         | 블로그 사용자 식별용 고유 이름           | UNIQUE                              |
| MEMBER   | 삭제여부     | Deleted             | 삭제여부      | 시스템 처리    | member.deleted          | soft delete 여부                  | false면 활성 상태                        |
| MEMBER   | 생성일시     | CreatedAt           | 생성일시      | 시스템 자동 생성 | member.created_at       | 데이터 최초 생성 시 기록되는 시각         | DEFAULT CURRENT_TIMESTAMP           |
| MEMBER   | 수정일시     | UpdatedAt           | 수정일시      | 시스템 자동 갱신 | member.updated_at       | 데이터 수정 시 갱신되는 시각            | ON UPDATE CURRENT_TIMESTAMP         |
| | | | | | | |
| POSTING  | 주간 포스팅   | WeeklyPosting       | 주간 포스팅 수  | 배치 수집     | posting                 | 한 주간 요일별 포스팅 수 집계 데이터       | (member_id, week_start_date) UNIQUE |
| POSTING  | 주 시작일    | WeekStartDate       | 주 시작일     | 배치 계산     | posting.week_start_date | 해당 포스팅 집계 주의 시작 날짜 (월요일 기준) | 월요일 기준 정책                           |
| POSTING  | 월요일 포스팅  | MondayPosting       | 월요일 포스팅 수 | 배치 수집     | posting.mon             | 해당 주 월요일의 포스팅 수             | 기본값 0                               |
| POSTING  | 화요일 포스팅  | TuesdayPosting      | 화요일 포스팅 수 | 배치 수집     | posting.tue             | 해당 주 화요일의 포스팅 수             | 기본값 0                               |
| POSTING  | 수요일 포스팅  | WednesdayPosting    | 수요일 포스팅 수 | 배치 수집     | posting.wed             | 해당 주 수요일의 포스팅 수             | 기본값 0                               |
| POSTING  | 목요일 포스팅  | ThursdayPosting     | 목요일 포스팅 수 | 배치 수집     | posting.thu             | 해당 주 목요일의 포스팅 수             | 기본값 0                               |
| POSTING  | 금요일 포스팅  | FridayPosting       | 금요일 포스팅 수 | 배치 수집     | posting.fri             | 해당 주 금요일의 포스팅 수             | 기본값 0                               |
| POSTING  | 토요일 포스팅  | SaturdayPosting     | 토요일 포스팅 수 | 배치 수집     | posting.sat             | 해당 주 토요일의 포스팅 수             | 기본값 0                               |
| POSTING  | 일요일 포스팅  | SundayPosting       | 일요일 포스팅 수 | 배치 수집     | posting.sun             | 해당 주 일요일의 포스팅 수             | 기본값 0                               |
| POSTING  | 월별 집계    | MonthlyPostingSummary | 월별 포스팅 집계 | 이벤트 기반 갱신 | monthly_posting_summary | 회원별 월간 총 포스팅 수 (월요일 저장 커밋 후 비동기 upsert) | (member_id, summary_month) UNIQUE |
| POSTING  | 월별 랭킹    | MonthlyRanking       | 월별 포스팅 랭킹 | 조회 시 계산   | monthly_posting_summary | 특정 월의 회원별 포스팅 총합을 순위로 정렬한 결과 | rank/memberId/nickname/totalCount |
| | | | | | | |
| BATCH    | 배치 실행    | BatchExecution      | 배치 실행 이력  | 시스템 스케줄러/수동 실행 | batch_execution     | 포스팅 수집 배치 1회 실행의 진행 상태와 집계     | batch_type: SCHEDULED/MANUAL, status: IN_PROGRESS/COMPLETED/FAILED |
| BATCH    | 에러 로그    | PostingError        | 포스팅 수집 에러 | 배치 실행 중 발생 | posting_error       | 크롤링/저장 중 발생한 재시도 가능 오류 기록     | retry_count 3회 초과 시 Dead Letter로 이동 |
| BATCH    | Dead Letter | PostingDeadLetter | 처리 불가 오류   | 배치 실행 중 발생 | posting_dead_letter | 재시도 3회를 초과해 격리된 오류 (수동 재처리 대상) | MonitoringController에서 재시도 가능 |
| | | | | | | |
| SYSTEM   | 배치 수집 시각 | BatchCollectionTime | 배치 수행 시각  | 시스템 스케줄러  | -                       | 매주 월요일 00:00(Asia/Seoul)에 수행되는 지난주 포스팅 수집 작업 | PostingScheduler cron |
| SYSTEM   | 요청 제어    | RateLimiter          | 크롤링 요청 제어 | 시스템 구성    | -                       | 네이버 블로그 크롤링 요청 간 최소 간격을 보장하는 제어 장치 | 첫 요청은 즉시 허용 |

## 3. 정책 관련 용어
| 명칭           | 정의                               |
| ------------ |----------------------------------|
| 주간 집계        | 한 주의 포스팅 수를 그 다음주 월요일 00:00 에 집계 |
| 재시도 정책       | 크롤링 중 네트워크/HTTP 오류는 최대 3회 재시도, 초과 시 Dead Letter로 이동 (파싱 오류는 재시도 없이 즉시 실패 처리) |
| 월별 집계 갱신 정책  | 주간 포스팅 저장 트랜잭션이 커밋된 이후에만 월별 집계를 갱신 (AFTER_COMMIT 이벤트) |
