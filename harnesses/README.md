# Amazon2 Harnesses

모듈별 개발 가이드입니다. 각 도메인의 아키텍처, 개발 규칙, 테스트 방법을 설명합니다.

---

## 프로젝트 아키텍처

### 계층 구조

```
src/main/java/com/jk/amazon2/
├── [도메인]/
│   ├── controller/      REST API
│   ├── service/         비즈니스 로직 (Command/Query)
│   ├── repository/      데이터 접근
│   ├── entity/          JPA 엔티티
│   ├── dto/             Request/Response
│   └── exception/       도메인 예외
├── common/              공통 유틸, 상수
├── config/              Spring 설정
└── exception/           글로벌 예외 처리
```

### 서비스 패턴

- **CommandService**: Create, Update, Delete 담당
- **QueryService**: Read 담당
- **예외**: 각 도메인에서 `ErrorCode` 구현 → `GlobalExceptionHandler` 처리

---

## 도메인별 가이드

| 도메인 | 파일 | 설명 |
|--------|------|------|
| Member (회원) | [member/README.md](member/README.md) | 회원 조회, 등록, 수정, 삭제 |
| Category (카테고리) | [category/README.md](category/README.md) | 카테고리 관리 |
| Posting (포스팅) | [posting/README.md](posting/README.md) | 포스팅 조회, 통계 |

각 도메인의 상세 구현 가이드는 해당 README를 참조하세요.

---

## 공통 개발 규칙

### 커밋 메시지

Conventional Commits 규칙 ([docs/CONTRIBUTING.md](../docs/CONTRIBUTING.md) 참조)

```
<Type>: <subject>

<body>
```

**Type**: `Feat`, `Fix`, `Refactor`, `Test`, `Docs`, `Chore`, `Build`, `Config`, `Ci`

### 브랜치 네이밍

```
<type>/<이슈번호>-<설명>
```

예: `feature/#5-member-crud-api`

### 테스트

모든 도메인은 Testcontainers 기반 MySQL 통합 테스트 필수

```bash
# 도메인별 테스트
./gradlew test --tests "com.jk.amazon2.member.*"

# 특정 클래스만
./gradlew test --tests "com.jk.amazon2.member.controller.MemberControllerTest"
```

---

## 주요 의존성

- **Spring Boot 4.0.0**: 웹 프레임워크
- **JPA (Hibernate 6)**: ORM
- **QueryDSL 7.1**: 동적 쿼리
- **Lombok**: 보일러플레이트 제거
- **SpringDoc OpenAPI 3.0.0**: Swagger 문서화
- **Testcontainers**: 통합 테스트

---

## 새 도메인 추가

1. `src/main/java/com/jk/amazon2/[domain]/` 패키지 생성
2. 하위 패키지: `controller`, `service`, `repository`, `entity`, `dto`, `exception`
3. `harnesses/[domain]/README.md` 작성
4. 다음 순서로 구현: Entity → Repository → Service → Controller
5. 테스트 작성: `src/test/java/com/jk/amazon2/[domain]/`

---

## 참고 자료

- **요구사항**: [docs/Requirements.md](../docs/Requirements.md)
- **ERD**: [docs/ERD.md](../docs/ERD.md)
- **기여 규칙**: [docs/CONTRIBUTING.md](../docs/CONTRIBUTING.md)
- **API 명세**: http://localhost:8080/swagger-ui/index.html (로컬 실행 후)