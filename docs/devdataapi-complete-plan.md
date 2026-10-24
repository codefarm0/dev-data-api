# DevDataAPI --- Complete Product & Engineering Plan

**DevDataAPI by Codefarm** is a standalone, file-backed REST API
platform that gives developers, automated tests, and LLM-powered coding
agents instant access to realistic data and usable API endpoints ---
without setting up a database or building a backend first.

**Product tagline:** *Realistic data. Ready REST APIs. LLM-ready.*

The central design decision is to make DevDataAPI a **generic API engine
driven by resource definitions**, not a collection of individually
implemented controllers for users, products, employees, and every future
resource. The same engine should serve HTTP clients, tests, and AI
integrations.

## 1. Product Goals and Boundaries

### Primary goals

-   Start with `java -jar devdataapi.jar`.
-   Provide useful APIs immediately, with no manual database setup.
-   Persist created, updated, and deleted records across restarts.
-   Ship realistic, internally consistent seed data.
-   Support normal REST clients, Postman, frontend developers,
    integration tests, and AI coding agents.
-   Allow adding new resource categories through configuration and seed
    data wherever possible.
-   Provide secure, configurable authentication for local and remote
    use.
-   Release versioned, downloadable JARs through GitHub Releases.

### Non-goals for the first release

-   Replacing a production database.
-   Distributed multi-writer storage across multiple server instances.
-   Arbitrary user-defined Java code or scripts.
-   Supporting every possible query language.
-   Requiring an LLM provider or API key to use the core application.

**Important boundary:** JSON files are the source of truth. Indexes and
caches are derived data that can be rebuilt. DevDataAPI is initially a
single-process, single-writer server with bounded memory and measurable
dataset limits.

## 2. Architecture

``` mermaid
flowchart TB
    Client["REST clients / Postman / Frontends"]
    Tests["Automated tests"]
    Agents["LLM agents / MCP clients"]

    HTTP["Spring Boot REST API"]
    MCP["Optional MCP adapter"]
    Security["Authentication and authorization"]
    Metadata["Resource registry and schemas"]
    Service["Generic resource services"]
    Query["Filtering / sorting / pagination"]
    Storage["File-backed storage engine"]
    Index["Rebuildable indexes"]
    Journal["Write journal and recovery"]
    Files["JSON batch files on disk"]
    OpenAPI["OpenAPI and API discovery"]

    Client --> HTTP
    Tests --> HTTP
    Agents --> HTTP
    Agents --> MCP
    HTTP --> Security
    MCP --> Security
    Security --> Service
    Service --> Metadata
    Service --> Query
    Query --> Storage
    Storage --> Index
    Storage --> Journal
    Storage --> Files
    HTTP --> OpenAPI
    MCP --> Service
```

### Architectural rules

1.  **One application service layer.** REST and MCP must call the same
    validated business operations.
2.  **Metadata-driven resources.** Generic controllers should not
    contain resource-specific branching.
3.  **Storage abstraction.** Application services must not manipulate
    filesystem paths directly.
4.  **Explicit write lifecycle.** Validate, lock, journal, write,
    commit, update indexes, then respond.
5.  **Secure by default.** Bind to localhost by default, limit payloads,
    and require explicit configuration for remote exposure.
6.  **Observable startup.** Do not accept normal traffic until seed
    extraction, recovery, and index initialization finish.

