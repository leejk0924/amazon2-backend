# Amazon2 - Blog Management Service
네이버 블로그 모임을 위한 관리 및 조회 서비스입니다.

---
## 1.프로젝트 개요
기존에 프로토타입으로 운영했으나, 성능 이슈 및 설계 미스로 인해 프로젝트를 리뉴얼하고자 한다.
아래는 이전에 경험했던 한계점들이다.
- DB 없이 파일 시스템으로 관리 인원을 저장하여 변경이 어러움
- 관리 인원 수가 점차 증가하여 응답의 지연이 점차 늘어남
- 라즈베리파이 단일 서버 운영으로 인해 불안정한 서비스

본 프로젝트는 위 문제를 해결하기 위해 재설계된 프로젝트이다.

---
## 2. 목표
- 관리 인원의 포스팅 수를 확인 응답 속도 개선
- 인원 관리 편의성 개선
- 장애 발생 시 복구 가능한 구조 설계 및 모니터링 시스템 구축
- AWS 이전을 고려한 확장 가능한 구조

---
## 3. 기술 스택
### Backend
- SpringBoot
- JPA

### Database
- MySQL

### Frontend
현재 :
- HTML / CSS / JS
- Nginx 통한 정적 파일 제공 (Backend의 REST API 기능에 집중을 위해)

향후 :
- AWS 이전하면서 React 기반 SPA로 전환할 예정
- CloudFront + S3를 통한 정적 배포 예정

### Infra
- Docker / Docker Compose
- Nginx

## 4. 시스템 아키텍처
(추후 다이어그램으로 추가 예정)

## 5. 문서
- [요구사항 명세서](/docs/Requirements.md)
- [ERD 다이어그램](/docs/ERD.md)

## 6. API 명세서 (Swagger)
[API 명세서](http://localhost:8080/swagger-ui/index.html)
[API 문서](http://localhost:8080/v3/api-docs)

## 7. 사전 요구사항
- Java 21
- MySQL 8.x

## 8. 로컬 환경 세팅 절차

### 8.1 프로파일 기반 설정
```
application.yml              → 공통 설정 (운영 기본값)
application-local.yml        → 로컬 개발 환경
application-local.yml.example → 로컬 설정 예시 (커밋 대상)
application-prod.yml         → 프로덕션 환경 (환경 변수 사용)
application-test.yml         → 테스트 환경 (src/test/resources/)
```

### 8.2 로컬환경 세팅
```markdown
1. `application-local.yml.example` 을 복사하여 `application-local.yml` 생성
2. DB 접속 정보 입력
3. `spring.profiles.active=local` 설정 후 실행
```

### 8.3 실행 방법
```shell
# Gradle 빌드 및 실행
./gradlew bootRun --args='--spring.profiles.active=local'
```

### 8.4 프로덕션 환경 세팅

#### 환경 변수 설정
프로덕션 환경에서는 민감한 정보를 환경 변수로 주입합니다.

**필수 환경 변수:**
```shell
DB_URL=jdbc:mysql://<host>:<port>/amazon?characterEncoding=UTF-8&serverTimezone=UTC
DB_USERNAME=<username>
DB_PASSWORD=<password>
```

#### 실행 방법
```shell
# 환경 변수 설정 후 실행
export DB_URL="jdbc:mysql://prod-db:3306/amazon?characterEncoding=UTF-8&serverTimezone=UTC"
export DB_USERNAME="prod_user"
export DB_PASSWORD="prod_password"

# 프로덕션 프로파일로 실행
./gradlew bootRun --args='--spring.profiles.active=prod'
```

또는 Docker/Docker Compose 사용 시:
```yaml
# docker-compose.yml
services:
  app:
    environment:
      - SPRING_PROFILES_ACTIVE=prod
      - DB_URL=jdbc:mysql://db:3306/amazon?characterEncoding=UTF-8&serverTimezone=UTC
      - DB_USERNAME=prod_user
      - DB_PASSWORD=prod_password
```

### 8.5 테스트 환경

테스트 환경은 **Testcontainers**를 사용하여 실제 MySQL 환경과 동일하게 테스트합니다.

#### 특징
- Docker 기반 MySQL 컨테이너 자동 생성/삭제
- `db/schema.sql` 초기화 스크립트 자동 실행
- 로컬 DB 설정 불필요

#### 실행 방법
```shell
# 테스트 실행 (Testcontainers 자동 시작)
./gradlew test

# Docker가 실행 중이어야 합니다
docker ps
```

**주의사항:**
- Docker Desktop 또는 Docker Engine이 실행 중이어야 합니다
- 첫 실행 시 MySQL 이미지 다운로드로 시간이 소요될 수 있습니다