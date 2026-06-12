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
├── member/      → harnesses/member 참조
├── category/    → harnesses/category 참조
├── posting/     → harnesses/posting 참조
├── common/      공통 유틸, 상수
├── config/      Spring 설정
└── exception/   글로벌 예외 처리
```

**각 도메인의 개발 가이드는 `harnesses/` 참조**

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