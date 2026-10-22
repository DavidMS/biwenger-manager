# #8 — Backend skeleton

- **Issue:** [#8 [1.1] Backend skeleton: Maven multi-module, Spring Boot 4, Java 25](https://github.com/DavidMS/biwenger-manager/issues/8)
- **Phase:** 1 — Project skeleton
- **Date:** 2026-10-22

## Prompt

> merged, move phase 1 to ready and go with #8

The first issue run through the `implement-issue` skill (#4) rather than by hand. The skill supplied the steps, the board ids and the review step, so the prompt only had to name the issue.

## What Claude did

1. Moved #7 to *Done* and the six Phase 1 issues (#8–#13) to *Ready*, then branched and moved #8 to *In progress*.
2. **Set up Java 25.** The machine's newest JDK was 24 and the default was 21. SDKMAN's install of Temurin 25 failed twice with "archive was corrupt". Claude then:
   - downloaded the tarball from Adoptium directly
   - checked its SHA-256 against Adoptium's published checksum
   - unpacked it into SDKMAN's folder under the official id `25.0.4.1+1-tem`, so `sdk env` works with the committed `backend/.sdkmanrc`

   The default JDK was left at 21.
3. Asked Spring Initializr for current versions instead of trusting memory: Spring Boot 4.1.1 and Maven 3.10.0. It used Initializr's output only for the official Maven Wrapper files and the Boot 4 starter names (`spring-boot-starter-webmvc`, `-webmvc-test`, `-actuator-test`).
4. Wrote the build:
   - **parent POM:** Java 25, an enforcer rule that requires JDK 25 with a message saying how to fix it, and Spotless with google-java-format and sortpom, checked in `verify`
   - **`biwenger-client`:** a library with an enforcer rule that bans Spring Boot, Spring MVC/WebFlux and every database library. It first banned Spring Web too; see Corrections
   - **`app`:** Spring Boot with Actuator, exposing only `/actuator/health`, plus a `@SpringBootTest` that calls it over HTTP
5. Made the edit hook (`format-file.sh`) use the JDK named in `backend/.sdkmanrc`, since the hook runs with the default `java`.
6. Updated the status, repo map and commands in `CLAUDE.md`.

## Verification

| Check | Result |
|---|---|
| `./mvnw verify` | Green: 3 modules, 1 test, formatting check passing |
| Live app (`java -jar`, then `spring-boot:run` as documented) | `/actuator/health` → `{"status":"UP"}`. `/actuator/env` → 404 |
| Add `spring-webmvc` / the Postgres driver to `biwenger-client` | Build fails with the ADR 0004 message. `spring-web` is allowed |
| Build on JDK 21 | Fails: "Java 25 is required. Run `sdk env` in backend/…" |
| Edit hook | A badly formatted Java file written by Claude was reformatted right away |
| `java-reviewer` agent on the diff | Approve with nits (see below) |

## Review

This was the first real run of the `java-reviewer` agent (#5). Its verdict was **approve with nits**:

- **minor:** the client's ban list covered Spring Web, Boot and Data, but not the rest of "no database" from ADR 0003. JDBC, JPA, Hibernate, Flyway and the Postgres driver would have passed. **Fixed**, and checked by adding the Postgres driver to the client, which the build now rejects.
- **nit:** banning `app` in the client can never fire, because Maven rejects the dependency cycle first. **Dropped.**

The review also exposed a gap in our own tooling: the agent guard blocked `JAVA_HOME=… ./mvnw verify`, so the reviewer could only run the build on the default JDK 21, where it fails at the JDK check by design. The guard now allows a `JAVA_HOME=` prefix on Maven commands, and still blocks it on anything else.

## Corrections

- **google-java-format 1.37.0 fails inside Spotless 3.10.4.** Every Java file failed with `InvocationTargetException`. Claude's first guess was JDK 25 module access, and that guess was wrong: the flags it added in `.mvn/jvm.config` changed nothing. Running google-java-format by hand, then the same Spotless command on JDK 21, showed the real cause was the version combination. Pinning 1.36.1 fixed it, the useless `jvm.config` was deleted, and a comment in the POM records why.
- **The run command in `CLAUDE.md` didn't work.** `./mvnw -pl app -am spring-boot:run` also ran `spring-boot:run` on the parent POM, which has no main class. Claude only found this because it ran the documented command instead of a similar one. The parent now skips `run` by default and `app` turns it back on.
- **macOS has no `timeout` command,** so the first run of that check silently did nothing.

- **From human review: the client ban was too strict.** The human asked:

  > why are you leaving Spring web out of the client for the biwenger client?

  Claude had written "no Spring web deps" into the plan and ADR 0003, then read it in the strictest way: it banned the whole `spring-web` artifact. That artifact holds Spring's HTTP *client* (`RestClient`), not only server-side types. The ban added little isolation, because the mapping layer is what isolates the API. But it would have forced hand-written auth headers, JSON mapping and retries on top of the JDK's `HttpClient`. Claude explained the trade-off and recommended allowing `spring-web` while keeping Spring Boot, MVC/WebFlux and the database banned. The human replied:

  > yes, allow spring-web and write ADR 0004

  [ADR 0004](../../adr/0004-biwenger-client-uses-spring-http-client.md) supersedes that part of 0003. The enforcer rule, the plan, `CLAUDE.md`, the `backend-feature` skill and the `java-reviewer` agent were all updated to match.

Human review may add more corrections. Record them here before merging.

## Decisions made along the way

- **Package `io.github.davidms.biwenger`, groupId `io.github.davidms`.** That's the usual namespace for a GitHub-hosted project without its own domain. Left open in review here, and confirmed by the human in #12.
- **google-java-format** (Google style, 2-space indent) as the Java style, chosen because it has no options to argue about.
- **Enforce the module boundary in the build.** The client may use `spring-web` for `RestClient`, but the build fails if it pulls in Spring Boot, the server side or a database library (ADR 0004).
- **Fail fast on the wrong JDK** with a message that says how to fix it, rather than a confusing compiler error.

## Lesson

Check what you are told against reality, and that includes your own rules:

- **Versions and commands:** Initializr gave the real current versions. Running google-java-format by hand found the real cause of a confusing error. Running the documented command exactly as written found a bug a similar command would have hidden.
- **Rules:** "no Spring web deps" was a phrase Claude wrote into the plan. Turned into a build ban, it would have cost real code. Before enforcing a rule, ask what it actually protects. Here, it took the human's "why?" to ask that question.
