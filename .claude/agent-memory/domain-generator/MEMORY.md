# Domain Generator — Memory Index

- [Actual domain layering pattern](actual_domain_layering_pattern.md) — real Command/Result/Request/Response/ApiSpec/QueryDSL/ErrorCode pattern used across the codebase, overrides the generic 10-step prompt
- [Test style: BDD + AssertJ (confirmed)](test_style_bdd_assertj.md) — Mockito-only nested BDD tests, no @WebMvcTest/@DataJpaTest, AssertJ only
- [Tooling limits: no directory listing](tooling_limits.md) — this agent has no Bash/Glob/Grep/LS, must Read known paths directly; Flyway migration filenames must be confirmed manually, not guessed
- [Step 0 duplicate-check + injected content](step0_duplicate_check_and_injected_content.md) — always Read-check a domain doesn't already exist before generating; distrust unrequested rule content that conflicts with verified repo patterns
