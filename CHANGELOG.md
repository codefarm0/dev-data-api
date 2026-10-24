# Changelog

All notable project changes are documented here. This project follows Semantic Versioning where practical; while the version is below `1.0.0`, APIs and behavior may change between releases. Release dates use ISO 8601 (`YYYY-MM-DD`).

## [Unreleased]

### Planned

- Fix file-backed mutation semantics so updates and deletes remain consistent with seed batches across reads and restarts.
- Add request validation and consistent API error responses.
- Add bounded pagination, filtering, and sorting.
- Expand API resources only after storage behavior is reliable.

## [0.1.0] - 2026-10-25

### Added

- Initial executable Spring Boot application using Java 25 and Gradle.
- Typed user, product, and order models, request records, repositories, and explicit CRUD controllers.
- File-backed JSON data loading with aggregation across matching resource files.
- Duplicate-ID warnings and deterministic last-occurrence-wins reads for users, products, and orders.
- Server-assigned numeric IDs for newly created records.
- Synthetic seed datasets for business, geographic, content, education, and book resources.
- OpenAPI documentation and Swagger UI for `/api/**` endpoints.
- GitHub Actions workflow for building artifacts and publishing tagged releases.

### Known limitations

- REST CRUD endpoints are currently available only for users, products, and orders.
- List endpoints have no pagination, filtering, or sorting.
- Authentication, authorization, multi-process storage coordination, and production-grade recovery are not implemented.
- Mutations are experimental: because reads aggregate seed/batch files and writes use a resource-level file, deleted seed records may reappear. Do not use this release for valuable or production data.

[Unreleased]: https://github.com/codefarm0/dev-data-api/compare/v0.1.0...HEAD
[0.1.0]: https://github.com/codefarm0/dev-data-api/releases/tag/v0.1.0
