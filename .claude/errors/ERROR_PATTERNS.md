# 에러 패턴 분석 (ERROR_PATTERNS.md)

Amazon2 프로젝트에서 자주 발생하는 3가지 에러 패턴을 정의하고, 각 패턴의 감지 방법과 해결책을 제시합니다.

## 패턴 1: 패키지 네이밍 및 구조 오류 (Package Naming Error)

### 패턴 설명

도메인 패키지가 Amazon2 표준 구조를 따르지 않는 경우입니다.

### 감지 조건

1. **패키지 경로 오류**
   - 경로가 `com.jk.amazon2.{domain}` 형식이 아님
   - 예: `com.amazon2.product` (amazon2 앞에 jk 없음)
   - 예: `amazon2.product` (회사/사용자 네임스페이스 없음)

2. **서브패키지 명명 오류**
   - 표준: `entity`, `dto`, `repository`, `service`, `controller`, `exception`
   - 오류: `model`, `entities`, `dtos`, `impl`, `handler`

3. **클래스 파일 위치 오류**
   - Entity가 `com.jk.amazon2.product.entity` 대신 `com.jk.amazon2.product`에 위치
   - DTO가 별도 폴더가 아니라 entity/dto 폴더 없음

4. **테스트 클래스 경로 오류**
   - 테스트: `src/test/java/com/jk/amazon2/{domain}/**`이 아닌 곳에 위치

### 패턴 예시

```
❌ 잘못된 구조
src/main/java/com/jk/amazon2/
├── product/
│   ├── Product.java (entity 폴더 없음)
│   ├── ProductService.java
│   ├── ProductDTO.java
│   └── ProductRepository.java

✅ 올바른 구조
src/main/java/com/jk/amazon2/product/
├── entity/
│   └── Product.java
├── dto/
│   ├── ProductCreateRequest.java
│   ├── ProductUpdateRequest.java
│   └── ProductResponse.java
├── service/
│   └── ProductService.java
├── repository/
│   └── ProductRepository.java
├── controller/
│   └── ProductController.java
└── exception/
    └── ProductException.java
```

### Dependency Analyzer 검증 로직

```java
public class PackageNamingChecker {
    
    /**
     * 패키지 네이밍 규칙 검증
     * 
     * 규칙:
     * 1. 기본 패키지: com.jk.amazon2.{domain}
     * 2. 서브패키지: entity, dto, repository, service, controller, exception
     * 3. 각 도메인별 폴더 구조 분리
     */
    public List<Issue> checkPackageNaming() {
        List<Issue> issues = new ArrayList<>();
        
        File javaDir = new File("src/main/java/com/jk/amazon2/");
        for (File domainDir : javaDir.listFiles(File::isDirectory)) {
            String domainName = domainDir.getName();
            
            // 예약된 폴더가 아닌지 확인 (config, exception, common은 제외)
            if (isReservedDomain(domainName)) {
                continue;
            }
            
            // 1. 필수 서브패키지 확인
            String[] requiredPackages = {"entity", "dto", "repository", "service", "controller"};
            for (String pkg : requiredPackages) {
                File packageDir = new File(domainDir, pkg);
                if (!packageDir.exists()) {
                    issues.add(new Issue(
                        "E100",
                        "Missing required package: " + pkg,
                        domainDir.getPath() + "/" + pkg,
                        "Create " + pkg + " directory"
                    ));
                }
            }
            
            // 2. 도메인 루트에 Java 파일이 있는지 확인 (폴더 오류)
            File[] javaFiles = domainDir.listFiles((d, n) -> n.endsWith(".java"));
            if (javaFiles != null && javaFiles.length > 0) {
                for (File javaFile : javaFiles) {
                    issues.add(new Issue(
                        "E100",
                        "Java file in wrong location: " + javaFile.getName(),
                        domainDir.getPath(),
                        "Move to appropriate subpackage (entity, dto, service, etc)"
                    ));
                }
            }
            
            // 3. 비표준 서브패키지 확인
            String[] validPackages = {"entity", "dto", "repository", "service", "controller", "exception"};
            for (File subDir : domainDir.listFiles(File::isDirectory)) {
                if (!isValidPackage(subDir.getName(), validPackages)) {
                    issues.add(new Issue(
                        "WARNING",
                        "Non-standard package name: " + subDir.getName(),
                        subDir.getPath(),
                        "Rename to one of: " + String.join(", ", validPackages)
                    ));
                }
            }
        }
        
        return issues;
    }
    
    private boolean isReservedDomain(String name) {
        return Arrays.asList("config", "exception", "common").contains(name);
    }
    
    private boolean isValidPackage(String name, String[] valid) {
        return Arrays.asList(valid).contains(name);
    }
}
```

