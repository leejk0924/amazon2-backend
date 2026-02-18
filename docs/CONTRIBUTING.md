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
- `chore/` : 설정, 의존성 정리

---

## 3. GitHub 이슈 작성

### 이슈는 "무엇을" "왜" 중심으로 작성

**제목:**
- 명확하고 간결하게 (50자 이내)
- 예: `회원 API 구현` 또는 `포스팅 검색 오류`

**본문:**
```markdown
## 개요
이 기능이 필요한 이유를 한 문장으로 설명

## 목표
이 이슈로 달성하려는 목표들

## 관련 문서/링크 (필요시 작성)
필요한 참고 자료 (Swagger 경로, 설계서 등)
```

**금지사항:**
- ❌ 파일명, 함수명 등 구현 세부사항 작성 → PR에서 작성
- ❌ "어떻게" 구현할지 → 설계 단계가 아니면 불필요

---

## 4. Pull Request

### PR 제목
- 이슈 번호 포함: `#이슈번호: 간단한 설명`
- 예: `#5: 전역 예외 처리 구조 구현`

### PR 설명 (템플릿)
```markdown
## 개요
이 PR이 무엇을 구현하는지 간단히 설명

## 변경사항
- 주요 변경 사항 1
- 주요 변경 사항 2

## 테스트 방법
- 어떻게 테스트했는지
- 스크린샷/로그 필요시 첨부

## 관련 이슈
Closes #5
```

### PR 머지 시 이슈 자동 종료
```markdown
Closes #5
```

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
