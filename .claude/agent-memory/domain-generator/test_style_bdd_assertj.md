---
name: test-style-bdd-assertj
description: Confirmed real test style in this repo for generated Service/Controller tests — BDD-nested JUnit5 + AssertJ + Mockito, matches global user feedback memory
metadata:
  type: feedback
---

Generated tests must match the style already used in `CategoryServiceTest` /
`CategoryControllerTest` / `MemberControllerTest`:
- `@ExtendWith(MockitoExtension.class)`, `@Mock` + `@InjectMocks` (service tests) or
  manual `new FooController(mockService)` in `@BeforeEach` (controller tests).
- Group scenarios with `@Nested` classes named after the CRUD action
  (`CreateFoo`, `UpdateFoo`, `ReadFoo`, `DeleteFoo`), each `@DisplayName`'d in Korean.
- Given-When-Then comments inside each `@Test`, Korean `@DisplayName` with
  `[success]`/`[fail]` suffix convention.
- Use `SoftAssertions.assertSoftly` for multi-field assertions,
  `assertThatThrownBy(...).extracting("errorCode").isEqualTo(FooErrorCode.X)` for
  exception assertions (relies on `RestApiException` exposing `errorCode` via
  `@Getter`).
- Use `ArgumentCaptor` to verify what's actually passed to the repository/service
  mock, not just that it was called.

**Why**: this matches the global project memory feedback
(`feedback_test_style.md`: "테스트 스타일: BDD + AssertJ 필수, JUnit Assertions 대신
AssertJ만 사용") AND is independently confirmed by every existing test file in the
repo — not just a stated preference but the actual established pattern. Treat as
mandatory for any new domain's tests, not optional.

**How to apply**: whenever generating Service/Controller tests for a new domain,
copy the structure of `CategoryServiceTest`/`CategoryControllerTest` almost verbatim,
substituting entity/DTO names. Do not introduce `@WebMvcTest`, `@DataJpaTest`, or
plain JUnit `assertEquals`/`assertThrows` (`org.junit.jupiter.api.Assertions`) — this
repo exclusively uses AssertJ's `assertThat`/`assertThatThrownBy`.