## 3. Technology Stack

  -----------------------------------------------------------------------
  Area                                Choice
  ----------------------------------- -----------------------------------
  Language                            Java 25 LTS as a stable baseline;
                                      evaluate newer JDKs separately

  Framework                           Current stable Spring Boot release
                                      compatible with the selected JDK

  Build                               Gradle

  Web                                 Spring MVC and Jackson

  Validation                          Jakarta Bean Validation

  API documentation                   Springdoc OpenAPI, exposing OpenAPI
                                      3.1 where supported

  Storage                             JSON files, with records grouped
                                      into batches

  Indexing                            In-memory ID index and selective
                                      secondary indexes; persisted
                                      snapshots optional

  Security                            Spring Security, API keys, JWT
                                      validation, configurable mock auth

  Testing                             JUnit 5, Spring Boot Test,
                                      integration tests and concurrency
                                      tests

  Packaging                           Executable Spring Boot JAR

  CI/CD                               GitHub Actions and GitHub Releases

  Metrics                             Spring Boot Actuator and Micrometer

  AI integration                      OpenAPI first; optional MCP server
                                      adapter in a later phase
  -----------------------------------------------------------------------

Avoid adding a database, Redis, or another mandatory external dependency
to the core distribution.

## 4. File-Backed Storage Design

This is the most important engineering area. The storage engine must be
designed and tested before adding a large number of resources.

### Directory layout

``` text
devdataapi/
├── devdataapi.jar
├── application.yml
└── data/
    ├── users/
    │   ├── users-000001.json
    │   ├── users-000002.json
    │   └── resource-meta.json
    ├── products/
    │   ├── products-000001.json
    │   └── resource-meta.json
    ├── employees/
    ├── departments/
    ├── customers/
    ├── orders/
    └── _system/
        ├── indexes/
        ├── journal/
        ├── metadata/
        └── backups/
```

### Batch file format

Each JSON file contains an array of complete records rather than one
file per record.

Example `data/users/users-000001.json`:

``` json
[
  {
    "id": 1001,
    "firstName": "Aarav",
    "lastName": "Sharma",
    "email": "aarav.sharma@example.test",
    "departmentId": 10,
    "jobTitle": "Software Engineer",
    "active": true
  },
  {
    "id": 1002,
    "firstName": "Meera",
    "lastName": "Verma",
    "email": "meera.verma@example.test",
    "departmentId": 20,
    "jobTitle": "Product Analyst",
    "active": true
  }
]
```

### Batch policy

-   Default: **1,000 records per file**, configurable per resource.
-   Minimum design target: **10 records per file** where practical;
    never manufacture fake records to fill a batch.
-   The last batch can contain fewer records.
-   Batch size must be benchmarked against listing, writes, memory,
    startup time, and recovery.
-   Resource IDs must be unique across all batches.
-   New records go into an available batch with capacity or a newly
    created batch.
-   Updates and deletes rewrite the affected batch using a crash-safe
    protocol.

### Indexing strategy

The engine should maintain:

-   **ID index:** resource ID → batch ID.
-   **Secondary indexes:** optional mappings for common equality
    filters, such as email, category, status, or department ID.
-   **Batch metadata:** record count, format version, and integrity
    information.
-   **Index snapshots:** optional persisted snapshots to reduce startup
    work.

A lookup can resolve the relevant batch without scanning every file, but
with a JSON array it still needs to parse that batch to locate the
target record unless a separate record-offset strategy is implemented.
The first version should favor correctness and simple recovery over
premature byte-offset optimization.

### Safe write protocol

For every mutation:

1.  Validate the request and enforce payload limits.
2.  Acquire the required in-process resource/batch lock.
3.  Verify uniqueness and any conditional-write requirements.
4.  Record enough information in a journal to recover an interrupted
    mutation.
5.  Write the updated batch to a temporary file.
6.  Flush and atomically replace the original where supported.
7.  Mark the journal operation committed.
8.  Update or invalidate the affected indexes.
9.  Release the lock and return the HTTP response.

The implementation must document filesystem-specific atomic-move
limitations and include recovery behavior for interrupted writes. Do not
assume that a rename alone solves every crash-consistency problem.

### Persistence and bootstrap rules

-   Extract bundled seed files into the sibling `data/` directory on
    first startup.
-   **Never overwrite an existing user data file on restart or
    upgrade.**
-   Allow missing new seed files to be added during upgrades without
    replacing existing data.
