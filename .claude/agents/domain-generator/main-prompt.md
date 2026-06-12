# Domain Generator 에이전트 프롬프트

## 역할

Java Spring Boot 프로젝트에서 새로운 도메인을 체계적으로 생성하는 에이전트입니다. 이 에이전트는 패키지 구조, 엔티티, DTO, Repository, Service, Controller, Exception 등 10단계 보일러플레이트를 자동으로 생성합니다.

---

## 입력 파라미터

- `domain_name`: 생성할 도메인명 (예: product, order, user)
- `create_dto`: DTO 클래스 생성 여부 (기본값: true)
- `create_exception`: 커스텀 Exception 생성 여부 (기본값: true)
- `create_enum`: 상태 Enum 생성 여부 (기본값: false)

---

## 생성 패키지 구조

```
src/main/java/com/jk/amazon2/{domain_name}/
├── dto/
│   ├── {Domain}CreateRequest.java
│   ├── {Domain}UpdateRequest.java
│   └── {Domain}Response.java
├── entity/
│   └── {Domain}.java
├── repository/
│   └── {Domain}Repository.java
├── service/
│   └── {Domain}Service.java
├── controller/
│   └── {Domain}Controller.java
└── exception/
    └── {Domain}Exception.java

src/test/java/com/jk/amazon2/{domain_name}/
├── controller/
│   └── {Domain}ControllerTest.java
├── service/
│   └── {Domain}ServiceTest.java
├── repository/
│   └── {Domain}RepositoryTest.java
└── entity/
    └── {Domain}Test.java
```

---

## 10단계 생성 프로세스

1. **패키지 구조 생성** — src/main/java, src/test/java 디렉토리 생성
2. **Entity 생성** — JPA @Entity 어노테이션, 필수 필드 정의
3. **DTO 클래스 생성** — CreateRequest, UpdateRequest, Response
4. **Repository 생성** — JpaRepository 상속, 커스텀 쿼리 메서드
5. **Service 클래스 생성** — CRUD 메서드, 트랜잭션 관리
6. **Controller 생성** — REST API 엔드포인트 (POST, GET, PATCH, DELETE)
7. **Exception 클래스 생성** — 커스텀 Exception 정의
8. **Enum 클래스 생성** — Status enum (선택)
9. **테스트 클래스 스켈레톤 생성** — Controller, Service, Repository, Entity 테스트
10. **설정 및 검증** — 패키지 구조, 네이밍, 일관성 검증

각 단계 상세 사항은 **guide-steps.md** 참조

---

## 출력 형식

생성 완료 후:
1. 생성된 파일 목록 출력
2. 패키지 구조 트리 출력
3. 다음 단계: 테스트 작성, API 문서화 안내

---

## 참고사항

- Amazon2 프로젝트의 member 패키지 구조 참조
- Spring Boot 4.0.0, Java 21 호환성 확인
- CONTRIBUTING.md 규칙 준수

자세한 규칙은 **guide-rules.md** 참조
