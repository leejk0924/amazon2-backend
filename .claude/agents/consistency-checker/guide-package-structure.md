# Harness Consistency Checker - 패키지 구조 검증 가이드

## 표준 패키지 구조

```
src/main/java/com/jk/amazon2/{domain}/
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

src/test/java/com/jk/amazon2/{domain}/
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

## 검증 규칙

### 1단계: 도메인 경로 확인

```
검증:
- 경로 존재 여부: src/main/java/com/jk/amazon2/{domain}/
- 디렉토리 구조 확인
```

**위반 시**: ERROR - 경로 재설정 필요

---

### 2단계: 필수 패키지 확인

**필수 패키지**:
- [ ] dto/ 존재
- [ ] entity/ 존재
- [ ] repository/ 존재
- [ ] service/ 존재
- [ ] controller/ 존재
- [ ] exception/ 존재 (권장)

**위반 시**: ERROR - 패키지 누락

---

### 3단계: 필수 클래스 확인

**필수 클래스**:
- [ ] {Domain}CreateRequest.java
- [ ] {Domain}UpdateRequest.java
- [ ] {Domain}Response.java
- [ ] {Domain}.java (Entity)
- [ ] {Domain}Repository.java
- [ ] {Domain}Service.java
- [ ] {Domain}Controller.java

**선택 클래스**:
- [ ] {Domain}Exception.java
- [ ] {Domain}Enum.java (Status 등)

**위반 시**: ERROR - 클래스 누락

---

### 4단계: 테스트 구조 확인

**필수 테스트**:
- [ ] {Domain}ControllerTest.java
- [ ] {Domain}ServiceTest.java
- [ ] {Domain}RepositoryTest.java
- [ ] {Domain}Test.java

**위반 시**: WARNING - 테스트 누락

---

## 자동 수정 규칙

### 패키지 생성 (auto_fix=true)

```
자동 생성 가능:
- 누락된 패키지 디렉토리 생성
```

### 클래스 생성

```
자동 생성 불가:
- 클래스 파일은 개발자가 직접 생성해야 함
- 복잡한 비즈니스 로직이 필요
```

---

## 예시

### ✅ 통과 구조

```
src/main/java/com/jk/amazon2/product/
├── controller/
│   └── ProductController.java
├── service/
│   └── ProductService.java
├── repository/
│   └── ProductRepository.java
├── entity/
│   └── Product.java
├── dto/
│   ├── ProductCreateRequest.java
│   ├── ProductUpdateRequest.java
│   └── ProductResponse.java
└── exception/
    └── ProductNotFoundException.java
```

### ❌ 실패 구조

```
src/main/java/com/jk/amazon2/
├── product/
│   ├── Product.java
│   ├── ProductService.java  ← 패키지 분리 필요
│   ├── ProductRepository.java ← 패키지 분리 필요
│   └── ...
```

---

## 심각도

| 항목 | 심각도 | 설명 |
|------|-------|------|
| 도메인 경로 오류 | ERROR | 전체 구조 재설정 필요 |
| 필수 패키지 누락 | ERROR | 패키지 생성 필수 |
| 필수 클래스 누락 | ERROR | 클래스 생성 필수 |
| 테스트 누락 | WARNING | 테스트 작성 권장 |
| 선택 클래스 누락 | INFO | 필요시 생성 |
