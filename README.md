# Amazon2 - Blog Management Service
네이버 블로그 모임을 위한 관리 및 조회 서비스입니다.

---
## 1.프로젝트 개요
기존 프로토타입의 성능 이슈와 설계 한계를 극복하기 위해 진행되는 리뉴얼 프로젝트입니다.
- **데이터 관리**: 파일 시스템 기반 관리에서 RDBMS(MySQL)으로 전환
- **성능 개선**: 관리 인원 증가에 따른 응답 지연 해결 및 속도 최적화
- **안정성**: 라즈베리파이 단일 서버에서 클라우드 기반 확장 가능 구조로 재설계

---
## 2. 목표
- 포스팅 수 확인 및 인원 관리 응답 속도 대폭 개선
- 장애 복구 시나리오 확보 및 실시간 모니터링 체계 구축
- AWS 이전을 고려한 컨테이너 기반 인프라 확장성 확보

---
## 3. 기술 스택
### Backend / Database
- Java 21, SpringBoot 3.x
- JPA (Hibernate 6), MySQL 8.x

### Frontend
- 현재: HTML/CSS/JS (Nginx 정적 제공)
- 향후: React 기반 SPA 전환 및 AWS CloudFront/S3 배포 예정

### Infra
- Docker, Docker Compose, Nginx

## 4. 시스템 아키텍처
(추후 다이어그램으로 추가 예정)

## 5. 문서 및 API 명세
- [요구사항 명세서](/docs/Requirements.md)
- [ERD 다이어그램](/docs/ERD.md)
- [API 명세서](http://localhost:8080/swagger-ui/index.html)
- [API 문서](http://localhost:8080/v3/api-docs)

## 6. 로컬(Local) 환경 세팅 절차
### 6.1 프로파일 전략
애플리케이션은 실행 환경에 따라 설정을 분리하여 관리한다.
- `application.yml`: 공통 기본 설정
- `application-local.yml`: 로컬 개발 전용 (DB 접속 등)
- `application-test.yml`: 통합 테스트용 (Testcontainers 활용)
- `application-prod.yml`: 운영 환경 (환경 변수 주입 방식)

### 6.2 로컬 개발 환경 실행
1. `application-local.yml.example`을 복사하여 `application-local.yml`을 생성한다.
2. 로컬 DB 접속 정보를 입력한다.
3. 아래 명령어를 실행한다.

```shell
./gradlew bootRun --args='--spring.profiles.active=local'
```

## 7. 운영(Prod) 환경 세팅 절차
### 7.1 운영 환경 실행
1. `application-prod.yml`에 아래와 같이 DB 접속 정보를 등록한다.
```text
// 예시
DB_URL=jdbc:mysql://<host>:<port>/amazon?characterEncoding=UTF-8&serverTimezone=UTC
DB_USERNAME=<username>
DB_PASSWORD=<password>
```
2. 아래 명령어를 실행한다.
```shell
./gradlew bootRun --args='--spring.profiles.active=prod'
```

### 7.2 Docker / Docker Compose 실행 시
1. 아래와 같이 DB 접속 정보를 추가한다.
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
2. 애플리케이션 빌드 
```shell
./gradlew clean bootJar
```
3. 컨테이너 실행
```shell
docker compose up -d
```
4. 실행 상태 및 로그 확인
```shell
# 전체 서비스 로그 확인
docker compose logs -f

# 특정 서비스(app) 로그만 확인
docker compose logs -f app
```
5. 컨테이너 중지 및 제거
```shell
docker compose down
```

## 8. 테스트 환경
### 8.1 Testcontainers 기반의 테스트 환경
Testcontainers를 사용하여 실제 MySQL 환경에서 독립적인 테스트를 수행한다.
- `Docker` 기반 컨테이너 자동 생명주기 관리
- `db/schema.sql`을 통한 스키마 자동 초기화
- 실행: `./gradlew test` (Docker 실행 필수)

**주의사항:**
- Docker Desktop 또는 Docker Engine이 실행 중이어야 합니다.
- 첫 실행 시 MySQL 이미지 다운로드로 시간이 소요될 수 있습니다.

### 8.2 SQL 로깅 및 가시성
데이터 정합성 검증이 중요한 Local 및 Test 프로파일에서만 상세 로깅이 활성화한다. 
운영 환경(Prod)은 성능을 위해 해당 설정이 제외한다.

- **SQL 포맷팅 & 하이라이트**: ANSI 컬러를 적용한 SQL 키워드 강조로 가독성을 극대화했습니다.
- **파라미터 바인딩 확인**: ?에 주입되는 실제 값을 TRACE 레벨 로그로 출력합니다. (JPA 및 JdbcTemplate 모두 적용)
- **실행 출처 표시**: 하이버네이트 주석을 통해 쿼리를 발생시킨 소스 코드를 추적할 수 있습니다.