-   Validate file format and supported schema versions.
-   Rebuild indexes if missing, corrupt, or inconsistent.
-   Stop startup with an actionable error if source data cannot be
    recovered safely.
-   Allow a configurable external data directory.
-   Support backup/export before destructive operations.

### Concurrency and scale boundaries

-   Support concurrent HTTP requests inside one running process.
-   Prevent duplicate ID allocation and lost updates through appropriate
    locking.
-   Initially, only one DevDataAPI process may own a data directory.
-   Reject or document unsupported multi-process access rather than
    pretending JVM locks coordinate multiple processes.
-   Bound list sizes, scan budgets, request payloads, and memory usage.
-   Benchmark large datasets before advertising supported record counts.

## 5. Resource Catalog and Seed Data

The resource catalog should be broad enough to support common
application development scenarios and rich enough that frontend
developers and AI coding assistants can use it without inventing missing
fields.

### Initial resource categories

  -----------------------------------------------------------------------
  Category                            Example attributes
  ----------------------------------- -----------------------------------
  `users`                             Names, email, phone, address,
                                      roles, status, preferences,
                                      timestamps

  `products`                          SKU, title, description, category,
                                      price, currency, inventory,
                                      variants, dimensions, tags

  `employees`                         Employee number, title, department,
                                      manager, salary band, location,
                                      hire date, status

  `departments`                       Name, code, manager, location,
                                      employee count or linked employees

  `companies`                         Name, industry, size, website,
                                      contact details, address

  `customers`                         Contact details, addresses, loyalty
                                      tier, preferences, account status

  `orders`                            Customer, line items, quantities,
                                      prices, shipping address, status,
                                      timestamps

  `addresses`                         Street, city, state, postal code,
                                      country, address type

  `countries`                         ISO codes, names, regions,
                                      currencies

  `cities`                            Name, country, region, postal
                                      metadata

  `posts`                             Title, body, author, tags, status,
                                      publication time

  `comments`                          Post, author, body, status,
                                      timestamps

  `courses`                           Title, description, instructor,
                                      category, duration, lessons

  `students`                          Profile, enrolled courses, status,
                                      enrollment dates

  `books`                             ISBN, title, author, publisher,
                                      genre, publication year,
                                      availability

  `authors`                           Name, biography, nationality, books
  -----------------------------------------------------------------------

Start with 8--10 well-designed resource categories for the first
release, then add the rest through data packs.

### Data quality requirements

-   Use synthetic data and reserved example domains, such as
    `example.test`.
-   Use consistent ID types and unique identifiers.
-   Keep foreign keys valid: orders reference existing customers, order
    lines reference products, employees reference departments.
-   Use realistic timestamps, statuses, prices, addresses, and optional
    fields.
-   Include nested structures and arrays where they reflect actual
    domain models.
-   Include edge cases in dedicated test fixtures: null or optional
    fields, inactive users, empty results, out-of-stock products,
    cancelled orders, and boundary dates.
-   Keep generated seed data deterministic and reproducible.

Provide a small default dataset in the JAR. Larger datasets should be
optional downloadable data packs or importable files so the base JAR
does not become unnecessarily large.

## 6. REST API Contract

Use a predictable base path: `/api`.

### Core endpoints

  Method     Endpoint            Behavior
  ---------- ------------------- -------------------------
  `GET`      `/api/users`        List users
  `GET`      `/api/users/{id}`   Fetch one user
  `POST`     `/api/users`        Create a user
  `PUT`      `/api/users/{id}`   Replace a user
  `PATCH`    `/api/users/{id}`   Partially update a user
  `DELETE`   `/api/users/{id}`   Delete a user

The same generic routes should work for every registered resource.

### Query capabilities

Examples:

``` http
GET /api/users?page=0&size=20
GET /api/users?sort=lastName,asc
GET /api/products?category=electronics
GET /api/products?price_gte=100&price_lte=500
GET /api/users?search=meera
GET /api/orders?customerId=1001&status=SHIPPED
GET /api/users?fields=id,firstName,email
```

