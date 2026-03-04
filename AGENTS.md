# AGENTS Working Rules

## Development Rules

- Always use test-first development (TDD).
- Keep commits small and single-purpose.
- Do not claim completion without running verification commands.
- Use package prefix `com.club.sla` for Java sources.
- Keep file naming explicit and avoid abbreviations unless domain-standard.

## Verification Rules

- Run `./scripts/check.sh` before commit.
- For API changes, include at least one controller test.
- For persistence changes, include integration tests when feasible.

## Collaboration Rules

- Document design decisions in `docs/adr/`.
- Keep runbooks in `docs/runbooks/` updated with operational changes.
