# Test Case Management System

Spring Boot API (Java 26, MySQL) with a React/TypeScript frontend.

## Run locally

1. Install Java 26, Maven, Node.js 22, and MySQL. With nvm, run `nvm install` and `nvm use` from the repository root to select the version pinned in `.nvmrc`. Create a `tcms` database.
2. Copy `.env.example` to `.env` in the repository root, set your database password, and replace the signing-key placeholder with the output of `openssl rand -hex 32`. Use plain `KEY=value` lines without shell quotes or `export`. Spring Boot loads this ignored local file automatically when the working directory is the repository root, including when launched from an IDE. Existing environment variables take precedence over `.env` values. If local settings have already been created, keep that file instead of replacing it.

   For deployment, set `DB_PASSWORD` and `JWT_SECRET` in the deployment environment. The local `.env` is optional, is excluded from Git, and is not bundled into the application JAR. Missing secrets still prevent startup when neither an environment variable nor a local value is present.

3. Run `./scripts/backend.sh spring-boot:run`. The launcher selects the installed JDK 26 automatically on macOS; elsewhere use an existing Java 26 installation or set `JAVA26_HOME`. The API prefix is `/dhtcms/api/v1`.
4. In `tms-react-app`, run `npm ci`, then `npm run dev`. Set `VITE_API_BASE_URL=/dhtcms/api/v1` in `tms-react-app/.env.local` to use Vite's local API proxy.

The legacy SQL dump in `Dumps` is optional sample data. Inspect it before importing into a disposable local database; do not import it into an existing environment.

For IntelliJ npm run configurations, select the Node interpreter installed by `nvm install` (find its path with `nvm which`). On Apple Silicon, use the native ARM64 interpreter. After changing Node versions or architecture, run `npm ci --include=optional` in `tms-react-app` to reinstall native dependencies such as Rollup. The install checks for Node 22.12 or newer within the 22.x series; CI uses the same `.nvmrc` version.

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

## Cucumber Manual scenarios

Choose **Cucumber Manual** when creating or editing a case. Write Given/When/Then steps directly, a Scenario, or a complete Feature. The editor provides colored keywords and tables, scenario and outline templates, syntax validation, and a formatted preview.

New lines suggest the next keyword. **Ctrl+Space** or **Suggest step** opens suggestions, **Tab** accepts a suggestion, and **Esc** dismisses it. Existing Cucumber steps from the current project are also suggested. Use **Scenario outline** for parameterized cases, **Add Examples table** to insert sample rows, and **Format tables** to align columns.

Cucumber Manual runs display the saved scenario and example tables. The Then steps express the expected outcome; there is no separate expected-result field for this type. Testers still record a result, observations, notes, and evidence. Conventional Manual cases retain their expected-result fields.

Saving stores the scenario and case metadata together, preserving indentation and line breaks. Existing Cucumber step rows are combined when opened in the editor; plans retain their original snapshots when the library scenario changes.

## Case details and reports

Open **Case details** in the case editor to set priority, automation status, component, a project owner, estimated minutes per execution, description, preconditions, and up to 20 custom text fields. An owner maintains the library case; assigned testers perform individual runs. Owners must be active project members. Leave duration blank when unknown. Clear an optional field explicitly to remove its value; older API clients that omit details retain their existing values on edit.

New run snapshots preserve these details along with the steps. The runner shows the saved description, preconditions, priority, component, estimate, and custom fields. Older snapshots remain readable and have no reconstructed historical metadata.

Choose **Reports** in the project sidebar for outcomes, declared automation status, library priorities, daily result activity, latest case outcomes, plan comparisons, and active tester workload. Export all filtered case rows, plan comparisons, or daily activity as CSV.

- Environment and release filters select plans, results, and workload. Current library counts always cover all project cases.
- Execution totals count case memberships in active and closed plans; draft plans are excluded. The same case in two plans counts twice. Latest-case outcomes count each library case once.
- Pass rate is `PASS / (PASS + FAIL)`; completion is `(PASS + FAIL + SKIPPED) / all case executions`. Undefined rates display a dash.
- The date range applies only to daily activity, using inclusive UTC dates (up to 366 days). Activity counts saved attempts, including reruns and result resets. It does not measure unique cases or historical end-of-day pass rates.
- Linked defects count distinct URLs without inferring whether those defects are open. Remaining effort uses estimates preserved in active run snapshots, with missing estimates shown separately. Actual elapsed testing time and requirements coverage are not measured.
- A library case whose name, type, steps, or preserved details differ from its latest run snapshot is labeled **Library changed**. The old result remains available for review; the label does not imply the updated definition has passed. Metadata that was absent from older snapshots is not compared.

