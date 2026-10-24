# DevDataAPI

**Realistic data. Ready REST APIs. LLM-ready.**

DevDataAPI is an early-stage, file-backed REST API for local development and API prototyping. It starts with synthetic JSON data so a frontend, integration test, or developer can exercise common endpoints without provisioning a database.

> **Status:** early preview (`v0.1.0`). This is a development tool, not a production database or production-ready service. Review the known limitations before using it with important data or exposing it to a network.

## What is available today

- Explicit, typed CRUD endpoints for **users**, **products**, and **orders**.
- JSON files under a configurable data directory; the default is `./data` relative to the process working directory.
- Reads aggregate matching resource JSON files. Duplicate IDs are logged, and the last matching occurrence in file order is used.
- Numeric IDs are assigned by the resource repositories when creating records.
- OpenAPI and Swagger UI documentation.
- Synthetic seed datasets for users, products, orders, customers, departments, employees, addresses, countries, cities, posts, comments, courses, students, books, and authors. **Only users, products, and orders currently have API controllers.**

## Requirements

- Java 25 or later to run the application.
- Git is optional if downloading a release JAR; the Gradle wrapper is included for building from source.

## Quick start

### Download a release

1. Open the [GitHub Releases page](https://github.com/codefarm0/dev-data-api/releases) and download the latest `dev-data-api-<version>.jar`.
2. From a working directory where you want the application data to live, run:

   ```bash
   java -jar /path/to/dev-data-api-<version>.jar
   ```

The server listens on port **8081**. The default data directory is `./data` relative to the directory from which the JAR is launched. Set `DEVDATAAPI_DATA_DIR` to use a different path. The application reads and writes JSON files there; back up data before experimenting.

### Run from source

```bash
./gradlew bootRun
```

This also uses `./data` relative to the project directory unless `DEVDATAAPI_DATA_DIR` is set.

## API overview

The current API supports the following routes for each of the three resources:

| Method | Route | Behavior |
| --- | --- | --- |
| `GET` | `/api/users` (also `/api/products`, `/api/orders`) | List records |
| `GET` | `/api/users/{id}` (also products/orders) | Get one record |
| `POST` | `/api/users` (also products/orders) | Create a record; the server assigns its ID |
| `PUT` | `/api/users/{id}` (also products/orders) | Update a record |
| `PATCH` | `/api/users/{id}` (also products/orders) | Update a record using the current update request shape |
| `DELETE` | `/api/users/{id}` (also products/orders) | Delete a record |

List endpoints currently return the full resource list; pagination, filtering, and sorting are not implemented yet.

Examples:

```bash
curl http://localhost:8081/api/users
curl http://localhost:8081/api/products/1
curl http://localhost:8081/swagger-ui/index.html
```

Create a user:

```bash
curl -X POST http://localhost:8081/api/users \
  -H 'Content-Type: application/json' \
  -d '{"firstName":"Ava","lastName":"Nguyen","email":"ava.nguyen@example.test","status":"ACTIVE"}'
```

Create a product:

```bash
curl -X POST http://localhost:8081/api/products \
  -H 'Content-Type: application/json' \
  -d '{"name":"Notebook","category":"stationery","status":"ACTIVE","price":4.50}'
```

## Data and current limitations

- JSON is the storage format; there is no database, multi-instance coordination, authentication, or authorization in this release.
- The default configuration listens on port `8081`; do not expose this unauthenticated preview service to an untrusted network.
- Extra seed folders are sample data only. They do not imply that those resources are available through REST.
- Duplicate IDs across matching JSON files are resolved using last occurrence wins, with a startup warning for users, products, and orders.
- The current storage implementation writes mutations to a resource-level JSON file while also reading seed/batch files. In particular, deleting an ID that remains in a seed file can cause that record to reappear on a later read or restart. Treat the current storage behavior as experimental and do not use it as durable storage for valuable data.
- Request validation, consistent structured error responses, pagination, query filters, and safe multi-process writes are not complete.

These limitations are prioritized in the [roadmap](docs/roadmap.md). Please report bugs through [GitHub Issues](https://github.com/codefarm0/dev-data-api/issues).

## Build a JAR locally

```bash
./gradlew clean bootJar -PreleaseVersion=0.1.0
```

The executable JAR is created at `build/libs/dev-data-api-0.1.0.jar`.

## Releases and versioning

Releases are published from version tags such as `v0.1.0`. Pushing a `v*` tag triggers the GitHub Actions release workflow, which builds the executable JAR and attaches it to a GitHub Release. Pushes to `main` create build artifacts for inspection but do not publish a GitHub Release.

Before each feature release, update the [changelog](CHANGELOG.md), add release notes under `docs/releases/`, verify the documented behavior and artifact, then tag the release. See the [release checklist](docs/releasing.md). Releases before `1.0.0` may change behavior as the project develops; each release should document compatibility changes and known issues.

## Project direction

The goal is to grow DevDataAPI incrementally into a useful local API and test-data platform. The roadmap is prioritized around storage correctness first, then API usability and resource coverage, followed by optional security/testing features and agent integrations. All roadmap items are plans, not promises or delivered functionality.

## Documentation

- [Product and engineering plan](docs/devdataapi-complete-plan.md)
- [Roadmap](docs/roadmap.md)
- [Changelog](CHANGELOG.md)
- [Release checklist](docs/releasing.md)
- [v0.1.0 release notes](docs/releases/v0.1.0.md)

## License

See [LICENSE](LICENSE).