Define a documented filter grammar instead of accepting arbitrary query
expressions. Support:

-   Exact match and a controlled set of comparison operators.
-   Search over explicitly searchable fields.
-   Multi-field sorting with stable ID tie-breaking.
-   Bounded offset pagination and a maximum page size.
-   Cursor pagination for large datasets when needed.
-   Optional total counts, since calculating totals can be expensive.
-   Field selection with strict validation.
-   Resource-specific allowed filters and sortable fields.

### Response and error standards

-   `200 OK`: successful reads and updates returning a representation.
-   `201 Created`: successful creation, including a `Location` header.
-   `204 No Content`: successful deletion or update without a response
    body.
-   `400 Bad Request`: malformed requests or invalid filters.
-   `401 Unauthorized`: missing or invalid authentication.
-   `403 Forbidden`: insufficient permission.
-   `404 Not Found`: unknown resource or record.
-   `409 Conflict`: duplicate IDs, uniqueness violations, or
    incompatible state.
-   `412 Precondition Failed`: failed conditional writes.
-   `413 Content Too Large`: payload limit exceeded.
-   `429 Too Many Requests`: configured rate limit exceeded.
-   `500 Internal Server Error`: unexpected failure without leaking
    stack traces.

Use one structured error format throughout the application, preferably
aligned with RFC 9457 Problem Details, with an application-specific
error code and a correlation ID.

### Additional endpoints

``` text
GET  /api/_meta
GET  /api/_meta/resources
GET  /api/_meta/resources/{resource}/schema
GET  /actuator/health
GET  /v3/api-docs
GET  /swagger-ui/index.html
```

Resource discovery must report supported methods, schema, queryable
fields, relationships, and authentication requirements. Documentation
endpoints should reflect enabled capabilities and must not reveal
secrets or internal filesystem paths.

### Relationships and bulk operations

Add after core CRUD:

``` http
GET  /api/customers/{id}/orders
GET  /api/orders/{id}/items
POST /api/users/bulk
PATCH /api/products/bulk
```

Bulk operations need explicit maximum item counts, per-item outcomes,
validation rules, and documented transaction semantics. Do not imply
that a multi-file batch is atomic unless it truly is.

## 7. Generic Application Design

Suggested package layout:

``` text
in.codefarm.devdataapi
├── api
│   ├── resource
│   ├── metadata
│   └── error
├── application
│   ├── resource
│   ├── query
│   ├── validation
│   └── auth
├── domain
│   ├── resource
│   ├── identity
│   └── errors
├── storage
│   ├── batch
│   ├── index
│   ├── journal
│   ├── locking
│   └── bootstrap
├── security
│   ├── apikey
│   ├── jwt
│   └── mockauth
├── testing
│   ├── latency
│   ├── errorsimulation
│   └── ratelimit
├── integrations
│   ├── openapi
│   └── mcp
├── observability
└── config
```

### Main components

-   `GenericResourceController`: dispatches generic CRUD requests.
-   `ResourceRegistry`: resolves configured resource definitions.
-   `ResourceDefinition`: fields, ID rules, relationships, and allowed
    operations.
-   `ResourceSchema`: validation and machine-readable field definitions.
-   `ResourceService`: common create/read/update/delete orchestration.
-   `QueryService`: validated filtering, sorting, pagination, and
    search.
-   `IdGenerationService`: generates IDs safely for each configured ID
    type.
-   `JsonFileRepository`: storage abstraction over batches.
-   `FilePathResolver`: derives safe, server-controlled file paths.
-   `AtomicFileWriter`: temporary-file replacement and durability
    behavior.
-   `FileLockManager`: coordinates concurrent operations.
-   `JournalManager`: mutation recovery.
-   `IndexManager`: loads, validates, updates, and rebuilds indexes.
-   `DataInitializer`: extracts missing seed files without overwriting
    existing data.
