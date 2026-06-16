# Contributing Guide

Amazon2 프로젝트에 기여하실 때 따라야 할 규칙들입니다.

---

## 1. 커밋 메시지 규칙 (Conventional Commits)

### 포맷
```
<type>: <subject>

<body>
```

### 타입 (Type)

| 타입         | 설명                          | 예시                       |
|------------|-----------------------------|--------------------------|
| `Feat`     | 새 기능 추가                     | `Feat: 회원 조회 API 구현`     |
| `Fix`      | 버그 수정                       | `Fix: 요일 계산 오류 수정`       |
| `Docs`     | 문서 작성/수정 (코드 제외)            | `Docs: API 접속 링크 추가`     |
| `Config`   | 설정 파일 변경                    | `Config: Swagger 경로 설정`  |
| `Test`     | 테스트 코드 작성/수정                | `Test: 회원 API 테스트 추가`    |
| `Chore`    | 코드와 무관한 작업 (import 정리, 의존성) | `Chore: 불필요한 import 제거`  |
| `Refactor` | 코드 구조 개선 (동작 변경 없음)         | `Refactor: 중복 로직 추출`     |
| `Build`    | 빌드 시스템, 의존성 변경              | `Build: 의존성 추가`          |
| `Ci`       | CI/CD 설정 변경                 | `Ci: Github Actions 추가 ` |

### 규칙
- 주제(subject)는 명령형으로 작성: "추가하다" (X) → "추가" (O)
- 첫 글자는 대문자로 시작
- 마침표(.) 사용 금지
- 50글자 이내로 간결하게

### 본문(Body) 작성
```
Docs: API 명세 확인 링크 README.md에 추가

- Swagger에서 생성한 swagger-ui와 api-docs 바로가기 추가
- 로컬 환경 접속 정보 명시
```

### 복잡한 변경사항 작성 예시

여러 변경사항이 있을 때는 본문(body)에 구체적으로 나열하여 변경 내용을 명확하게 표현합니다.

#### 본문에서 사용하는 변경 키워드

| 키워드 | 의미 | 사용 예시 |
|--------|------|-----------|
| `add` | 새로운 파일/기능/의존성 추가 | `add: spring-boot-testcontainers` |
| `remove` | 파일/기능/의존성 제거 | `remove: H2 database dependency` |
| `update` | 기존 항목 수정/업데이트 | `update: application-local.yml 환경별 설정 유지` |
| `refactor` | 코드 구조 개선 (동작 변경 없음) | `refactor: 의존성 그룹화 및 주석 추가` |
| `extract` | 기존 코드에서 로직/클래스 추출 | `extract: 중복 검증 로직을 Validator로 분리` |
| `move` | 코드/파일 위치 이동 | `move: 조회 메서드들을 QueryService로 이동` |
| `rename` | 이름 변경 | `rename: MemberService → MemberCommandService` |
| `docs` | 문서화 (JavaDoc, 주석 등) | `docs: MemberService 메서드 JavaDoc 추가` |
| `deprecate` | 기능 사용 중단 표시 | `deprecate: legacy API endpoint` |
| `revert` | 이전 상태로 되돌림 | `revert: 잘못된 캐시 로직 제거` |

**의존성 변경 예시:**
```
Build: Testcontainers 기반 테스트 환경 구성

- remove: H2 database dependency
- add: spring-boot-testcontainers
- add: testcontainers-mysql 2.0.2
- add: Lombok test scope dependencies
- refactor: 의존성 그룹화 및 주석 추가
```

**코드 리팩토링 예시:**
```
Refactor: 회원 서비스 계층 구조 개선

- extract: 중복 검증 로직을 Validator로 분리
- rename: MemberService → MemberCommandService
- add: MemberQueryService 신규 생성
- move: 조회 메서드들을 QueryService로 이동
```

---

## 2. 브랜치 전략

### 브랜치 네이밍
```
<type>/<이슈번호>-<설명>
```

