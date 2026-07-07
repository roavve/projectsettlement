# CLAUDE.md

Guidance for working in this repository. Read this before making changes — it documents the
project's **actual** conventions (which sometimes differ from generic Spring Boot best practice).
When in doubt, match the surrounding file rather than "fixing" it.

## What this is

A monolithic **Spring Boot 3.3.13 / Java 17** REST API (`com.example.mssqll`, artifact
`docu-prod`) for managing **connection-fee settlement** data extracted from Excel files.
Backend DB is **MS SQL Server**; security is **stateless JWT**; Excel import/export via Apache
POI and FastExcel; async exception alerts go to **Slack**.

## Build & run

```bash
mvn clean install            # build + run tests
mvn spring-boot:run          # run (default profile is 'dev')
mvn test                     # tests only
mvn -Dtest=SaveFeeTest test  # single test class
```

- Active profile is set in `application.properties` → `spring.profiles.active=dev`.
  Real config lives in `application-dev.properties` / `application-prod.properties`
  (datasource, JWT key, `upload.directory`, `slack.webhook.url`, CORS origins).
- Lombok is used heavily and is excluded from the repackaged jar (see `pom.xml`).

## Architecture & packages

Classic layered flow:

```
controller  →  service (interface)  →  service.impl  →  repository / customRepository  →  models (JPA)
          ↘  dto/{request,response}           ↘  specifications        ↘  converter
          ↘  utiles (exceptions, resonse [sic], SecurityUtils)
```

Package root `com.example.mssqll`:
`configuration` · `controller` (+ `Auth`, `File`) · `converter` · `dto/{request,response}` ·
`filter` · `models` · `providers` · `repository` · `service` (+ `impl`) · `specifications` ·
`utiles` (+ `exceptions`, `resonse`).

## Conventions (follow these)

### Dependency injection
- Preferred: constructor injection via Lombok **`@RequiredArgsConstructor`** on `final` fields
  (used by controllers and `@Configuration`).
- **Known quirk:** `ConnectionFeeServiceImpl` and `UserController` redundantly combine
  `@Autowired` + `final` fields + a hand-written constructor. Don't copy this into new files;
  but when editing those files, stay consistent with what's already there.

### Services
- Interface in `service/`, implementation `service/impl/<Name>ServiceImpl` with `@Override` on
  every method. Annotate impl with `@Service` + `@Slf4j`.
- Transactions: **`@Transactional(rollbackFor = Exception.class)` at the method level** on
  mutating operations (e.g. `saveFee`). Read methods are left untransacted. Not class-wide.
- Business logic and **validation live in the service**, not the controller or DTO.

### Controllers
- `@RestController` + `@RequestMapping("/api/v1/...")` + `@Slf4j` + Swagger `@Tag`. Keep thin.
- Authorization is **method-level** `@PreAuthorize("hasAnyRole('ADMIN','MANAGER','OPERATOR')")`.
- Pagination input is **1-based**, normalized to 0-based: `int adjustedPage = (page < 1) ? 0 : page - 1;`
- Response types are **inconsistent** across the codebase (`ResponseEntity<T>`, the
  `ApiResponse<T>` builder, and raw `Map`/`PagedModel`). **Mirror the neighbouring endpoints in
  the same controller** rather than introducing a new style.

### Logging
- Lombok `@Slf4j`. Every endpoint and significant service action logs.
- **Idiom:** append the actor to every message using the statically-imported
  `getCurrentUsername()` from `utiles.SecurityUtils`, e.g.
  `log.info("Fetching ... page: {} (requested by {})", page, getCurrentUsername());`
- `log.info` for actions, `log.warn` for not-found/rejected input, `log.error(..., ex)` (pass the
  throwable) for failures before rethrow/rollback.

### Exceptions
- Throw the custom unchecked exceptions in `utiles.exceptions` (each extends `RuntimeException`
  with a single `(String message)` constructor). Reuse an existing one before creating a new type.
- Standard idiom: `.orElseThrow(() -> new ResourceNotFoundException("..."))`.
- All handling is centralized in `utiles.exceptions.GlobalExceptionHandler` (`@ControllerAdvice`):
  each handler logs, fires `WebhookNotifierService.sendExceptionNotification(ex)` (async Slack),
  and returns `ErrorResponse { message, exception }` with a mapped `HttpStatus`. A catch-all
  `@ExceptionHandler(Exception.class)` returns 500. **New exception types need a handler here.**

### Entities (`models/`)
- `@Entity` + `@Table(name=..., indexes={...})`, Lombok `@Data @Builder @AllArgsConstructor`
  (+ explicit constructors where present).
- **String columns use Hibernate `@Nationalized`** (NVARCHAR for Georgian text) — keep this on any
  new string column.
- **Enum persistence is mixed:** `orderStatus` is `@Enumerated(ORDINAL)`, `status` is
  `EnumType.STRING`. ⚠️ Because `OrderStatus` is stored by **ordinal**, **never reorder or insert
  enum constants in the middle of `OrderStatus`** — append only, or you corrupt existing rows.
  (`OrderStatusConverter` mirrors the ordinal mapping.)
- Soft delete = `Status.SOFT_DELETED` + `@Where(clause = "status != 'SOFT_DELETED'")`, not row
  removal. Self-referential `parent`/`children` tree (`@JsonIgnore` on `parent`).
- **No JPA auditing** — audit fields (`transferPerson`, `changePerson`, `transferDate`,
  `changeDate`, `clarificationDate`) are set **manually in service code**.
