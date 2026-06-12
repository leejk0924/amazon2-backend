# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

Amazon2 - 네이버 블로그 모임 관리 서비스 | Java 21, Spring Boot 4.0.0, MySQL 8.x

---

## 빠른 시작

```bash
# 로컬 환경 설정
cp src/main/resources/application-local.yml.example src/main/resources/application-local.yml

# 실행
./gradlew bootRun --args='--spring.profiles.active=local'

# 테스트 (Docker 필수)
./gradlew test

# 빌드
./gradlew clean build
```

---

## 핵심 명령어

| 명령 | 설명 |
|------|------|
| `./gradlew bootRun --args='--spring.profiles.active=local'` | 로컬 개발 모드 |
| `./gradlew test` | 전체 테스트 |
| `./gradlew test --tests "com.jk.amazon2.member.*"` | 도메인별 테스트 |
| `./gradlew clean bootJar` | JAR 빌드 |
| `docker compose up -d` | Docker 실행 |

---

## 프로젝트 구조

```
src/main/java/com/jk/amazon2/
├── member/              # 회원 모듈
│   ├── controller/      # REST API
│   ├── service/         # 비즈니스 로직
│   ├── repository/      # 데이터 접근 (JPA)
│   ├── entity/          # JPA 엔티티
│   ├── dto/             # DTO (Request/Response)
│   └── exception/       # 도메인 예외
├── category/            # 카테고리 모듈
│   ├── controller/      # REST API
│   ├── service/         # 비즈니스 로직
│   ├── repository/      # 데이터 접근 (JPA)
│   ├── entity/          # JPA 엔티티
│   ├── dto/             # DTO (Request/Response)
│   └── exception/       # 도메인 예외
├── posting/             # 포스팅 모듈
│   ├── controller/      # REST API
│   ├── service/         # 비즈니스 로직
│   ├── repository/      # 데이터 접근 (JPA)
│   ├── entity/          # JPA 엔티티
│   ├── dto/             # DTO (Request/Response)
│   └── exception/       # 도메인 예외
├── common/              # 공통 코드
│   ├── exception/       # 글로벌 예외 처리 (ErrorCode, GlobalExceptionHandler, ErrorResponse)
│   ├── constant/        # 공통 상수
│   ├── dto/             # 공통 DTO
│   └── utils/           # 유틸리티 클래스
└── config/              # Spring 설정
```

### 개발 방식

- 모든 새로운 기능은 해당 도메인 폴더 아래에서 개발합니다
- 도메인별로 완전히 독립적인 모듈 구조입니다
- 새 도메인 추가 시 동일한 구조로 폴더를 생성합니다
- 각 도메인의 상세 가이드는 `harnesses/[domain]/README.md` 참조

---

## 개발 가이드

- **전체 아키텍처**: [harnesses/README.md](harnesses/README.md)
- **Member (회원)**: [harnesses/member/README.md](harnesses/member/README.md)
- **Category (카테고리)**: [harnesses/category/README.md](harnesses/category/README.md)
- **Posting (포스팅)**: [harnesses/posting/README.md](harnesses/posting/README.md)
- **기여 규칙**: [docs/CONTRIBUTING.md](docs/CONTRIBUTING.md)

---

## 환경 설정

프로필별 설정 파일:
- `application.yml` - 공통
- `application-local.yml` - 로컬 (DB 접속)
- `application-test.yml` - 테스트 (Testcontainers)
- `application-prod.yml` - 운영 (환경 변수)

---