-   `GlobalExceptionHandler`: maps failures to consistent HTTP
    responses.

### Resource registration

A new resource should usually require only a definition, a schema, and
seed data---not a new controller or service.

Illustrative configuration:

``` yaml
devdataapi:
  resources:
    users:
      id-field: id
      id-type: long
      batch-size: 1000
      queryable-fields:
        - email
        - departmentId
        - active
      searchable-fields:
        - firstName
        - lastName
      sortable-fields:
        - firstName
        - lastName
        - createdAt
      unique-fields:
        - email
```

The exact schema format should be finalized during implementation.
Resource definitions must be validated at startup, and unsupported
operators or fields must fail clearly rather than silently being
ignored.

## 8. Authentication and Security

DevDataAPI should support both frictionless local testing and realistic
API security scenarios.

### Authentication modes

  Mode          Intended use
  ------------- --------------------------------------------------------
  `none`        Local-only development with no authentication
  `api-key`     Simple service-to-service testing
  `jwt`         Validate externally issued bearer tokens
  `mock-auth`   Simulate login, registration, refresh, and roles
  `hybrid`      Explicitly configured combination of supported methods

### Mock authentication endpoints

``` http
POST /api/auth/register
POST /api/auth/login
POST /api/auth/refresh
POST /api/auth/logout
GET  /api/auth/me
```

Mock users, roles, access tokens, refresh-token rotation, expiration,
and invalidation should be configurable. Clearly label mock
authentication as a simulation---not a production identity provider.

For JWT validation, verify signatures, expiry, issuer, and audience as
configured. Never accept a token merely because it contains plausible
claims.

### Security controls

-   Default bind address: `127.0.0.1`.
-   Remote binding must be an explicit configuration choice.
-   Never commit API keys, JWT secrets, or signing keys into the
    repository.
-   Use request-body size limits and strict JSON parsing.
-   Prevent path traversal and filesystem access through
    client-controlled paths.
-   Apply CORS only to explicitly configured origins.
-   Avoid logging tokens, credentials, or unnecessary personal data.
-   Apply authorization consistently to REST and MCP calls.
-   Provide configurable roles and endpoint permissions.
-   Make destructive reset operations opt-in and protected.
-   Document that mock auth and synthetic data are for
    development/testing.

## 9. LLM and AI-Agent Integration

AI readiness should be a core product capability, not an afterthought,
but the base server should work without an LLM account or external AI
service.

### Layer 1 --- OpenAPI and discovery (MVP)

-   Publish machine-readable API definitions.
-   Expose resource names, field schemas, relationships, allowed
    operations, and filter syntax.
-   Use descriptive operation IDs and useful request/response examples.
-   Return predictable, structured errors.
-   Bound result sizes and enforce normal authorization.
-   Ensure an AI coding assistant can discover the API and generate
    correct HTTP requests from its contract.

### Layer 2 --- MCP server adapter

Add an optional MCP integration that exposes controlled tools such as:

``` text
list_resources
get_resource_schema
list_records
get_record
create_record
update_record
delete_record
```

The MCP adapter should call the same application services as REST. Do
not implement a second storage or validation path.

For safety:

-   Separate read tools from write tools.
-   Respect configured authentication and authorization.
-   Enforce argument schemas, payload limits, and result limits.
-   Require explicit opt-in for destructive tools.
-   Never expose arbitrary filesystem operations.
-   Return structured results and actionable errors.
-   Document the transport and client configuration needed to connect.

### Layer 3 --- AI-assisted workflows

Possible later additions:

-   Generate example requests from resource schemas.
-   Export ready-to-use curl commands and sample payloads.
-   Provide deterministic fixtures for agent-driven tests.
-   Allow agents to seed or reset isolated test scenarios under strict
    permissions.
-   Publish client configuration examples for MCP-compatible tools.

