# Claude 에이전트 사용 가이드

Amazon2 프로젝트의 6개 에이전트를 효과적으로 활용하기 위한 가이드입니다.

---

## 🔄 작업 흐름별 에이전트 사용

### 시나리오 1: 새로운 도메인 추가

```
당신: "comment 도메인을 만들어줘. 
       Comment, CommentDTO, CommentService, CommentController 필요해"

내가 자동으로:
1️⃣ Domain Generator 실행
   → comment 도메인 전체 구조 자동 생성
   
2️⃣ 생성된 코드 확인
   → 패키지 구조, 네이밍 규칙 검증
   
3️⃣ 테스트 필요 여부 확인
   당신: "테스트도 작성해줘"

4️⃣ Test Generator 실행
   → CommentServiceTest, CommentControllerTest 자동 생성
   
5️⃣ 하네스 규칙 검증
   당신: "규칙 확인해줄래?"

6️⃣ Consistency Checker 실행
   → comment 도메인이 Amazon2 규칙을 따르는지 검증
```

---

### 시나리오 2: 기존 도메인 리팩토링

```
당신: "MonitoringController가 Repository를 직접 주입받아서
       Service 계층을 우회해. 이걸 고쳐줘"

내가 자동으로:
1️⃣ 문제 분석
   → Service 계층 분리 필요 인식

2️⃣ Service 생성 및 Controller 수정
   → MonitoringService 생성
   → MonitoringController의 의존성 변경

3️⃣ 테스트 생성
   당신: "테스트도 생성해줘"

4️⃣ Test Generator 실행
   → MonitoringServiceTest, MonitoringControllerTest 생성

5️⃣ 의존성 검증
   당신: "의존성 구조 확인해줄래?"

6️⃣ Dependency Analyzer 실행
   → posting → category → member 의존성 구조 검증
   → 순환 참조 검사
```

---

### 시나리오 3: API 문서화

```
당신: "PostingController의 모든 엔드포인트에 Swagger 문서를 붙여줄래?"

내가 자동으로:
1️⃣ API Documenter 실행
   → @Operation, @ApiResponse 애노테이션 추가
   → DTO에 @Schema 추가
   → OpenAPI 문서 생성

2️⃣ 생성된 문서 검증
   당신: "Swagger UI에서 문서가 제대로 보이는지 확인해줘"
```

---

## 📋 에이전트별 상세 정보

### Domain Generator

| 항목 | 설명 |
|------|------|
| **목적** | 새로운 도메인의 기본 구조 자동 생성 |
| **생성물** | Entity, DTO, Repository, Service, Controller, Exception |
| **언제 사용** | 새로운 도메인(member, category, posting 등) 추가 시 |
| **명령어** | "comment 도메인 생성해줘" |
| **예상 시간** | 2-3분 |

**생성되는 파일 구조:**
```
src/main/java/com/jk/amazon2/comment/
├── controller/
│   └── CommentController.java
├── service/
│   └── CommentService.java
├── repository/
│   └── CommentRepository.java
├── entity/
│   └── Comment.java
├── dto/
│   ├── CommentRequest.java
│   └── CommentResponse.java
└── exception/
    └── CommentException.java
```

---

### Test Generator

| 항목 | 설명 |
|------|------|
| **목적** | JUnit5/Mockito 기반 테스트 자동 생성 |
| **생성물** | ServiceTest (@ExtendWith), ControllerTest (@WebMvcTest) |
| **언제 사용** | Service/Controller 구현 완료 후 |
| **명령어** | "CommentService와 CommentController 테스트 생성해줘" |
| **예상 시간** | 3-4분 |

**생성되는 테스트:**
```
src/test/java/com/jk/amazon2/comment/
├── service/
│   └── CommentServiceTest.java (단위 테스트)
└── controller/
    └── CommentControllerTest.java (MockMvc 테스트)
```

---

### API Documenter

