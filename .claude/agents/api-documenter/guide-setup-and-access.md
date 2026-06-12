# API Documenter - 설정 및 접근 방법

## Gradle 의존성

### build.gradle에 추가

```gradle
dependencies {
    implementation 'org.springdoc:springdoc-openapi-starter-webmvc-ui:2.0.0'
}
```

---

## Swagger UI 설정

### application.yml

```yaml
springdoc:
  swagger-ui:
    enabled: true
    path: /swagger-ui.html
    operations-sorter: method
    tags-sorter: alpha
  api-docs:
    path: /v3/api-docs
```

### application-local.yml (로컬 개발)

```yaml
springdoc:
  swagger-ui:
    enabled: true
    path: /swagger-ui.html
    try-it-out-enabled: true
    operations-sorter: method
    tags-sorter: alpha
    urls:
      - name: "Product API"
        url: "http://localhost:8080/v3/api-docs?group=product"
      - name: "Member API"
        url: "http://localhost:8080/v3/api-docs?group=member"
  api-docs:
    path: /v3/api-docs
```

---

## 문서 접근 방법

### Swagger UI

```
http://localhost:8080/swagger-ui.html
```

- 브라우저에서 모든 API 확인
- "Try it out" 버튼으로 API 테스트
- 요청/응답 예시 자동 표시

### OpenAPI JSON

```
http://localhost:8080/v3/api-docs
```

- 기계 가독 형식
- 외부 도구 연동 가능

### OpenAPI YAML

```
http://localhost:8080/v3/api-docs.yaml
```

- YAML 형식
- 텍스트 에디터에서 편집 가능

---

## 문서화 체크리스트

### Controller 어노테이션
- ✅ @RestController 추가
- ✅ @RequestMapping 정의
- ✅ @Tag(name="...", description="...") 추가
- ✅ 각 메서드에 @Operation 추가
- ✅ 모든 응답 코드에 @ApiResponse 추가
- ✅ 모든 파라미터에 @Parameter 추가

### DTO 어노테이션
- ✅ @Schema(description="...") 추가
- ✅ 각 필드에 @Schema(description, example) 추가
- ✅ requiredMode 지정
- ✅ allowableValues 정의 (enum)

### OpenAPI YAML
- ✅ paths 섹션에 모든 엔드포인트 정의
- ✅ components/schemas에 DTO 정의
- ✅ 모든 응답 스키마 정의
- ✅ 예시 데이터 포함
- ✅ operationId는 유일하게

### Swagger UI 설정
- ✅ application.yml에 springdoc 설정
- ✅ 의존성 추가
- ✅ 문서 접근 가능 확인

---

## 두 가지 방식 비교

| 항목 | Swagger 어노테이션 | OpenAPI YAML |
|------|-----------------|-------------|
| 작성 위치 | 자바 코드 | 별도 YAML 파일 |
| 유지보수 | 코드와 함께 관리 | 문서만 관리 |
| 자동 생성 | springdoc이 자동 생성 | 수동 작성 |
| 버전 관리 | Git에서 추적 용이 | 문서 버저닝 가능 |
| 외부 도구 | 제한적 | 도구 독립적 |
| 학습 곡선 | 낮음 | 중간 |
| 권장 | 대부분의 프로젝트 | 마이크로서비스, 게이트웨이 |

---

## 권장 방식

### 단일 모놀리식 프로젝트
→ **Swagger 어노테이션**
- 코드와 문서가 함께 유지됨
- 리팩토링 시 자동 반영
- 개발 속도 향상

### 마이크로서비스 아키텍처
→ **OpenAPI YAML** (또는 둘 다)
- 서비스별 문서 관리
- API 게이트웨이와 연동
- 외부 도구 지원

### Amazon2 프로젝트 권장
→ **Swagger 어노테이션** + **OpenAPI YAML** (선택)
- 주로 Swagger 어노테이션 사용
- 필요시 OpenAPI YAML로 추가 문서화
- 클라이언트 코드 생성 필요 시 YAML 활용

---

## 생성 파일 요약

### 필수 파일

1. **Controller** (수정)
   - @Tag, @Operation, @ApiResponse 추가
   
2. **DTO** (수정)
   - @Schema 어노테이션 추가

3. **application.yml** (수정 또는 신규)
   - springdoc 설정 추가

### 선택 파일

4. **docs/openapi/{domain}-api.yaml** (신규)
   - OpenAPI 3.0 스펙
   - YAML 형식

---

## 문제 해결

### Swagger UI가 나타나지 않음

```yaml
# application.yml 확인
springdoc:
  swagger-ui:
    enabled: true
```

### 어노테이션이 반영되지 않음

```bash
# 프로젝트 재빌드
./gradlew clean build

# 서버 재시작
./gradlew bootRun --args='--spring.profiles.active=local'
```

### 스키마가 표시되지 않음

```java
// DTO에 @Schema 추가되었는지 확인
@Schema(description = "...", example = "...")
private String fieldName;
```

---

## 참고 링크

- [springdoc-openapi 공식 문서](https://springdoc.org/)
- [OpenAPI 3.0 스펙](https://spec.openapis.org/oas/v3.0.3)
- [Swagger UI 공식 사이트](https://swagger.io/tools/swagger-ui/)