### Consistency Checker 검증 로직

```java
public class ConsistencyChecker {
    
    public List<Issue> checkPackageStructure(String domainName) {
        List<Issue> issues = new ArrayList<>();
        
        // 경로 구성
        String basePackagePath = String.format(
            "src/main/java/com/jk/amazon2/%s", 
            domainName
        );
        
        // 필수 파일 검증
        Map<String, String> requiredClasses = new HashMap<>();
        requiredClasses.put("entity", domainName + ".java");
        requiredClasses.put("dto", domainName + "CreateRequest.java");
        requiredClasses.put("repository", domainName + "Repository.java");
        
        for (Map.Entry<String, String> entry : requiredClasses.entrySet()) {
            String packagePath = basePackagePath + "/" + entry.getKey();
            File classFile = new File(packagePath + "/" + entry.getValue());
            
            if (!classFile.exists()) {
                issues.add(new Issue(
                    "ERROR",
                    "Missing class file: " + entry.getValue(),
                    classFile.getPath(),
                    "Create " + entry.getValue() + " in " + entry.getKey() + " package"
                ));
            }
        }
        
        return issues;
    }
}
```

### 해결 방법

1. **자동 구조 생성**: Domain Generator 에이전트 사용
   ```bash
   # Domain Generator 에이전트 실행
   # domain_name: product
   # 결과: com.jk.amazon2.product 패키지 생성
   ```

2. **수동 리팩토링**: 기존 코드를 올바른 구조로 이동
   - entity 폴더 생성 후 Entity 클래스 이동
   - dto 폴더 생성 후 DTO 클래스 이동
   - 각 layer별 폴더 생성

3. **import 경로 수정**: 패키지 이동 후 모든 import 문 검증

---

## 패턴 2: 크로스 도메인 의존성 (Cross-Domain Dependency)

### 패턴 설명

한 도메인의 클래스가 다른 도메인의 Service/Entity에 직접 의존하는 경우입니다. Amazon2는 도메인 계층(member > category > posting)을 정의하고 있으며, 이를 위반하는 의존성을 감지합니다.

### 도메인 계층 정의

```
Level 1: member (상위 도메인, 모든 도메인의 기초)
         ↓ (member는 아무것도 의존하지 않음)

Level 2: category (중간 도메인, member에만 의존 가능)
         ↓ (category는 member에 의존 가능)

Level 3: posting (하위 도메인, member와 category에 의존 가능)
         (posting은 member/category에 의존 가능)
```

### 감지 조건

1. **역방향 의존성**
   - category.service → member.service (X)
   - posting.service → member.service (X)
   - posting.service → category.service (X)

2. **양방향 의존성**
   - A.service ↔ B.service (X)

3. **도메인 간 Entity 직접 참조**
   - category.entity → member.entity (X, DTO 사용)
   - posting.entity → category.entity (X, DTO 사용)

4. **도메인 간 Repository 직접 사용**
   - category.service에서 member.repository 주입 (X)

### 패턴 예시

```
❌ 금지된 의존성
category/
└── service/
    └── CategoryService.java
        ├── @Autowired MemberService memberService  // ❌ E003
        └── memberService.findById(id)

❌ 금지된 Entity 의존
posting/
└── entity/
    └── Post.java
        └── Category category  // ❌ Category Entity 직접 참조
                               // ✅ categoryId (Long)로 변경

✅ 올바른 의존성 (Dependency Injection)
category/
└── service/
    └── CategoryService.java
        ├── @Autowired CategoryRepository repository
        └── repository.findById(id)

✅ 올바른 Entity 설계
posting/
├── entity/
│   └── Post.java
│       └── Long categoryId  // DTO에서 Category 정보 제공
├── dto/
│   └── PostResponse.java
│       └── CategoryResponse category  // 응답에만 포함
```

### Dependency Analyzer 검증 로직

