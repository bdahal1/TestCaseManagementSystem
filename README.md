# Test Case Management System

Spring Boot API (Java 26, MySQL) with a React/TypeScript frontend.

## Run locally

1. Install Java 26, Maven, Node.js 22, and MySQL. Create a `tcms` database.
2. Copy `.env.example` to `.env` in the repository root, set your database password, and replace the signing-key placeholder with the output of `openssl rand -hex 32`. Use plain `KEY=value` lines without shell quotes or `export`. Spring Boot loads this ignored local file automatically when the working directory is the repository root, including when launched from an IDE. Existing environment variables take precedence over `.env` values. If local settings have already been created, keep that file instead of replacing it.

   For deployment, set `DB_PASSWORD` and `JWT_SECRET` in the deployment environment. The local `.env` is optional, is excluded from Git, and is not bundled into the application JAR. Missing secrets still prevent startup when neither an environment variable nor a local value is present.

3. Run `./scripts/backend.sh spring-boot:run`. The launcher selects the installed JDK 26 automatically on macOS; elsewhere use an existing Java 26 installation or set `JAVA26_HOME`. The API prefix is `/dhtcms/api/v1`.
4. In `tms-react-app`, run `npm ci`, then `npm run dev`. Set `VITE_API_BASE_URL=/dhtcms/api/v1` in `tms-react-app/.env.local` to use Vite's local API proxy.

The legacy SQL dump in `Dumps` is optional sample data. Inspect it before importing into a disposable local database; do not import it into an existing environment.

## Verification

```sh
./scripts/backend.sh verify
cd tms-react-app
npm ci
npm run build
npm test -- --run
```

Backend tests use an isolated in-memory H2 database and a test-only signing key; they do not connect to your MySQL database. CI runs backend verification and frontend build/tests.

## IntelliJ and Java compiler setup

Use **Project SDK: 26** and **Language level: 26** in Project Structure. Set the Maven runner JRE and importer JDK to **Project JDK** (or explicitly **26**), then reload the Maven project. Run configurations should also use the project SDK.

The backend targets Java 26 throughout: Maven compilation, tests, the launcher, `.java-version`, and CI. It uses Spring Boot 4.1.2 and Lombok 1.18.48 with an explicit annotation-processor path. Run `./scripts/backend.sh clean verify` for a clean build.

IntelliJ IDEA 2026.1 or newer is required for full Java 26 language support. Older IDE versions can retain stale compiler or processor settings. After updating the IDE, reload the Maven project and rebuild it. If `ExceptionInInitializerError` / `com.sun.tools.javac.code.TypeTag :: UNKNOWN` persists, ensure annotation processing resolves the Maven-managed Lombok 1.18.48 processor rather than an older manually configured JAR.

Compatibility references: [Spring Boot requirements](https://docs.spring.io/spring-boot/system-requirements.html), [Lombok changelog](https://projectlombok.org/changelog), and [IntelliJ Java 26 support](https://blog.jetbrains.com/idea/2026/03/intellij-idea-2026-1/).

## Access and reliability

- Active users can access cases, steps, folders, and executions only in their assigned projects. QA managers and administrators have global project access.
- Administrative user, role, department, and project operations require a manager or administrator. Users can retrieve their own assigned-project list and assigned-project details.
- Global name searches and the global step list require a manager. Use project-scoped endpoints for engineer workflows.
- Folder and execution case assignments must stay within one project. Execution assignment/removal runs in a transaction and locks the execution to serialize concurrent changes.
- Passwords are write-only JSON properties. Audit authors come from the authenticated user. Invalid/expired tokens return 401; permission failures return 403.
- Create requests validate required fields. Unexpected failures return a public error and log server-side diagnostics.

## Release test plans and guided execution

Open a project and choose **Test Plans & Runs** in the sidebar. Existing execution URLs continue to work.

1. A QA manager creates a draft plan with a release, build, environment, deadline, selected cases, and optional initial tester.
2. Assign individual cases or select multiple rows and apply an assignment. Only active users who belong to the project can be assigned. Every case must have a tester before starting.
3. Start the plan. Assigned testers open **Run my cases**, follow the saved steps, record an actual result and notes, and use **Save & next** to continue. Managers can record any case in the project.
4. Attach PNG/JPEG screenshots or UTF-8 `.txt`/`.log` files (5 MB each, at most 10 files per case), and optionally link an HTTP/HTTPS bug URL. Downloads require project access.
5. Monitor passed, failed, blocked, skipped, unrun, and unassigned counts. Passed, failed, and skipped cases count as resolved. A manager can close the plan when all cases are resolved; closing preserves read-only results and evidence. Reopening allows retesting without erasing earlier attempts.

Cases added to a run retain a snapshot of their name, type, and steps. New result saves keep an author/time-stamped history and reject stale concurrent edits. Cases with plan results or evidence cannot be removed from the plan, and library cases referenced by release plans cannot be deleted. Search and assignment filters run before pagination; case pickers load the complete project library.

Existing executions appear in the same overview. Old runs without snapshots continue to display their live case content until a snapshot is first captured; historical steps and result attempts from before this feature cannot be reconstructed. New plans enforce tester assignments; legacy executions retain their previous project-member permissions.

The schema additions use the existing Hibernate update configuration on application restart. Verification uses an isolated H2 database rather than your local MySQL database. Versioned production database migrations remain separate work.

## Remaining modernization work

This hardening pass retains the existing database schema and API paths. Before a production rollout, add reviewed, versioned database migrations (the current configuration still uses `ddl-auto=update`), extend immutable snapshots to a full case-revision system, and maintain dependency updates. The backend uses the stable Spring Boot 4.1.2 release, Jackson 3, and JDK 26.
