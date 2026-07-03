---
name: tooling-limits-no-directory-listing
description: This agent's toolset has no Bash/Glob/Grep/LS — only Read/Write/Edit/WebFetch/WebSearch/NotebookEdit/TaskStop. Directory contents cannot be listed directly.
metadata:
  type: project
---

When invoked as the domain-generator subagent, the available tools do NOT include a
directory-listing or search tool (no Bash, Glob, Grep, LS). Only `Read` (single file),
`Write`, `Edit`, `NotebookEdit`, `WebFetch`, `WebSearch`, `TaskStop` are available.

**How to apply**:
- Discover existing domain conventions by directly `Read`-ing known/likely file paths
  (e.g. `src/main/java/com/jk/amazon2/category/entity/Category.java`) based on the
  package structure documented in `harnesses/README.md` and `CLAUDE.md`, rather than
  trying to list directories.
- The project filesystem is case-insensitive (macOS/APFS) — a `Read` with wrong casing
  in the path can still succeed and return the real file. Don't rely on this, but don't
  be surprised by it either.
- `Read` occasionally suggests a similarly-named existing file ("Did you mean
  application-test.properties?") when the exact path doesn't exist — this only
  triggers for close string matches, not a general directory listing, so it's not a
  reliable way to enumerate a directory's contents (e.g. it did NOT help find the
  exact Flyway migration filenames in `src/main/resources/db/migration/`).
- For anything that genuinely requires a directory listing (e.g. determining the next
  free Flyway migration version number), do not guess silently — surface it to the
  user as a manual step instead of fabricating a filename.