| 항목 | 설명 |
|------|------|
| **목적** | REST API에 Swagger/OpenAPI 문서 자동 추가 |
| **생성물** | @Operation, @ApiResponse, @Schema 애노테이션 |
| **언제 사용** | Controller 엔드포인트 문서화 필요 시 |
| **명령어** | "PostingController API 문서 자동 생성해줘" |
| **예상 시간** | 2-3분 |

**추가되는 애노테이션:**
```java
@Operation(summary = "포스팅 조회", description = "ID로 포스팅을 조회합니다")
@ApiResponse(responseCode = "200", description = "조회 성공")
@ApiResponse(responseCode = "404", description = "포스팅 없음")
@GetMapping("/{id}")
public ResponseEntity<PostingResponse> getPosting(@PathVariable Long id) {
    // ...
}
```

---

### Consistency Checker

| 항목 | 설명 |
|------|------|
| **목적** | 도메인 패키지가 하네스 규칙을 따르는지 검증 |
| **검증 항목** | 패키지 구조, 클래스 네이밍, 애노테이션, DTO 규칙 |
| **언제 사용** | 새 도메인 또는 리팩토링 후 규칙 준수 확인 시 |
| **명령어** | "posting 도메인이 규칙을 따르는지 확인해줘" |
| **예상 시간** | 1-2분 |

**검증 항목:**
- ✅ 패키지 구조 (controller, service, repository, entity, dto 등)
- ✅ 클래스 네이밍 규칙 (PascalCase)
- ✅ @Entity, @Service, @Repository 애노테이션
- ✅ DTO 필드 검증
- ✅ Exception 처리

---

### Dependency Analyzer

| 항목 | 설명 |
|------|------|
| **목적** | 모듈 간 의존성 구조 분석 및 검증 |
| **분석 항목** | 의존성 그래프, 순환 참조, 계층 위반 |
| **언제 사용** | 의존성 구조 확인 또는 리팩토링 후 검증 시 |
| **명령어** | "posting → category → member 의존성이 맞는지 확인해줄래?" |
| **예상 시간** | 2-3분 |

**검증 규칙:**
```
posting → category ✅ OK
posting → member  ✅ OK
category → member ✅ OK
member → (다른 도메인) ❌ NOT ALLOWED
```

---

## ✅ 체크리스트

### 새로운 도메인 추가 시

- [ ] Domain Generator로 도메인 생성
- [ ] 생성된 코드 검토
- [ ] Test Generator로 테스트 생성
- [ ] 테스트 실행 및 통과 확인
- [ ] API Documenter로 API 문서화
- [ ] Consistency Checker로 규칙 준수 확인
- [ ] Dependency Analyzer로 의존성 검증
- [ ] PR 생성 및 머지

### 기존 도메인 리팩토링 시

- [ ] 리팩토링 완료
- [ ] Test Generator로 필요시 테스트 추가/수정
- [ ] Consistency Checker로 규칙 준수 확인
- [ ] Dependency Analyzer로 의존성 검증
- [ ] PR 생성 및 머지

---

## 🤔 자주 묻는 질문

### Q: 에이전트를 직접 호출할 수 있나?
**A:** 아니요. Claude Code가 자동으로 판단해서 필요한 에이전트를 호출합니다. 
"Domain Generator를 사용해줘"보다는 "comment 도메인 만들어줘"라고 요청하면 됩니다.

### Q: 에이전트가 생성한 코드를 수정할 수 있나?
**A:** 네. 생성된 코드는 기본 틀일 뿐입니다. 필요시 직접 수정하거나 변경을 요청하면 됩니다.

### Q: 여러 도메인을 한 번에 생성할 수 있나?
**A:** 가능하지만 한 도메인씩 하는 것을 권장합니다. 
예: "comment와 like 도메인을 순서대로 만들어줄래?" (한 번에 한 도메인씩 진행)

### Q: 테스트는 필수인가?
**A:** 네. Test Generator가 생성하는 테스트는 모든 메서드를 검증합니다. 
테스트 없이는 머지할 수 없습니다.

---

**마지막 업데이트**: 2026-06-15