The API is `GET /projectReports/{projectId}` with optional `environment`, `release`, `from`, and `to` filters. It requires access to the project.

## Remaining modernization work

This hardening pass retains the existing database schema and API paths. Before a production rollout, add reviewed, versioned database migrations (the current configuration still uses `ddl-auto=update`), extend immutable snapshots to a full case-revision system, and maintain dependency updates. The backend uses the stable Spring Boot 4.1.2 release, Jackson 3, and JDK 26.


## Browser automation workers and downloads

Cucumber Automation and Keyword Driven cases support the built-in browser templates. Create an active test plan, assign the case, and choose **Run automation**. Only one pending job per case is allowed; cancel it before starting a replacement. Results from expired claims, superseded runs, changed case results, or closed plans are discarded. Existing pending jobs created before the lease checks were introduced should be cancelled and queued again.

The worker API is disabled until the backend has both of these environment variables:

```bash
export AUTOMATION_WORKER_TOKEN="$(openssl rand -hex 32)"
export AUTOMATION_WORKER_PROJECT_ID="123" # use the actual project ID
./scripts/backend.sh spring-boot:run
```

Keep the generated token private. The worker must receive the same token through `AUTOMATION_WORKER_TOKEN`; it can claim jobs only for that configured project. You can instead put `AUTOMATION_WORKER_TOKEN` and `AUTOMATION_WORKER_PROJECT_ID` in the ignored root `.env` file and restart the backend, then run `./scripts/worker.sh`. The worker reads its token and optional `TMS_URL` from that file, with environment variables taking precedence. It reads properties directly without evaluating shell commands. This setup supports one project per backend worker credential. Restart the backend and workers together when rotating it. Use HTTPS when workers connect over a network.

In a separate terminal, set that same token and the backend address, install Chromium once, then start the worker:

```bash
export AUTOMATION_WORKER_TOKEN="<the same private token>"
export TMS_URL="http://localhost:8080/dhtcms/api/v1"
./scripts/backend.sh compile exec:java -Dexec.mainClass=com.microsoft.playwright.CLI -Dexec.args="install chromium"
./scripts/worker.sh
```

**Browser preview and live steps** appear inside **Run automation** for both Cucumber Automation and Keyword Driven tests, including runs opened from batch details. The updated worker reports the current step before executing it, reports completed step outcomes, and captures a browser image after each step. The UI checks for updates about every 1.5 seconds without overlapping requests. This is a step-by-step screenshot preview; an image does not refresh during a long step. It works with headless browsers and remote workers. Restart workers with the updated code to enable it; older workers can still complete tests without previews.

Preview images mask form inputs, textareas, editable fields, and matching known secret text. Other application content may still appear. Preview access requires project membership, and publishing requires the current worker lease. Only the latest frame per job is stored, at most 512 KB. It is available for 15 minutes after the last update and then removed by periodic cleanup. Cancelled and reclaimed attempts clear their previews. Preview failures do not change the test outcome, and live observations do not record case results; final completion remains authoritative.

Page navigation allows 30 seconds by default (`navigationTimeoutMs`) and waits for DOM content. Individual actions and assertions still wait for their target elements. Background images or analytics do not need to finish before the next step. The same navigation behavior is used in downloaded projects.

Project secrets are masked in the UI and API without changing the stored values. Workers receive the configured runtime values for their project, so run workers only on trusted machines. Diagnostic text is redacted on ingestion; screenshots may still show application content.

**Download automation project** creates an independent Java 26 Maven project with feature files, keyword definitions, named locators, framework source, and working Maven wrappers for macOS/Linux and Windows. Extract the ZIP and follow its README. Set local values in `src/test/resources/config.properties`; secret values are replaced with placeholders in the export, and this local configuration is excluded from Git. The downloaded project requires the target application, Java, and Chromium, but does not require a running TMS server. The wrapper downloads Maven on first use.

Use **Import Automation** in the test-case library to preview `.feature` or keyword `.json` files before saving. Feature and Rule backgrounds retain their scope and order. Correct unmapped steps, required values, and case errors in the preview; replacement requires selecting the existing case explicitly. Invalid batches are rejected together.

