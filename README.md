# ShopLite: Spring Boot companion code

ShopLite is the small online shop you build in a series of three Spring Boot courses. This repository holds
a runnable copy of the app at the end of every lesson that changes code, plus the finished app of each course.

| Course | Folder | Starts from | What it adds |
| --- | --- | --- | --- |
| Spring Boot Fundamentals | [`fundamentals/`](fundamentals/README.md) | an empty Spring Initializr project | beans and dependency injection, configuration, a REST catalog API, validation, Problem Details, PostgreSQL with Spring Data JPA and Flyway, logging |
| Spring Boot: Production-Ready APIs | [`production-apis/`](production-apis/README.md) | `fundamentals/final` | orders and customers, fetch plans, pagination, transactions, API versioning, testing, Spring Security with Keycloak, calling other services, caching, observability |
| Spring Boot: Advanced Architecture and Scale | [`advanced/`](advanced/README.md) | `production-apis/final` | events and messaging, modules and services, gateways, resilience, performance, container images, Kubernetes, CI, Spring AI |

Inside each course folder:

```text
steps/<NN>-<lesson-slug>/   the app at the end of lesson NN (a complete Gradle project)
final/                      the finished app of the course; the next course starts here
```

Every folder builds and runs on its own. If something you typed does not work, compare it with the step folder
of that lesson.

## Set up your computer

You need three things: **JDK 25**, **Docker**, and **an editor**. Gradle comes with every project (the Gradle
wrapper, `gradlew`), so you do not install it yourself.

### JDK 25

The projects compile and run with Java 25 (Gradle's toolchain asks for it). Any JDK 25 works; the courses use
Eclipse Temurin.

| System | Command |
| --- | --- |
| Windows (PowerShell) | `winget install EclipseAdoptium.Temurin.25.JDK` |
| macOS (Homebrew) | `brew install --cask temurin@25` |
| Ubuntu or Debian | add the Adoptium apt repository (see the Adoptium installation guide), then `sudo apt install temurin-25-jdk` |

Open a new terminal and check:

```bash
java -version
```

It must print `25` in the first line. If an older Java is first on your PATH, set `JAVA_HOME` to the JDK 25
folder, for example:

```bash
# macOS
export JAVA_HOME=$(/usr/libexec/java_home -v 25)
# Linux (path depends on your distribution)
export JAVA_HOME=/usr/lib/jvm/temurin-25-jdk-amd64
```

```powershell
# Windows PowerShell (path depends on the installer)
$env:JAVA_HOME = 'C:\Program Files\Eclipse Adoptium\jdk-25.0.5.8-hotspot'
```

### Docker

The apps run PostgreSQL (and later Redis, Keycloak and more) in Docker containers, and the tests start their own
containers with Testcontainers.

- Windows and macOS: install Docker Desktop and start it.
- Linux: install Docker Engine and the Compose plugin, and add your user to the `docker` group.

Check with `docker version` and `docker compose version`. Docker must be running before you build or run any
project that has a `compose.yaml`.

### An editor

IntelliJ IDEA (Community Edition is free) or Visual Studio Code with the Extension Pack for Java. Open a project
folder (for example `fundamentals/final`) and the editor reads `build.gradle.kts` by itself.

## Run any step or final

From the folder of the project you want, for example `fundamentals/final`:

```bash
./gradlew build        # compile and run all tests (Docker must be running)
./gradlew bootRun      # start the app on http://localhost:8080
```

On Windows in PowerShell or cmd, use `.\gradlew.bat build` and `.\gradlew.bat bootRun`.

Then try it:

```bash
curl -s localhost:8080/api/products
```

or open `http://localhost:8080/swagger-ui.html` in a browser (from lesson 10 of Spring Boot Fundamentals on).

Stop the app with Ctrl+C. Projects with a `compose.yaml` start their containers through Spring Boot's Docker
Compose support; to remove the containers and their data afterwards, run `docker compose down -v` in the same
folder.

Each project listens on port 8080, so run one at a time.

## Check everything

`scripts/verify-all.sh` (macOS, Linux, Git Bash) and `scripts/verify-all.ps1` (Windows PowerShell) build and
test every step and final folder. Add `--clean` / `-Clean` to delete each project's `build/` folder afterwards.

## License

Copyright (C) 2026 SypherNext

This project is licensed under the GNU Affero General Public License v3.0 (AGPL-3.0). You may
use, study and change it. If you share a modified version, or run one as a website or service
that other people use, you must make your full modified source code available under the same
license. See [LICENSE](LICENSE) for the full terms.