```java
public class CrossDomainDependencyChecker {
    
    // 도메인 계층 정의
    static Map<String, Integer> DOMAIN_HIERARCHY = new HashMap<>();
    static {
        DOMAIN_HIERARCHY.put("member", 1);
        DOMAIN_HIERARCHY.put("category", 2);
        DOMAIN_HIERARCHY.put("posting", 3);
    }
    
    /**
     * 크로스 도메인 의존성 검사
     */
    public List<Violation> checkCrossDomainDependencies() {
        List<Violation> violations = new ArrayList<>();
        
        // 모든 클래스 분석
        for (JavaClass clazz : allClasses) {
            String fromDomain = extractDomain(clazz.getPath());
            
            // 모든 의존성 확인
            for (String dependency : clazz.getImports()) {
                String toDomain = extractDomain(dependency);
                
                // 도메인 간 의존성인 경우
                if (!fromDomain.equals(toDomain) && isInternalDomain(toDomain)) {
                    
                    // 1. 역방향 의존성 확인
                    int fromLevel = DOMAIN_HIERARCHY.getOrDefault(fromDomain, 999);
                    int toLevel = DOMAIN_HIERARCHY.getOrDefault(toDomain, 0);
                    
                    if (fromLevel > toLevel) {
                        // 하위 도메인이 상위 도메인에 의존
                        violations.add(new Violation(
                            "E003",
                            "Lower domain depends on higher domain",
                            clazz.getPath(),
                            dependency,
                            String.format(
                                "%s (level %d) cannot depend on %s (level %d)",
                                fromDomain, fromLevel, toDomain, toLevel
                            )
                        ));
                    }
                    
                    // 2. Service 직접 의존 확인
                    if (isServiceClass(dependency)) {
                        // 같은 계층 도메인의 Service 의존 검사
                        if (fromLevel == toLevel) {
                            violations.add(new Violation(
                                "E003_SERVICE",
                                "Same-level domains should not directly depend",
                                clazz.getPath(),
                                dependency
                            ));
                        }
                    }
                    
                    // 3. Entity 직접 참조 확인
                    if (isEntityClass(dependency)) {
                        violations.add(new Violation(
                            "E008",
                            "Entity should not reference other domain's Entity",
                            clazz.getPath(),
                            dependency,
                            "Use DTO or ID reference instead"
                        ));
                    }
                }
            }
        }
        
        return violations;
    }
    
    private String extractDomain(String classPath) {
        // "com.jk.amazon2.{domain}.service.ProductService" → "product"
        Pattern pattern = Pattern.compile("com\\.jk\\.amazon2\\.([a-z]+)");
        Matcher matcher = pattern.matcher(classPath);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }
    
    private boolean isServiceClass(String className) {
        return className.contains(".service.");
    }
    
    private boolean isEntityClass(String className) {
        return className.contains(".entity.");
    }
    
    private boolean isInternalDomain(String domain) {
        return DOMAIN_HIERARCHY.containsKey(domain);
    }
}
```

### 해결 방법

1. **Event-driven 아키텍처**: 도메인 간 느슨한 결합
   ```java
   // CategoryService에서 MemberService 대신 이벤트 발행
   @Service
   public class CategoryService {
       @Autowired
       private ApplicationEventPublisher eventPublisher;
       
       public void createCategory(CategoryCreateRequest request) {
           Category category = new Category(...);
           eventPublisher.publishEvent(new CategoryCreatedEvent(category));
       }
   }
   ```

2. **공통 유틸리티**: common 패키지로 공유 로직 추출
   ```
   com/jk/amazon2/common/
   └── util/
       └── DateUtils.java
   ```

3. **DTO를 통한 데이터 전달**: Entity 대신 DTO 사용
   ```java
   // Entity 직접 참조 대신
   Post post = new Post(category);  // ❌
   
   // ID 또는 DTO 참조 사용
   Post post = new Post(categoryId);  // ✅
   // 또는 응답시에만 DTO 포함
   PostResponse postResponse = new PostResponse(post, categoryDTO);
   ```

---

## 패턴 3: 순환 의존성 (Circular Dependency)

### 패턴 설명

클래스 또는 패키지 간에 순환적인 의존성이 존재하는 경우입니다. A → B → C → A 형태의 사이클이 발생하면 컴파일 오류가 발생합니다.

### 감지 조건

1. **직접 순환**
   - A ↔ B (A → B, B → A)

2. **간접 순환**
   - A → B → C → A

3. **자기 참조**
   - A → A

4. **패키지 레벨 순환**
   - product.service → category.service → product.service

### 패턴 예시

```
❌ 직접 순환
ProductService → MemberService
                 ↓
         MemberService → ProductService

❌ 간접 순환
PostService → CategoryService → MemberService → PostService
    ↑___________________________________|

✅ 올바른 구조 (단방향)
PostService → CategoryService
            ↓
        MemberService
```