**Scope rule:** Do not make an LLM provider a runtime dependency. The
product supplies reliable APIs and tools; users choose their own model
and agent.

## 10. API Testing and Simulation Features

These features turn DevDataAPI from a static data server into a
practical backend-testing environment.

  -----------------------------------------------------------------------
  Feature                 Behavior                Priority
  ----------------------- ----------------------- -----------------------
  Configurable latency    Add deterministic or    Phase 2
                          randomized response     
                          delays                  

  Error simulation        Simulate selected       Phase 2
                          `4xx`/`5xx` responses   

  Rate limiting           Simulate `429`          Phase 2
                          responses and retry     
                          scenarios               

  ETags                   Support conditional     Phase 2
                          reads and writes        

  Bulk operations         Create, update, or      Phase 2
                          delete bounded batches  
                          of records              

  Dataset import/export   Load and export         Phase 2
                          resource data in        
                          supported JSON formats  

  Reset scenarios         Restore a known test    Phase 2
                          fixture or reset an     
                          isolated dataset        

  Request logging         Record method, path,    Phase 1
                          status, latency, and    
                          correlation ID          

  Metrics                 Request counts,         Phase 2
                          latency, errors,        
                          storage operations,     
                          index rebuilds          

  Audit events            Track mutations without Phase 2
                          exposing secrets        
  -----------------------------------------------------------------------

Every simulation must be explicitly configured, disabled by default, and
deterministic when a fixed seed or scenario is specified. Error
simulation must never accidentally corrupt stored data.

Example configuration:

``` yaml
devdataapi:
  simulation:
    latency:
      enabled: false
      min-ms: 50
      max-ms: 300
    errors:
      enabled: false
      rate: 0.05
      status-codes: [503]
    rate-limit:
      enabled: false
      requests-per-minute: 120
```

The example is illustrative; the implementation must define whether
limits apply per client, key, route, or server, and which algorithm is
used.

## 11. Configuration and Startup Lifecycle

### Configuration surface

Allow settings through a configuration file and environment variables.
The exact property names should be consistent and documented.

``` yaml
server:
  port: 8080
  address: 127.0.0.1

devdataapi:
  storage:
    directory: ./data
    batch-size: 1000
    max-page-size: 100
    max-request-bytes: 1048576

  seed-data:
    enabled: true
    overwrite-existing: false

  security:
    mode: none

  llm:
    openapi-enabled: true
    mcp-enabled: false
```

Important: `overwrite-existing: false` is a product invariant, not a
switch users should casually disable. If seed replacement or dataset
reset is ever implemented, make it a separate, explicitly authorized
operation with backup and confirmation.

### Startup sequence

``` mermaid
flowchart TD
    A["Start JAR"] --> B["Load and validate configuration"]
    B --> C["Validate storage directory and permissions"]
    C --> D["Extract missing seed files"]
    D --> E["Recover interrupted mutations"]
    E --> F["Load or rebuild indexes"]
    F --> G["Validate resource schemas and data"]
    G --> H["Start accepting requests"]
    B -. invalid config .-> X["Fail with actionable error"]
    C -. storage error .-> X
    E -. unsafe recovery .-> X
    G -. invalid data .-> X
```

Startup requirements:

-   Print the application version and effective non-secret
    configuration.
-   Report progress for large imports or index rebuilds.
-   Fail fast on invalid configuration or unrecoverable storage
    problems.
-   Expose a readiness state distinct from process liveness.
-   Do not log secret values or full sensitive payloads.
-   Shut down gracefully and coordinate in-flight writes.

## 12. GitHub Repository and Release Strategy

Suggested repository: `codefarm0/devdata-api`