Imported inputs that contain a configured project secret become variable references such as `${PASSWORD}`, preserving their runtime values without returning the secret in the preview. Text diagnostics remain masked, and old `[REDACTED]` input values must be replaced with valid values or variable references before saving. Variable references also work in locators, both in workers and downloaded projects.

### Supported Automation Templates and Examples

Both Cucumber Automation and Keyword Driven authoring support 11 templates with full parity across authoring, worker execution, import recognition, and standalone exports:

1. **`OPEN_PAGE`**: Navigates to a target URL (supports `${baseUrl}` and variable references).
   - Gherkin: `Given open page "http://localhost:8080/demo/login"`
2. **`ENTER_TEXT`**: Types text into an element located by ID, CSS, XPath, testId, role, or project named locator.
   - Gherkin: `When enter text "admin" into "#username"`
3. **`CLICK_ELEMENT`**: Clicks a target element.
   - Gherkin: `When click element "#login-btn"`
4. **`VERIFY_TEXT_EQUALS`**: Asserts the element's inner text matches the expected value.
   - Gherkin: `Then verify text of "#welcome" equals "Welcome, admin!"`
5. **`VERIFY_TEXT_CONTAINS`**: Asserts the element's text contains the substring.
   - Gherkin: `Then verify text of "#welcome" contains "Welcome"`
6. **`VERIFY_ELEMENT_STATE`**: Asserts the element is `visible` or `hidden`.
   - Gherkin: `Then verify element "#welcome" is visible`
7. **`SELECT_OPTION`**: Selects a native `<select>` option by label or by value.
   - Fields: Target locator, selection method (`label` or `value`), and option string.
   - Gherkin: `When select option "Playwright" by label in "#framework-select"`
   - Gherkin (by value): `When select option "pw" by value in "#framework-select"`
8. **`SET_CHECKBOX`**: Sets a checkbox to checked or unchecked idempotently (does not toggle an already-checked box).
   - Fields: Target locator, desired state (`checked` or `unchecked`).
   - Gherkin: `When set checkbox "#subscribe-checkbox" to checked`
   - Gherkin (unchecked): `When set checkbox "#subscribe-checkbox" to unchecked`
9. **`PRESS_KEY`**: Dispatches keyboard key press on the target element. Validates common keys (`Enter`, `Tab`, `Escape`, `ArrowDown`, `ArrowUp`, `ArrowLeft`, `ArrowRight`, `Backspace`, `Delete`, `Space`, `Home`, `End`, `PageUp`, `PageDown`) or single characters with clear error messages.
   - Fields: Target locator, key name.
   - Gherkin: `When press key "Enter" on "#keyboard-input"`
10. **`VERIFY_URL`**: Verifies the current page URL using `equals` or `contains`. Project variables such as `${baseUrl}` are resolved.
    - Fields: Comparison (`equals` or `contains`), expected URL.
    - Gherkin: `Then verify url equals "http://localhost:8080/demo/extended"`
    - Gherkin (contains): `Then verify url contains "/demo/extended"`
11. **`VERIFY_ATTRIBUTE`**: Verifies an element's HTML attribute equals the expected value. Distinguishes a missing attribute from an empty attribute value.
    - Fields: Target locator, attribute name, expected attribute value.
    - Gherkin: `Then verify attribute "data-status" of "#action-btn" equals "ready"`
    - Gherkin (empty attribute): `Then verify attribute "data-empty" of "#action-btn" equals ""`

Quoted Gherkin inputs preserve escaped quotes, backslashes, and line breaks across preview, import, worker execution, and downloads. Attribute assertions wait for delayed updates and work on hidden elements; an absent attribute fails even when the expected value is empty. Key names are normalized consistently in workers and downloaded projects, and may use project variable references. JSON imports require actual booleans for checkbox states and valid selection/URL comparison modes.

## Batch automation runs

Execute automated test cases in bulk directly within active test plans:

- **Starting a Batch**: In an active plan (`IN_PROGRESS`), use **Run All Automated** or select specific rows and choose **Run selected automation ({count})**.
- **Pre-Confirmation Preview**: Before queuing, an inspection modal reviews eligibility across all in-scope cases and breaks down exclusions into explicit tabs:
  - *Ready Cases*: Eligible automated cases (`CUCUMBER_AUTOMATION`, `KEYWORD_DRIVEN`) with valid step definitions.
  - *Manual Cases Skipped*: Manual cases are safely skipped without modifying their state.
  - *Invalid Configuration*: Cases with missing templates, empty steps, or unresolvable locators with clear diagnostic guidance.
  - *Already Pending*: Cases with an existing `QUEUED` or `RUNNING` job (enforcing the one-pending-job-per-case constraint).
  - *Assignment Restricted*: Cases the current user cannot execute under project and plan assignment rules.
