# Product roadmap

This roadmap is a prioritization guide, not a delivery commitment. Work is sequenced to address correctness before expanding the API surface. Each release should update `CHANGELOG.md` and include versioned notes under `docs/releases/`.

## Current: v0.1.0 — Preview

Delivered: typed CRUD for users, products, and orders; JSON-backed data; seed datasets; duplicate-ID warnings; OpenAPI/Swagger; GitHub release artifacts.

Known limitations are listed in the [README](../README.md) and [v0.1.0 release notes](releases/v0.1.0.md). This early preview is not recommended for valuable or production data.

## Next: storage correctness and API contract

Priority: make existing behavior predictable before adding resource types.

- Define how seed data, batch files, and mutations combine; implement updates and deletes so removed or changed seed records do not silently return.
- Make writes safe against interruption and concurrent requests within one process; document the single-process boundary.
- Validate request bodies and return consistent, useful HTTP errors.
- Add integration coverage for CRUD, persistence, duplicate IDs, and restart behavior.
- Document the current API request/response contract and add meaningful OpenAPI examples.

**Exit criteria:** CRUD changes remain visible across reads and restarts; behavior is documented and covered by integration tests.

## Then: API usability and data quality

- Add bounded pagination with a configurable maximum page size.
- Add documented filtering and sorting for explicitly supported fields.
- Validate data relationships and uniqueness rules where the domain supports them.
- Clarify seed-file layout, ordering, and duplicate handling.
- Add reproducible examples for users, products, and orders.

**Exit criteria:** clients can query larger datasets predictably without unbounded responses.

## Later: resource catalog and operational features

- Add explicit CRUD resources based on demand, starting with customers and employees only after shared storage behavior is stable.
- Add backup/export and import with validation and clear overwrite protections.
- Add health/readiness endpoints and basic request/storage metrics.
- Add optional testing features such as deterministic latency/error simulation and ETags.

**Exit criteria:** additional capabilities are opt-in, documented, and do not compromise stored data.

## Future: security and agent integrations

- Add configurable API-key authentication and authorization before supporting remote use.
- Improve OpenAPI schema discovery and client examples.
- Evaluate an optional MCP adapter that calls the same application services as REST.
- Add SDK and CI integration examples based on real user needs.

**Exit criteria:** security controls are tested; agent integrations respect the same authorization and validation as REST.

## Release principles

- Prefer small, focused releases with a clear user-visible outcome.
- Do not label a feature complete until its behavior, limitations, examples, and tests are in place.
- Include compatibility changes, migration guidance, and known issues in release notes.
- Avoid production-readiness claims until storage recovery, concurrency, security, and operational behavior are verified.