- `User` implements `UserDetails` directly; `getUsername()` returns the email, and the `User`
  entity itself is the security principal.

### DTOs & mapping
- DTOs are plain Lombok POJOs: `@Data @Builder @NoArgsConstructor @AllArgsConstructor`, with
  Swagger `@Schema(implementation = ...)` on enum fields. **No Bean Validation** (`@Valid`/`@NotNull`)
  is used — request bodies are unvalidated; validate in the service.
- **Mapping is fully manual (no MapStruct).** Reuse the private helpers in
  `ConnectionFeeServiceImpl` (`baseCast`, `castToDto`, `castToDtos`, `castUserToDto`,
  `convertToDto`) instead of writing new mapping logic.

### Repositories & filtering
- `@Repository interface ... extends JpaRepository<T, Long>, JpaSpecificationExecutor<T>`; mix of
  derived queries, `@Query` JPQL, `@Modifying @Transactional` bulk ops, native CTE queries, and
  `@EntityGraph` fetch tuning.
- **Two filtering mechanisms exist; the active one is the JDBC custom repo.** The live `/filter`
  and `/download` flows use `ConnectionFeeCustomRepository` (a `@Repository` **class** on
  `JdbcTemplate`) via `letDoFilterCustom` / `getExportData`. It whitelists sortable columns through
  `SORT_COLUMN_MAP` (SQL-injection guard) and uses SQL Server `STRING_AGG` / `OFFSET..FETCH`.
  `ConnectionFeeSpecification` + `JpaSpecificationExecutor` is the **older/alternate** path.
  **→ Add new connection-fee filtering to the JDBC custom repo, and add new sort keys to
  `SORT_COLUMN_MAP`.**

### Security
- `SecurityConfig`: `@EnableWebSecurity @EnableMethodSecurity`, CSRF off, `STATELESS` sessions,
  `DaoAuthenticationProvider` + `AuthenticationManager`. Public endpoints: `/api/v1/auth/signin`,
  `/api/v1/auth/refresh-token`, actuator health/prometheus, swagger; all else `authenticated()`.
- `JwtAuthenticationFilter extends OncePerRequestFilter @Order(1)`: extracts Bearer token, checks a
  token blacklist, validates via `JwtService`, sets the `SecurityContextHolder`, and writes a JSON
  `{status,error,message}` on failure. Use `SecurityUtils` (`getCurrentUsername()`,
  `getCurrentUser()`) to read the principal — don't re-derive it.

### Testing
- **JUnit 5 + Mockito + AssertJ.** `@ExtendWith(MockitoExtension.class)`, `@Mock` deps,
  `@InjectMocks` SUT — **pure unit tests, no `@SpringBootTest`/Spring context.**
- Set `SecurityContextHolder` manually in `@BeforeEach` with a built `User`, clear it in
  `@AfterEach`.
- Naming: `methodUnderTest_scenario_expectedOutcome`
  (e.g. `saveFee_taskNotFound_throwsResourceNotFoundException`). Test class mirrors the impl
  package. See `src/test/java/com/example/mssqll/service/impl/SaveFeeTest.java`.

## Reusable building blocks (prefer these)

- `utiles.SecurityUtils` — current user/username helpers.
- `utiles.resonse.ApiResponse<T>` / `ApiResponseUnit<T>` — `@Builder` response wrappers.
- `service.WebhookNotifierService.sendExceptionNotification(ex)` — async Slack alert (already wired
  into the global handler).
- `ConnectionFeeCustomRepository` + `SORT_COLUMN_MAP` — dynamic filtering/export.
- DTO-mapping helpers in `ConnectionFeeServiceImpl`.
- Existing custom exceptions in `utiles.exceptions`.

## Domain quick reference

`OrderStatus` (stored by **ordinal** — append-only):
`ORDER_COMPLETE(0)`, `ORDER_INCOMPLETE(1)`, `CANCELED(2)`, `YELLOW_AMOUNT(3)`, `IN_PROGRESS(4)`,
`RETURNED(5)`. UI labels (Georgian): შევსებული / გამოუყენებელი / გაუქმებული / დასასრულებელი /
პროცესში / დაბრუნებული.

`ConnectionFee.updateFee` contains non-trivial `clarificationDate` business rules — see
`clarificationDate-პირობები.md` in the repo root for the documented conditions.

## Known inconsistencies (don't mass-refactor; just be aware)

- Mixed controller response styles (`ResponseEntity` vs `ApiResponse` vs `Map`); `ApiResponseUnit`
  largely unused.
- Redundant `@Autowired` + `final` + explicit constructor in `ConnectionFeeServiceImpl` /
  `UserController`.
- No Bean Validation on request DTOs.
- Two parallel filter mechanisms (Specification vs JDBC custom repo).
- Package name misspelled `utiles.resonse`; root package `utiles`.
- Mixed enum persistence (ORDINAL vs STRING) within `ConnectionFee`.
- One stray `System.out.println` in `UserController`.

## Working rules for changes

- Make the smallest, most consistent change; match the surrounding file's style.
- Keep controllers thin, business logic in services, repositories for data access only.
- Respect existing transaction boundaries; preserve backward compatibility unless asked otherwise.
- Don't add libraries, introduce new abstractions, or refactor unrelated code without being asked.
- Don't invent classes/services/utilities that don't exist — reuse what's listed above.