- **Atomic Backend Enqueueing**:
  - Batches require an active plan (`IN_PROGRESS`) and project membership.
  - All requested cases are validated to belong to the plan.
  - Jobs are queued atomically under a pessimistic plan lock (`PESSIMISTIC_WRITE`) up to a maximum limit of 100 cases per batch.
  - Every job is linked to a durable `AutomationBatch` record while remaining fully compatible with existing workers and execution policies.
- **Consolidated Progress & Results**:
  - Live progress bar, aggregate status chips (`QUEUED`, `RUNNING`, `COMPLETED`, `FAILED`, `CANCELLED`), and counts breakdown.
  - Detailed case rows show individual durations, step summaries, error logs, and execution evidence links.
  - Invariant guarantee: A completed batch containing any failed or infrastructure-error job is marked as `FAILED` and never appears as "all passed".
  - Polling and batch monitoring persist across page refreshes via `GET /automation/batches/plan/{planId}/latest`.
- **Cancellation & Retries**:
  - **Cancel Batch**: Cancels authorized unfinished (`QUEUED`, `RUNNING`) jobs in an active plan while preserving other testers' jobs and completed results.
  - **Run Failed Cases Again**: When failures occur, this action spawns a brand new `AutomationBatch` and queues jobs strictly for failed or infrastructure-error cases. Earlier batch runs, job attempts, and execution history snapshots remain immutable and intact.

Batch progress is calculated from its persisted job rows, including after refreshing the page. Preview reads do not capture or change plan snapshots. Cancellation applies only to unfinished cases the current user can record in an active plan, and leaves other testers' jobs and completed results intact. “Run Failed Cases Again” creates new attempts for failed or infrastructure-error jobs; cancelled jobs are excluded. Historical details open the selected job, including its linked failure evidence. When a case already has ten evidence files, the result is still recorded and the diagnostic log explains why another screenshot was omitted.

## Automation run history and failure investigation

Trace, investigate, and compare historical automated test executions across plans and batches:

- **Project Automation History**:
  - Accessible via the **Automation History** navigation item in the project sidebar.
  - Lists individual job attempts with case name, test plan, batch reference (if applicable), execution status, queued/started/finished timestamps, and execution duration.
  - **Frozen Run Names**: Displays the frozen case name captured at queue time from the test definition snapshot, guaranteeing that future edits to the test case library do not alter execution history.
  - **Server-Side Pagination & Filtering**: Filter by test plan, job status (`QUEUED`, `RUNNING`, `COMPLETED`, `FAILED`, `INFRASTRUCTURE_ERROR`, `CANCELLED`), date range (from/to), and case name search with stable reverse-chronological ordering (`id DESC`).
  - Covers all execution modes: standalone single runs, original batches, and retry batches.
- **Exact Run Details & Failure Investigation**:
  - Clicking any history row opens that exact execution attempt in read-only mode using the automation run dialog.
  - Displays ordered step results with duration and failure messages, complete diagnostic execution logs, and failure screenshot evidence download links.
  - Clear empty states indicate when diagnostics, step outcomes, or screenshot evidence are unavailable (e.g. when cancelled or storage limits were reached).
  - Direct links to associated test plans and batch details allow smooth navigation across execution artifacts.
- **Side-by-Side Attempt Comparison**:
  - From any case run details, users can click **Compare Attempt** to select and inspect earlier attempts for the same case within that plan.
  - Compares overall status, execution duration, and step-level outcomes side by side.
  - **Changed Step Handling**: Steps are matched by their semantic action definition rather than sequential step numbers, explicitly detecting inserted, removed, or modified steps with dedicated diff badges (`Only in current`, `Only in previous`, `Fixed`, `Regressed`).
  - Stored execution results and historical records remain strictly immutable.
- **Project Isolation & Secret Masking**:
  - Every history, detail, attempt list, comparison, and evidence request enforces strict project membership authorization.
  - Project secrets are continuously redacted (`[REDACTED]`) in all diagnostic views, step descriptions, and error messages.