**예시:**
```
feature/#5-global-exception-handler
fix/#12-member-search-bug
docs/#8-api-documentation
```

### 브랜치 타입
- `feature/` : 신규 기능 개발
- `fix/` : 버그 수정
- `refactor/` : 코드 리팩터링
- `docs/` : 문서 작업
- `config/` : 설정 작업

---

## 3. GitHub 이슈 작성

### 이슈는 "무엇을" "왜" 중심으로 작성

**제목:**
- 명확하고 간결하게 (50자 이내)
- 타입 접두어 추가 권장: `[Bug]`, `[Feature]`, `[Architecture]`
- 예: `[Bug] 포스팅 검색 오류`, `[Feature] 회원 API 구현`

**금지사항:**
- ❌ 파일명, 함수명 등 구현 세부사항 작성 → PR에서 작성
- ❌ "어떻게" 구현할지 상세 코드 작성 → PR에서 작성

---

### 이슈 타입별 템플릿

#### 🐛 버그 리포트
```markdown
## 개요
버그가 발생하는 상황을 한 문장으로 설명

## 예상 결과
정상이어야 할 동작

## 실제 결과
현재 발생하는 동작

## 재현 방법
버그를 재현하는 단계
1. ...
2. ...

## 영향도
이 버그가 미치는 영향 (critical, high, medium, low)

## 관련 문서/링크 (필요시)
참고할 문서나 링크
```

**예시:**
```markdown
## 개요
회원 검색 시 특수문자 입력 시 500 에러 발생

## 예상 결과
특수문자도 정상적으로 검색되거나, 유효성 검증 후 에러 메시지 표시

## 실제 결과
500 Internal Server Error 반환

## 재현 방법
1. 회원 검색 API 호출
2. name 파라미터에 `!@#$%` 입력
3. 요청 전송

## 영향도
high - 사용자가 특수문자를 검색할 수 없음
```

#### ✨ 기능 요청
```markdown
## 개요
이 기능이 필요한 이유를 한 문장으로 설명

## 목표
이 기능으로 달성하려는 목표들
- 목표 1
- 목표 2

## 사용 사례
실제 사용 시나리오 (선택사항)

## 관련 문서/링크 (필요시)
참고할 설계서, API 스펙 등
```

**예시:**
```markdown
## 개요
회원이 작성한 포스팅 목록을 조회할 수 있는 API 필요

## 목표
- 로그인한 사용자의 포스팅 목록 조회
- 페이지네이션 지원
- 최신순 정렬 지원

## 관련 문서/링크
- Swagger: http://localhost:8080/swagger-ui.html
- API 설계서: [링크]
```

#### 🏗️ 아키텍처 개선
```markdown
## 개요
현재 아키텍처의 문제점을 한 문장으로 설명

## 문제점
현재 설계의 문제들
- 문제 1
- 문제 2
- 문제 3

## 목표
개선 후 달성하려는 상태
- 목표 1
- 목표 2

## 영향도
변경이 미치는 범위와 위험도
- CRITICAL / HIGH / MEDIUM / LOW
- 영향받는 도메인 / 모듈

## 관련 문서/링크 (필요시)
아키텍처 다이어그램, 설계 문서 등
```

**예시:**
```markdown
## 개요
MonitoringController가 Service 계층을 우회하여 Repository를 직접 주입

## 문제점
- Service 계층의 비즈니스 로직 캡슐화 위반
- Controller에 데이터 접근 로직 혼재
- 트랜잭션 관리 책임 분산
- 단위 테스트 작성 불가능

## 목표
- MonitoringService 생성으로 관심사 분리
- Controller는 Service만 의존
- 향후 쿼리 최적화/캐싱 추가 시 Service에서만 수정

## 영향도
CRITICAL - 아키텍처 규칙 위반