``` text
dev-data-api/
├── src/
│   ├── main/
│   │   ├── java/in/codefarm/devdataapi/
│   │   └── resources/
│   │       ├── application.yml
│   │       ├── seed-data/
│   │       └── resource-definitions/
│   └── test/
├── docs/
│   ├── architecture.md
│   ├── storage-design.md
│   ├── security.md
│   ├── llm-integration.md
│   └── configuration.md
├── examples/
│   ├── curl/
│   ├── postman/
│   └── mcp/
├── scripts/
├── .github/workflows/
├── build.gradle
├── settings.gradle
├── README.md
├── LICENSE
├── CONTRIBUTING.md
└── CHANGELOG.md
```

### Release pipeline

GitHub Actions should:

1.  Compile and run unit tests.
2.  Run integration, storage-recovery, and concurrency tests.
3.  Build the executable JAR.
4.  Start the packaged JAR in a clean temporary directory.
5.  Verify initial seed extraction.
6.  Exercise create, read, update, and delete operations.
7.  Restart the JAR and verify persistence.
8.  Verify existing seed files are not overwritten.
9.  Generate checksums and attach release notes.
10. Publish the versioned artifact to GitHub Releases.

The release should include a runnable JAR, a checksum file, supported
JDK requirements, a quick-start guide, and a list of included resource
categories.

### README quick start

The intended experience should be as simple as:

``` bash
java -jar devdataapi.jar
```

Then:

``` bash
curl http://localhost:8080/api/_meta/resources
curl http://localhost:8080/api/users
curl "http://localhost:8080/api/products?page=0&size=10"
```

Document that the application creates a sibling `data/` directory and
preserves mutations across restarts. Add examples for Windows PowerShell
and Unix shells.

## 13. Testing and Quality Strategy

The storage engine requires stronger testing than a typical in-memory
mock server.

### Unit tests

-   Resource registry and schema validation.
-   ID generation and duplicate detection.
-   Query parser, filter validation, and stable sorting.
-   Pagination boundaries and maximum limits.
-   API-key and JWT validation.
-   Structured error mapping.
-   File-path safety and request-size enforcement.

### Integration tests

-   Full CRUD lifecycle for every supported resource type.
-   Seed extraction on first launch.
-   No overwrite of existing records on restart or upgrade.
-   Persistence after graceful restart.
-   Index rebuild when snapshots are missing or corrupt.
-   Recovery after an interrupted mutation.
-   Concurrent creates without duplicate IDs.
-   Concurrent updates without lost writes.
-   Validation of relationships and unique fields.
-   Authorization consistency across REST and MCP.
-   Request limits, simulated failures, and rate limiting.

### Performance and resilience tests

Measure at multiple dataset sizes, for example 10,000, 100,000, and
1,000,000 records where the test environment permits.

Measure:

-   Startup and index rebuild time.
-   Heap usage and allocation rate.
-   ID lookup latency.
-   List, filtered search, and sorted query latency.
-   Create/update/delete throughput.
-   Batch rewrite cost as batch size increases.
-   Journal recovery time.
-   Behavior under concurrent requests.
-   Performance degradation for large scans.

Do not set arbitrary performance promises before these benchmarks.
Establish baseline results, record the hardware/JDK/configuration, and
publish reproducible benchmark instructions.

## 14. Delivery Roadmap

The order matters: prove the storage engine first, then expand the API
surface.

### Phase 1 --- Core platform (MVP)

**Goal:** a useful, reliable, standalone REST server.

-   Executable JAR and configuration.
-   Seed extraction with non-overwrite semantics.
-   Batch JSON storage, locking, journal, and recovery.
-   Generic CRUD, validation, ID generation, and structured errors.
-   Pagination, sorting, filtering, and ID indexing.
-   Initial resource catalog and OpenAPI documentation.
-   CI build, integration tests, and first GitHub Release.

**Exit criterion:** a clean install can perform CRUD, restart, and
retain all changes without corrupting or overwriting data.

### Phase 2 --- Authentication and testing

-   API keys, JWT validation, mock auth, roles, and CORS.
-   Latency and error simulation, rate limits, and ETags.
-   Bulk operations, import/export, backup, and reset scenarios.
-   Metrics, request logging, and audit events.

