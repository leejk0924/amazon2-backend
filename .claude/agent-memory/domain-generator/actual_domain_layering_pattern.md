---
name: actual-domain-layering-pattern
description: Amazon2 backend's real DTO/service/repository layering pattern for each domain, derived by reading member/category/posting source (not the generic 10-step boilerplate described in the agent prompt)
metadata:
  type: project
---

The Amazon2 codebase does NOT use plain `XxxRequest`/`XxxResponse` DTOs directly at the
service boundary. Verified by reading `category` and `member` domains (2026-07-04).

**Real per-domain file set** (e.g. for domain `Foo`):
- `entity/Foo.java` — extends `BaseCreation` (createdAt, createdBy only) or `BaseAudit`
  (adds updatedAt, updatedBy) from `com.jk.amazon2.common.entity`. Uses static factory
  `Foo.of(...)` instead of a public constructor/builder. Mutation via named methods
  (`update(...)`, `softDelete()`), not setters. Category uses a String business-key PK
  (`code`) + `Persistable<String>` because it has no auto-increment id; Member/Posting use
  `Long id` with `@GeneratedValue(IDENTITY)`. Default to Long id unless the domain has an
  obvious natural key like Category's code.
- `dto/FooRequest.java` — outer class w/ private no-args ctor, containing nested
  `record CreateDto`, `record UpdateDto`, `record SearchCondition` (all Bean Validation
  annotated). This is the only "request DTO" — no separate CreateRequest/UpdateRequest
  top-level classes.
- `dto/FooCommand.java` — outer class containing nested `Create` (private ctor,
  validates + throws `RestApiException` on bad input, has `.from(RequestDto)` and
  `.of(...)`) and `Update` (simple `.of(...)` factory, no validation). This is what
  Controller passes into Service — NOT the raw Request DTO.
- `dto/FooResult.java` — outer class containing nested `Detail` (full fields incl.
  createdAt/createdBy, has `.from(Entity)` and `.of(...)`) and `Info` (list/summary
  view, `@AllArgsConstructor(staticName = "of")`). This is what Service returns.
- `dto/FooResponse.java` — outer class containing nested `CreateDto`, `UpdateDto`,
  `Info` (records or getter classes), each with `.from(Result.X)`. This is what
  Controller returns to the client.
- `repository/FooRepository.java` — `extends JpaRepository<Foo, IdType>, FooQueryRepository`
  + simple derived query methods (`existsByX`, `findByXAndDeletedFalse`).
- `repository/FooQueryRepository.java` — interface with one method:
  `Page<Foo> search(FooRequest.SearchCondition condition, Pageable pageable)`.
- `repository/FooRepositoryImpl.java` — QueryDSL `JPAQueryFactory`-based implementation
  of the search interface (dynamic `BooleanExpression` per filter + custom
  `OrderSpecifier` builder from `Pageable.getSort()`). Requires the QueryDSL annotation
  processor to generate `QFoo` from the `@Entity` — no manual Q-class needed.
- `service/FooService.java` — single `@Service` class (NOT split into
  Command/QueryService despite what `harnesses/*/README.md` says — that README is
  aspirational/stale, real code uses one service). Constructor injection via
  `@RequiredArgsConstructor`. Methods: `create`, `update`, `delete`, single-get,
  paged search. `@Transactional` on writes, `@Transactional(readOnly = true)` on reads.
- `controller/FooController.java implements FooApiSpec` + `controller/FooApiSpec.java`
  — the ApiSpec interface holds all Swagger (`@Tag`/`@Operation`/`@ApiResponse`)
  annotations and method signatures; the Controller impl just has `@Override` +
  Spring `@GetMapping` etc. Keeps Swagger docs out of the controller impl. (Member
  domain puts this in a `controller.spec` subpackage instead of directly in
  `controller` — inconsistent across domains; category-style flat placement is
  simpler and was used for new generation.)
- `exception/FooErrorCode.java` — a single `enum FooErrorCode implements ErrorCode`
  (from `com.jk.amazon2.common.exception.ErrorCode`) with `HttpStatus` + Korean
  message per constant. **There are no per-domain custom exception classes** — every
  domain throws the shared `com.jk.amazon2.common.exception.RestApiException`
  constructed with one of these enum constants. So "create_exception=true" in the
  domain-generator spec effectively means "create the FooErrorCode enum", not a
  `FooNotFoundException` class.
- No `enums/` package pattern seen in existing domains for entity-state fields — they
  use `boolean deleted` instead of a status enum. Only add an enum package if the
  domain genuinely needs a closed set of business states.

**Tests**: only Mockito-based unit tests exist for Service and Controller
(`@ExtendWith(MockitoExtension.class)`, `@Mock`/`@InjectMocks` for service tests,
manual `new FooController(mockService)` construction for controller tests — no
`@WebMvcTest`). No `@DataJpaTest` repository tests exist for any domain. Follow this
— do not generate `@WebMvcTest`/`@DataJpaTest` classes unless the user explicitly asks,
since it would be inconsistent with everything else in the repo. See
[[test_style_bdd_assertj]].

**Schema**: `spring.jpa.hibernate.ddl-auto: none` and Flyway is enabled
(`classpath:/db/migration`). Adding a new `@Entity` requires a hand-written Flyway
migration SQL file — it will NOT be auto-created. Domain generation is incomplete
without also asking the user for/adding the migration script (get the next `V{n}__...sql`
number from the existing files in `src/main/resources/db/migration/`).

**Update 2026-07-04**: `duplicatetest123`, `duplicatetest456`, and `duplicatetest789`
(three disposable test domains used to probe Step-0 duplicate-check behavior) have all
been deleted by the user — none exist anywhere in this worktree (`src/main/java/...`
and `src/test/java/...` both clean). Do not assume any of these domain names still
exist; verify with `Read`/`Glob` before referencing them again. There is currently no
concrete example of this layering pattern in the repo other than the real `member`/
`category`/`posting` domains themselves — re-read one of those if this memory needs
updating. When cleaning up any disposable test domain in the future, delete both
`src/main/java/.../{domain}` and `src/test/java/.../{domain}` together (this agent has
no Bash/shell tool to do the deletion itself — see [[tooling_limits]] — so flag it to
the user).