## 관련 문서/링크
- 패키지 구조: CONTRIBUTING.md#5-패키지-구조-규칙
```

---

### 라벨 가이드

이슈 생성 시 다음 라벨을 추가해주세요:

| 라벨 | 설명 | 사용 조건 |
|------|------|---------|
| `bug` | 버그 리포트 | 기능이 정상 작동하지 않음 |
| `feature` | 새 기능 요청 | 새로운 기능 추가 필요 |
| `refactor` | 코드 리팩토링 | 기능은 그대로, 구조/성능 개선 |
| `docs` | 문서 작업 | README, 가이드, 주석 추가/수정 |
| `architecture` | 아키텍처 개선 | 시스템 구조 개선 |
| `high-priority` | 높은 우선순위 | 긴급 또는 중요한 이슈 |
| `good-first-issue` | 첫 번째 기여 | 신입 개발자 추천 |

---

## 4. Pull Request

### PR 제목
- 이슈 번호 포함: `#이슈번호: 간단한 설명`
- 예: `#5: 전역 예외 처리 구조 구현`

### PR 설명 (템플릿)
```markdown
## 개요
이 PR이 무엇을 구현하는지 간단히 설명 (1-2문장)

## 변경사항
주요 변경 사항을 구조별로 나열
- 파일명: 변경 내용
- 파일명: 변경 내용

## 테스트 방법
실제 테스트 과정과 결과
- 스텝 1: 어떻게 테스트했는지
- 스텝 2: 결과 캡처/로그 첨부

## 체크리스트
- [ ] 단위 테스트 작성/수정 완료
- [ ] CI 테스트 통과
- [ ] 코드 리뷰 요청 완료
- [ ] 문서 업데이트 (필요시)
- [ ] 기존 기능 리그레션 확인

## 관련 이슈
Closes #5
```

### PR 작성 시 주의사항

**필수:**
- ✅ 이슈 번호 포함 (`#5` 형식)
- ✅ 관련 이슈 자동 종료 문법 (`Closes #5`)
- ✅ 변경 이유 설명 (WHAT과 WHY)
- ✅ 테스트 방법 기술

**금지:**
- ❌ 파일 경로만 나열 (변경 내용도 함께)
- ❌ 구현 코드 붙여넣기 (필요시 하이라이트만)
- ❌ 테스트 없이 PR 생성
- ❌ 한 PR에 여러 이슈 담기

### PR 머지 조건
1. ✅ CI 테스트 통과
2. ✅ 최소 1명 이상 코드 리뷰 승인
3. ✅ 충돌 해결 완료
4. ✅ 관련 문서 업데이트
5. ✅ 이슈 자동 종료 문법 포함 (`Closes #XX`)

---

## 5. 패키지 구조 규칙

### 패키지 분류

| 패키지          | 역할                 |
|--------------|--------------------|
| `controller` | REST API 엔드포인트     |
| `service`    | 비즈니스 로직            |
| `repository` | DB 접근 계층           |
| `entity`     | 엔티티 (도메인 모델)       |
| `dto`        | DTO 클래스            |
| `exception`  | 글로벌 예외 클래스         |
| `config`     | 설정 클래스             |
| `common`     | 공통 클래스             |

### 규칙
- 도메인별 예외 처리: 각 도메인(member, category, posting)에서 `ErrorCode` 인터페이스 구현
- `common` 패키지는 도메인 무관 유틸/공통 구조만 포함
- `exception` 패키지는 예외 처리 전담

---

## 6. 코드 스타일

### 네이밍 규칙
- 변수/함수명: camelCase (영어)
- 클래스명: PascalCase (영어)
- 상수: UPPER_SNAKE_CASE (영어)
- 주석: 한국어

### 예시
```java
// 카테고리 코드별 조회
public CategoryResponse getCategoryByCode(String code) {
    final String CACHE_PREFIX = "CATEGORY_";
    // 구현...
}
```
---

## 참고
이 문서는 프로젝트 진행에 따라 지속적으로 업데이트됩니다.
규칙에 대한 제안사항은 이슈로 등록해주세요.