**Exit criterion:** realistic API integration and security scenarios
work without external infrastructure.

### Phase 3 --- LLM and agent integration

-   Polished OpenAPI contract and resource discovery.
-   Machine-readable schemas and examples.
-   Optional MCP adapter sharing the same application services.
-   Read/write tool separation and permission enforcement.
-   Examples for agent clients and AI coding workflows.

**Exit criterion:** an MCP-capable client can discover resources and
safely call permitted operations.

### Phase 4 --- Scale and ecosystem

-   More rich data categories and optional dataset packs.
-   Benchmarked index and batch-size tuning.
-   Cursor pagination and optimized common filters.
-   Versioned data packs, SDK examples, and improved operational docs.

**Exit criterion:** documented dataset limits and repeatable performance
measurements.

## 15. MVP Acceptance Checklist

### Run and bootstrap

-   [ ] Runnable JAR starts with one command.
-   [ ] Creates a sibling `data/` directory on first startup.
-   [ ] Preserves existing data files across restart and upgrade.
-   [ ] Fails safely on invalid configuration or unrecoverable data.

### REST API

-   [ ] Generic CRUD works for all registered resources.
-   [ ] Pagination, filtering, sorting, and validation work.
-   [ ] Consistent HTTP status codes and structured errors.
-   [ ] OpenAPI and resource discovery are available.

### Storage integrity

-   [ ] Multiple records are stored in each JSON batch file.
-   [ ] Concurrent creates cannot produce duplicate IDs.
-   [ ] Interrupted writes have a tested recovery path.
-   [ ] Indexes can be rebuilt from source data.

### Security and AI readiness

-   [ ] Local-only default binding and request limits.
-   [ ] Authentication modes are documented and tested.
-   [ ] REST and MCP share application services when MCP is enabled.
-   [ ] Agent-facing schemas and responses are bounded and predictable.

### Delivery

-   [ ] Automated unit and integration tests pass.
-   [ ] Packaged JAR is tested in a clean directory.
-   [ ] GitHub Release includes artifact and checksums.
-   [ ] README includes quick start, configuration, and API examples.

## 16. Decisions to Lock Before Implementation

These are the initial defaults. Treat them as the baseline unless
implementation experiments show a compelling reason to change them.

  -----------------------------------------------------------------------
  Decision                            Initial default
  ----------------------------------- -----------------------------------
  Product                             DevDataAPI by Codefarm

  Repository                          `codefarm0/devdata-api`

  Build                               Gradle

  Runtime                             Java 25 LTS baseline

  Packaging                           Executable Spring Boot JAR

  Storage                             JSON arrays containing multiple
                                      records per file

  Batch size                          1,000 records by default;
                                      configurable and benchmarked

  Persistence                         Sibling `./data` directory;
                                      existing data never overwritten
                                      during seed extraction

  Indexes                             Rebuildable derived indexes

  Deployment model                    One process per data directory

  API contract                        Generic REST CRUD + OpenAPI

  AI integration                      OpenAPI first, optional MCP adapter
                                      later

  Authentication                      Local development mode plus
                                      configurable secure modes

  Initial dataset                     Rich, coherent synthetic data for
                                      core business resources

  Release                             GitHub Releases with checksums and
                                      automated verification
  -----------------------------------------------------------------------

### Recommended implementation order

Do not ask a coding agent to implement every feature in one giant pass.
Start with the storage format, atomic-write/recovery design, and
concurrency tests. Then implement generic CRUD on top of the verified
storage engine. Add the resource catalog, authentication, simulations,
and MCP integration in successive phases.

The first concrete engineering deliverable should be a **detailed
implementation specification plus a runnable skeleton**, with interfaces
and tests defined for the storage engine before adding dozens of
resource categories.

That approach gives DevDataAPI a much better chance of becoming a
reliable developer tool rather than a quick mock server that breaks once
users begin modifying data or working with larger datasets.