### Dependency Analyzer 검증 로직

```java
public class CircularDependencyDetector {
    
    /**
     * 순환 의존성 감지 (DFS 기반)
     */
    public List<Cycle> detectCycles() {
        Map<String, Set<String>> graph = buildDependencyGraph();
        List<Cycle> cycles = new ArrayList<>();
        
        // 모든 노드에서 시작하여 사이클 검사
        for (String startNode : graph.keySet()) {
            List<String> path = new ArrayList<>();
            Set<String> visited = new HashSet<>();
            
            if (dfs(startNode, startNode, graph, path, visited)) {
                // 사이클 발견
                cycles.add(new Cycle(path));
            }
        }
        
        return cycles;
    }
    
    /**
     * DFS로 사이클 검사
     * 
     * @param current 현재 노드
     * @param start 시작 노드
     * @param graph 의존성 그래프
     * @param path 현재 경로
     * @param visited 방문한 노드
     * @return 사이클 존재 여부
     */
    private boolean dfs(
        String current,
        String start,
        Map<String, Set<String>> graph,
        List<String> path,
        Set<String> visited
    ) {
        path.add(current);
        visited.add(current);
        
        if (!graph.containsKey(current)) {
            return false;
        }
        
        for (String neighbor : graph.get(current)) {
            if (neighbor.equals(start) && path.size() > 1) {
                // 시작 노드로 돌아옴 → 사이클 발견
                path.add(start);
                return true;
            }
            
            if (!visited.contains(neighbor)) {
                if (dfs(neighbor, start, graph, path, visited)) {
                    return true;
                }
            }
        }
        
        visited.remove(current);
        path.remove(path.size() - 1);
        return false;
    }
    
    /**
     * 의존성 그래프 구성
     */
    private Map<String, Set<String>> buildDependencyGraph() {
        Map<String, Set<String>> graph = new HashMap<>();
        
        for (JavaClass javaClass : allClasses) {
            Set<String> dependencies = new HashSet<>();
            
            // import 문 파싱
            for (String importStatement : javaClass.getImports()) {
                // com.jk.amazon2.product.service.ProductService 형태의
                // 클래스명 추출
                String className = extractClassName(importStatement);
                dependencies.add(className);
            }
            
            graph.put(javaClass.getFullName(), dependencies);
        }
        
        return graph;
    }
}

class Cycle {
    List<String> path;  // [A, B, C, A]
    
    public String getDescription() {
        return String.join(" → ", path);
    }
}
```

### 해결 방법

1. **의존성 방향 역전**: 한쪽 의존성 제거
   ```java
   // Before: 순환 의존성
   // ProductService → MemberService
   // MemberService → ProductService
   
   // After: 단방향 의존성
   // ProductService → MemberService
   // (MemberService는 ProductService에 의존하지 않음)
   ```

2. **중간 계층 추가**: 공통 인터페이스/전략 패턴
   ```java
   // 인터페이스 정의
   public interface DomainProcessor {
       void process(Data data);
   }
   
   // 각 도메인에서 구현
   public class ProductProcessor implements DomainProcessor { }
   public class MemberProcessor implements DomainProcessor { }
   
   // 공통 service에서 사용
   public class CommonService {
       @Autowired
       private DomainProcessor processor;
   }
   ```

3. **이벤트 기반 아키텍처**: 느슨한 결합
   ```java
   // 이벤트 발행/구독으로 순환 의존성 제거
   ```

4. **지연 로딩**: Lazy 초기화
   ```java
   @Lazy
   @Autowired
   private CircularDependencyService service;
   ```

---

## 에러 패턴 요약 테이블

| 패턴 | 감지 방법 | 해결 방법 | 심각도 |
|------|---------|---------|-------|
| 패키지 네이밍 | 파일 시스템 스캔 + AST | Domain Generator | HIGH |
| 크로스 도메인 | import 분석 + 도메인 맵 | Event-driven / DTO | HIGH |
| 순환 의존성 | DFS 그래프 분석 | 의존성 역전 / 이벤트 | CRITICAL |

---

## 참고사항

- 모든 에러 코드는 `.claude/errors/ERRORS.md` 참조
- 실제 위반 사항은 Dependency Analyzer 에이전트로 감지
- 수정 후 메모리 시스템에 피드백 기록 (`.claude/memory/error_patterns/agent_feedback.md`)
