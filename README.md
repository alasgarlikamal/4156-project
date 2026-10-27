# Perihelion: Academic Planning and Assessment Service

A Java/Spring Boot REST service backed by SQLite. The current MVP supports CRUD
for courses, enrollments, assignments, tasks, and submissions (25 operations).
Workload analysis, work-plan generation, grading, progress analysis, and Google
Calendar integration are planned features. No authentication is configured.

## Team

| Member | UNI |
| --- | --- |
| Giana Castro | gmc2196 |
| Daniel Keinan | dk3425 |
| Yabby Maelaf | ym3013 |
| Kamal Alasgarli | ka3088 |
| Wanting You | wy2470 |
| Ryan Shi | rs4669 |

## Build and run

Requires **JDK 21** and internet access for the initial dependency download. Maven
is provided through the wrapper. Run from the repository root; on Windows, use
`mvnw.cmd` instead of `./mvnw`.

```bash
mkdir -p data
./mvnw clean verify
./mvnw spring-boot:run
```

The service runs at `http://localhost:8080`; stop it with Ctrl+C. After building,
you can also run `java -jar target/project-0.0.1-SNAPSHOT.jar`.

Development data persists in `data/perihelion.db` across restarts. Startup runs
[schema.sql](src/main/resources/schema.sql) and Hibernate schema updates.
Database and connection settings are in
[application.properties](src/main/resources/application.properties).

## API documentation

See the **[API reference and usage examples](docs/api.md)** for all endpoints,
request/response fields, validation, side effects, status codes, call order,
and valid/invalid usage scenarios.

| Resource | Base path | List filters |
| --- | --- | --- |
| Courses | `/api/courses` | `title`, `code` |
| Enrollments | `/api/enrollments` | `courseId`, `learnerRef` |
| Assignments | `/api/assignments` | `courseId` |
| Tasks | `/api/tasks` | `enrollmentId`, `workplanId`, `courseworkId` |
| Submissions | `/api/submissions` | `enrollmentId`, `courseworkId` |

Each resource supports POST and GET on its base path, and GET, PUT, and DELETE
on `/{id}`. Create courses before enrollments/assignments and enrollments before
submissions. Delete dependent records first; there are no cascading deletes.

Current limitations: assignments do not create coursework records; submissions
do not validate coursework existence; tasks do not validate linked IDs or effort.
Task filters use precedence rather than combining all supplied filters. Details
are in the API reference.

## Tests and code quality

Build dependencies and plugins are configured in [pom.xml](pom.xml).
Tests are grouped by resource and layer in [src/test/java](src/test/java), using
JUnit 5 and Mockito. MockMvc controller tests mock services; service tests mock
repositories. The assignment integration test uses SQLite with the
[test profile](src/test/resources/application-test.properties), which selects an
in-memory database instead of the development database.

```bash
# Style checks, tests, packaging, and coverage report
./mvnw clean verify

# Tests without generating the coverage report
./mvnw test

# Check style across production AND test sources
./mvnw checkstyle:check -Dcheckstyle.includeTestSourceDirectory=true
```

- **Checkstyle 12.3.1**, via Maven plugin 3.6.0: custom Sun-based rules in
  [checkstyle.xml](checkstyle.xml), including 80-character lines, Javadocs, naming,
  imports, and whitespace. Test sources are excluded by default in the POM; use
  the explicit command above to check the whole codebase.
- **JaCoCo 0.8.15**: records coverage during tests and generates reports during
  `verify`. No minimum-coverage enforcement is configured.
- **GitHub Actions**: [ci.yml](.github/workflows/ci.yml) runs Checkstyle and Maven
  verification on pushes and pull requests.

| Report | Generated location |
| --- | --- |
| Test results (Surefire) | `target/surefire-reports/` |
| Style findings | `target/checkstyle-result.xml` and console output |
| Coverage HTML | `target/site/jacoco/index.html` |
| Coverage XML / CSV | `target/site/jacoco/jacoco.xml` / `jacoco.csv` |

The rubric requires a committed clean style report and **at least 55% branch
coverage**. Reports have not yet been included in this branch. Because `target/`
is ignored, copy the final reports into a tracked directory and link them here;
preserve the full JaCoCo HTML directory and record the tested commit. Use the
**Branches** total rather than line coverage.

A Postman collection or linked live-service API testing workspace is still
missing. MockMvc tests and the manual curl examples are available, but do not
constitute a live-service API suite.

## Third-party software

Application and testing libraries are downloaded through Maven; dependency
versions are declared or managed through [pom.xml](pom.xml). Run
`./mvnw dependency:tree` for the resolved list. These include Spring Boot,
Hibernate, SQLite JDBC, JUnit, and Mockito, plus Checkstyle and JaCoCo plugins.
No other vendored third-party application code was identified during this review.

Included third-party build scripts: `mvnw`, `mvnw.cmd`, and
`.mvn/wrapper/maven-wrapper.properties` come from
[Apache Maven Wrapper](https://github.com/apache/maven-wrapper).
[checkstyle.xml](checkstyle.xml) is a custom configuration based on Checkstyle's
[Sun ruleset](https://github.com/checkstyle/checkstyle/blob/master/src/main/resources/sun_checks.xml).

## Project management

Tasks and individual contributions are tracked on our
[Jira board](https://perihelion-4156.atlassian.net/jira/software/projects/KAN/boards/1).

## Submission status

Before submission, complete the missing API tests, reports, and AI records;
confirm Mentor access to Jira; merge reviewed work into `main`; and tag the final
main-branch commit before the deadline. Only tagged `main` is graded.
